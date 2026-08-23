import groovy.json.JsonSlurper
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

plugins {
    // Gives the root project a `clean` task. Without it `./gradlew clean` never emptied
    // build/distributions, so stale soundpack ZIPs from older versions accumulated and the
    // release workflow's archive glob would attach all of them.
    base
    id("net.fabricmc.fabric-loom") version "1.17.19" apply false
    kotlin("jvm") version "2.4.10" apply false
    kotlin("plugin.serialization") version "2.4.10" apply false
    id("com.modrinth.minotaur") version "2.9.0" apply false
}

// The placeholders processResources expands into fabric.mod.json. Defined here and shared
// with :apps:mod so adding one is a single edit rather than three, and so the archive
// validator below expands exactly the set the build declares.
val biometunesManifestValues: Map<String, String> = mapOf(
    "version" to providers.gradleProperty("mod_version").get(),
    "minecraft_version" to providers.gradleProperty("minecraft_version").get(),
    "loader_version" to providers.gradleProperty("loader_version").get(),
    "fabric_kotlin_version" to providers.gradleProperty("fabric_kotlin_version").get(),
    "modmenu_version" to providers.gradleProperty("modmenu_version").get(),
)
extra["biometunesManifestValues"] = biometunesManifestValues

tasks.register<Zip>("packageSoundpack") {
    archiveBaseName.set("biometunes-soundpack")
    archiveVersion.set(providers.gradleProperty("mod_version"))
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from("apps/soundpack")
    from("LICENSE") {
        into("META-INF")
        rename { "LICENSE_BIOMETUNES" }
    }
}

data class DistributionArchiveInputs(
    val modArchive: Path,
    val soundpackArchive: Path,
    val soundpackRoot: Path,
    val catalogFile: Path,
    val licenseFile: Path,
    val fabricMetadataTemplateFile: Path,
    val mixinDescriptorFile: Path,
    val iconFile: Path,
    val modProjectAudioFiles: Set<Path>,
    val expectedModVersion: String,
    val expectedMinecraftVersion: String,
    val expectedManifestValues: Map<String, String>,
)

class DistributionArchiveValidator {
    fun verify(input: DistributionArchiveInputs): Int {
        requireValue(
            input.modProjectAudioFiles.isEmpty(),
            "mod project contains OGG files outside generated outputs: " +
                input.modProjectAudioFiles.map(Path::toString).sorted(),
        )

        val soundpackRoot = input.soundpackRoot
        val catalogBytes = Files.readAllBytes(input.catalogFile)
        val soundsBytes = Files.readAllBytes(soundpackRoot.resolve("assets/biometunes/sounds.json"))
        val creditsBytes = Files.readAllBytes(soundpackRoot.resolve("README.md"))
        val languageBytes = Files.readAllBytes(
            soundpackRoot.resolve("assets/biometunes/lang/en_us.json"),
        )
        val licenseBytes = Files.readAllBytes(input.licenseFile)
        val iconBytes = Files.readAllBytes(input.iconFile)
        val mixinBytes = Files.readAllBytes(input.mixinDescriptorFile)

        val trackIds = readTrackIds(catalogBytes)
        verifySoundRegistry(soundsBytes, trackIds)
        val expectedOggs = trackIds.mapTo(linkedSetOf()) { id ->
            "assets/biometunes/sounds/music/$id.ogg"
        }
        val canonicalOggs = Files.walk(soundpackRoot).use { paths ->
            paths.filter { path ->
                Files.isRegularFile(path) && path.fileName.toString().endsWith(".ogg")
            }.map { path ->
                soundpackRoot.relativize(path).toString().replace(File.separatorChar, '/')
            }.toList().toSet()
        }
        verifyOggInventory("canonical soundpack", canonicalOggs, expectedOggs)

        val jar = readArchive("Fabric JAR", input.modArchive)
        val zip = readArchive("standalone soundpack ZIP", input.soundpackArchive)
        jar.requireNonEmpty(
            setOf(
                "fabric.mod.json",
                "biometunes.client.mixins.json",
                "META-INF/LICENSE_BIOMETUNES",
                "META-INF/BIOMETUNES-CREDITS.md",
                "assets/biometunes/biometunes/tracks.json",
                "assets/biometunes/sounds.json",
                "assets/biometunes/lang/en_us.json",
                "assets/biometunes/icon.png",
            ),
        )
        zip.requireNonEmpty(
            setOf(
                "pack.mcmeta",
                "pack.png",
                "LICENSE",
                "README.md",
                "META-INF/LICENSE_BIOMETUNES",
                "assets/biometunes/sounds.json",
                "assets/biometunes/lang/en_us.json",
            ),
        )

        verifyOggInventory("Fabric JAR", jar.oggEntries(), expectedOggs)
        verifyOggInventory("standalone soundpack ZIP", zip.oggEntries(), expectedOggs)
        jar.requireBytes("assets/biometunes/biometunes/tracks.json", catalogBytes)
        jar.requireBytes("assets/biometunes/sounds.json", soundsBytes)
        zip.requireBytes("assets/biometunes/sounds.json", soundsBytes)
        jar.requireBytes("assets/biometunes/lang/en_us.json", languageBytes)
        zip.requireBytes("assets/biometunes/lang/en_us.json", languageBytes)
        jar.requireBytes("META-INF/BIOMETUNES-CREDITS.md", creditsBytes)
        zip.requireBytes("README.md", creditsBytes)
        jar.requireBytes("META-INF/LICENSE_BIOMETUNES", licenseBytes)
        zip.requireBytes("META-INF/LICENSE_BIOMETUNES", licenseBytes)
        jar.requireBytes("biometunes.client.mixins.json", mixinBytes)
        jar.requireBytes("assets/biometunes/icon.png", iconBytes)
        zip.requireBytes("pack.png", iconBytes)
        zip.requireBytes("LICENSE", Files.readAllBytes(soundpackRoot.resolve("LICENSE")))
        zip.requireBytes("pack.mcmeta", Files.readAllBytes(soundpackRoot.resolve("pack.mcmeta")))

        verifyFabricMetadata(jar.bytes("fabric.mod.json"), input)
        val packMetadata = parseObject(zip.bytes("pack.mcmeta"), "soundpack metadata")
        val pack = asObject(packMetadata["pack"], "soundpack metadata pack object")
        requireValue(
            pack["description"].toString().contains(input.expectedMinecraftVersion),
            "soundpack metadata does not identify Minecraft ${input.expectedMinecraftVersion}",
        )

        expectedOggs.forEach { entry ->
            val canonicalBytes = Files.readAllBytes(soundpackRoot.resolve(entry))
            val jarBytes = jar.bytes(entry)
            val zipBytes = zip.bytes(entry)
            requireValue(
                canonicalBytes.contentEquals(jarBytes) &&
                    canonicalBytes.contentEquals(zipBytes) &&
                    jarBytes.contentEquals(zipBytes),
                "audio bytes differ for $entry across canonical source, Fabric JAR, and soundpack ZIP",
            )
        }
        return expectedOggs.size
    }

    private fun verifyFabricMetadata(actualBytes: ByteArray, input: DistributionArchiveInputs) {
        val template = Files.readString(input.fabricMetadataTemplateFile)
        val versionPlaceholder = "\${version}"
        requireValue(
            template.contains(versionPlaceholder),
            "canonical fabric.mod.json template does not contain $versionPlaceholder",
        )
        // Substitute every placeholder processResources declares. Anything left over is a
        // placeholder the build does not know about, which would ship to players verbatim.
        val expanded = input.expectedManifestValues.entries.fold(template) { text, (key, value) ->
            text.replace("\${$key}", value)
        }
        requireValue(
            !Regex("""\$\{[^}]+}""").containsMatchIn(expanded),
            "canonical fabric.mod.json has an unexpanded placeholder",
        )
        val expected = parseObject(expanded.toByteArray(), "processed canonical Fabric metadata")
        val actual = parseObject(actualBytes, "Fabric metadata")
        requireValue(
            actual == expected,
            "Fabric JAR fabric.mod.json differs from canonical processed metadata",
        )
        requireValue(
            actual["version"] == input.expectedModVersion,
            "Fabric JAR version ${actual["version"]} does not match ${input.expectedModVersion}",
        )
        val dependencies = asObject(actual["depends"], "Fabric dependencies")
        requireValue(
            dependencies["minecraft"] == "~${input.expectedMinecraftVersion}",
            "Fabric JAR Minecraft dependency ${dependencies["minecraft"]} does not match " +
                input.expectedMinecraftVersion,
        )
    }

    private fun readTrackIds(catalogBytes: ByteArray): List<String> {
        val catalog = parseObject(catalogBytes, "canonical track catalog")
        val tracks = catalog["tracks"] as? List<*>
            ?: throw GradleException("canonical track catalog has no tracks array")
        val ids = tracks.mapIndexed { index, value ->
            val track = asObject(value, "track $index")
            val id = track["id"] as? String
                ?: throw GradleException("track $index has no string id")
            requireValue(
                track["sound_event"] == "biometunes:music.$id",
                "track $id has unexpected sound event ${track["sound_event"]}",
            )
            id
        }
        requireValue(ids.size == ids.toSet().size, "canonical track catalog has duplicate track IDs")
        requireValue(ids.isNotEmpty(), "canonical track catalog has no tracks")
        return ids
    }

    private fun verifySoundRegistry(soundsBytes: ByteArray, trackIds: List<String>) {
        val sounds = parseObject(soundsBytes, "canonical sounds registry")
        val expectedEvents = trackIds.mapTo(linkedSetOf()) { "music.$it" }
        val actualEvents = sounds.keys.mapTo(linkedSetOf()) { key ->
            key as? String ?: throw GradleException("canonical sounds registry has a non-string key")
        }
        requireValue(
            actualEvents == expectedEvents,
            "canonical sounds registry differs from catalog: expected $expectedEvents, got $actualEvents",
        )
        trackIds.forEach { id ->
            val event = asObject(sounds["music.$id"], "sound event music.$id")
            val definitions = event["sounds"] as? List<*>
                ?: throw GradleException("sound event music.$id has no sounds array")
            requireValue(definitions.size == 1, "sound event music.$id must have exactly one sound")
            val definition = asObject(definitions.single(), "sound definition music.$id")
            requireValue(
                definition["name"] == "biometunes:music/$id",
                "sound event music.$id points to ${definition["name"]}",
            )
            requireValue(
                definition["stream"] == true,
                "sound event music.$id must be streamed",
            )
        }
    }

    private fun readArchive(label: String, path: Path): ArchiveSnapshot {
        requireValue(Files.isRegularFile(path), "$label does not exist at $path")
        return ZipFile(path.toFile()).use { archive ->
            val counts = linkedMapOf<String, Int>()
            val contents = linkedMapOf<String, ByteArray>()
            val entries = archive.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                counts[entry.name] = counts.getOrDefault(entry.name, 0) + 1
                if (!entry.isDirectory && entry.name !in contents) {
                    contents[entry.name] = archive.getInputStream(entry).use { it.readBytes() }
                }
            }
            val duplicates = counts.filterValues { count -> count > 1 }.keys.sorted()
            requireValue(duplicates.isEmpty(), "$label has duplicate archive entries: $duplicates")
            ArchiveSnapshot(label, contents)
        }
    }

    private fun verifyOggInventory(label: String, actual: Set<String>, expected: Set<String>) {
        requireValue(
            actual == expected,
            "$label OGG inventory differs; missing=${(expected - actual).sorted()}, " +
                "excess=${(actual - expected).sorted()}",
        )
    }

    private fun parseObject(bytes: ByteArray, description: String): Map<*, *> =
        asObject(JsonSlurper().parseText(bytes.toString(Charsets.UTF_8)), description)

    private fun asObject(value: Any?, description: String): Map<*, *> =
        value as? Map<*, *> ?: throw GradleException("$description is not a JSON object")

    private fun requireValue(condition: Boolean, message: String) {
        if (!condition) throw GradleException(message)
    }

    private inner class ArchiveSnapshot(
        private val label: String,
        private val contents: Map<String, ByteArray>,
    ) {
        fun requireNonEmpty(required: Set<String>) {
            val missing = required.filter { entry -> contents[entry]?.isNotEmpty() != true }
            requireValue(missing.isEmpty(), "$label is missing required non-empty entries: $missing")
        }

        fun oggEntries(): Set<String> = contents.keys.filterTo(linkedSetOf()) { name ->
            name.endsWith(".ogg")
        }

        fun bytes(entry: String): ByteArray = contents[entry]
            ?: throw GradleException("$label is missing required entry $entry")

        fun requireBytes(entry: String, expected: ByteArray) {
            requireValue(
                bytes(entry).contentEquals(expected),
                "$label entry $entry differs from its canonical source",
            )
        }
    }
}

abstract class VerifyDistributionArchives : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val modArchive: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val soundpackArchive: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalSoundpack: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalCatalog: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repositoryLicense: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalFabricMetadata: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalMixinDescriptor: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalIcon: RegularFileProperty

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val modProjectAudioFiles: ConfigurableFileCollection

    @get:Input
    abstract val expectedModVersion: Property<String>

    @get:Input
    abstract val expectedMinecraftVersion: Property<String>

    @get:Input
    abstract val expectedManifestValues: MapProperty<String, String>

    @TaskAction
    fun verifyArchives() {
        val input = DistributionArchiveInputs(
            modArchive = modArchive.get().asFile.toPath(),
            soundpackArchive = soundpackArchive.get().asFile.toPath(),
            soundpackRoot = canonicalSoundpack.get().asFile.toPath(),
            catalogFile = canonicalCatalog.get().asFile.toPath(),
            licenseFile = repositoryLicense.get().asFile.toPath(),
            fabricMetadataTemplateFile = canonicalFabricMetadata.get().asFile.toPath(),
            mixinDescriptorFile = canonicalMixinDescriptor.get().asFile.toPath(),
            iconFile = canonicalIcon.get().asFile.toPath(),
            modProjectAudioFiles = modProjectAudioFiles.files.mapTo(linkedSetOf()) { file ->
                file.toPath()
            },
            expectedModVersion = expectedModVersion.get(),
            expectedMinecraftVersion = expectedMinecraftVersion.get(),
            expectedManifestValues = expectedManifestValues.get(),
        )
        val trackCount = DistributionArchiveValidator().verify(input)
        logger.lifecycle(
            "Verified BiomeTunes {} distributions: complete canonical metadata/assets, no mod " +
            "source OGGs, {} tracks, exact OGG inventory, no duplicates, and audio identity",
            input.expectedModVersion,
            trackCount,
        )
    }
}

tasks.register<VerifyDistributionArchives>("verifyDistributionArchives") {
    group = "verification"
    description = "Verifies the final Fabric JAR and standalone soundpack ZIP against canonical assets."
    dependsOn(":apps:mod:jar", "packageSoundpack")
    modArchive.set(layout.file(providers.provider {
        providers.gradleProperty("biometunesModArchive").orNull?.let(::file)
            ?: project(":apps:mod").tasks.named<Jar>("jar").get().archiveFile.get().asFile
    }))
    soundpackArchive.set(layout.file(providers.provider {
        providers.gradleProperty("biometunesSoundpackArchive").orNull?.let(::file)
            ?: tasks.named<Zip>("packageSoundpack").get().archiveFile.get().asFile
    }))
    canonicalSoundpack.set(layout.projectDirectory.dir("apps/soundpack"))
    canonicalCatalog.set(
        layout.projectDirectory.file(
            "apps/mod/src/client/resources/assets/biometunes/biometunes/tracks.json",
        ),
    )
    repositoryLicense.set(layout.projectDirectory.file("LICENSE"))
    canonicalFabricMetadata.set(
        layout.projectDirectory.file("apps/mod/src/main/resources/fabric.mod.json"),
    )
    canonicalMixinDescriptor.set(
        layout.projectDirectory.file("apps/mod/src/main/resources/biometunes.client.mixins.json"),
    )
    canonicalIcon.set(layout.projectDirectory.file("apps/soundpack/pack.png"))
    modProjectAudioFiles.from(
        layout.projectDirectory.dir("apps/mod").asFileTree.matching {
            include("**/*.ogg")
            exclude("build/**", "run/**")
        },
    )
    expectedModVersion.set(providers.gradleProperty("mod_version"))
    expectedMinecraftVersion.set(providers.gradleProperty("minecraft_version"))
    expectedManifestValues.set(biometunesManifestValues)
}

abstract class VerifyDistributionArchivesContract : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val modArchive: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val soundpackArchive: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalSoundpack: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalCatalog: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repositoryLicense: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalFabricMetadata: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalMixinDescriptor: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val canonicalIcon: RegularFileProperty

    @get:Input
    abstract val expectedModVersion: Property<String>

    @get:Input
    abstract val expectedMinecraftVersion: Property<String>

    @get:Input
    abstract val expectedManifestValues: MapProperty<String, String>

    @TaskAction
    fun verifyContract() {
        val validator = DistributionArchiveValidator()
        val base = DistributionArchiveInputs(
            modArchive = modArchive.get().asFile.toPath(),
            soundpackArchive = soundpackArchive.get().asFile.toPath(),
            soundpackRoot = canonicalSoundpack.get().asFile.toPath(),
            catalogFile = canonicalCatalog.get().asFile.toPath(),
            licenseFile = repositoryLicense.get().asFile.toPath(),
            fabricMetadataTemplateFile = canonicalFabricMetadata.get().asFile.toPath(),
            mixinDescriptorFile = canonicalMixinDescriptor.get().asFile.toPath(),
            iconFile = canonicalIcon.get().asFile.toPath(),
            modProjectAudioFiles = emptySet(),
            expectedModVersion = expectedModVersion.get(),
            expectedMinecraftVersion = expectedMinecraftVersion.get(),
            expectedManifestValues = expectedManifestValues.get(),
        )
        validator.verify(base)

        val metadata = readEntry(base.modArchive, "fabric.mod.json").toString(Charsets.UTF_8)
        mapOf(
            "server environment" to replaceRequired(
                metadata,
                "\"environment\": \"client\"",
                "\"environment\": \"server\"",
            ),
            "wrong client entrypoint" to replaceRequired(
                metadata,
                "com.hampushallkvist.biometunes.client.BiomeTunesClient",
                "com.hampushallkvist.biometunes.client.WrongEntrypoint",
            ),
            "wrong mixin metadata" to replaceRequired(
                metadata,
                "biometunes.client.mixins.json",
                "biometunes.wrong.mixins.json",
            ),
            "wrong dependency constraint" to replaceRequired(
                metadata,
                "\"fabric-api\": \"*\"",
                "\"fabric-api\": \">=999\"",
            ),
        ).forEach { (label, mutatedMetadata) ->
            val archive = mutateArchive(
                name = label.replace(' ', '-'),
                source = base.modArchive,
                replacements = mapOf("fabric.mod.json" to mutatedMetadata.toByteArray()),
            )
            expectRejected(
                label = label,
                expectedMessage = "fabric.mod.json differs from canonical processed metadata",
            ) {
                validator.verify(base.copy(modArchive = archive))
            }
        }

        val wrongMixin = mutateArchive(
            name = "wrong-mixin-bytes",
            source = base.modArchive,
            replacements = mapOf("biometunes.client.mixins.json" to "{}".toByteArray()),
        )
        expectRejected("changed mixin bytes", "biometunes.client.mixins.json differs") {
            validator.verify(base.copy(modArchive = wrongMixin))
        }

        val wrongIcon = mutateArchive(
            name = "wrong-icon-bytes",
            source = base.modArchive,
            replacements = mapOf("assets/biometunes/icon.png" to "not an icon".toByteArray()),
        )
        expectRejected("changed icon bytes", "assets/biometunes/icon.png differs") {
            validator.verify(base.copy(modArchive = wrongIcon))
        }

        val syntheticAudio = temporaryDir.toPath()
            .resolve("synthetic/apps/mod/src/main/resources/committed-source.ogg")
        Files.createDirectories(syntheticAudio.parent)
        Files.write(syntheticAudio, byteArrayOf(0x4f, 0x67, 0x67, 0x53))
        expectRejected("mod source OGG", "mod project contains OGG files outside generated outputs") {
            validator.verify(base.copy(modProjectAudioFiles = setOf(syntheticAudio)))
        }

        logger.lifecycle(
            "Verified committed archive rejection contract: metadata, mixin/icon bytes, and mod source OGG",
        )
    }

    private fun readEntry(archivePath: Path, name: String): ByteArray =
        ZipFile(archivePath.toFile()).use { archive ->
            val entry = archive.getEntry(name)
                ?: throw GradleException("contract fixture is missing $name")
            archive.getInputStream(entry).use { it.readBytes() }
        }

    private fun replaceRequired(source: String, oldValue: String, newValue: String): String {
        if (!source.contains(oldValue)) {
            throw GradleException("contract fixture does not contain $oldValue")
        }
        return source.replace(oldValue, newValue)
    }

    private fun mutateArchive(
        name: String,
        source: Path,
        replacements: Map<String, ByteArray>,
    ): Path {
        val target = temporaryDir.toPath().resolve("$name.jar")
        Files.createDirectories(target.parent)
        val replaced = mutableSetOf<String>()
        ZipFile(source.toFile()).use { input ->
            ZipOutputStream(Files.newOutputStream(target)).use { output ->
                val entries = input.entries()
                while (entries.hasMoreElements()) {
                    val original = entries.nextElement()
                    val copy = ZipEntry(original.name).apply { time = original.time }
                    output.putNextEntry(copy)
                    if (!original.isDirectory) {
                        val bytes = replacements[original.name]?.also { replaced += original.name }
                            ?: input.getInputStream(original).use { it.readBytes() }
                        output.write(bytes)
                    }
                    output.closeEntry()
                }
            }
        }
        val missing = replacements.keys - replaced
        if (missing.isNotEmpty()) throw GradleException("contract fixture entries missing: $missing")
        return target
    }

    private fun expectRejected(label: String, expectedMessage: String, verify: () -> Unit) {
        val failure = runCatching(verify).exceptionOrNull()
            ?: throw GradleException("$label unexpectedly passed archive verification")
        if (failure !is GradleException || !failure.message.orEmpty().contains(expectedMessage)) {
            throw GradleException(
                "$label failed for the wrong reason; expected '$expectedMessage', got '${failure.message}'",
                failure,
            )
        }
    }
}

tasks.register<VerifyDistributionArchivesContract>("verifyDistributionArchivesContract") {
    group = "verification"
    description = "Proves release verification rejects corrupted metadata/assets and mod source audio."
    dependsOn("verifyDistributionArchives")
    modArchive.set(
        project(":apps:mod").tasks.named<Jar>("jar").flatMap { task -> task.archiveFile },
    )
    soundpackArchive.set(tasks.named<Zip>("packageSoundpack").flatMap { task -> task.archiveFile })
    canonicalSoundpack.set(layout.projectDirectory.dir("apps/soundpack"))
    canonicalCatalog.set(
        layout.projectDirectory.file(
            "apps/mod/src/client/resources/assets/biometunes/biometunes/tracks.json",
        ),
    )
    repositoryLicense.set(layout.projectDirectory.file("LICENSE"))
    canonicalFabricMetadata.set(
        layout.projectDirectory.file("apps/mod/src/main/resources/fabric.mod.json"),
    )
    canonicalMixinDescriptor.set(
        layout.projectDirectory.file("apps/mod/src/main/resources/biometunes.client.mixins.json"),
    )
    canonicalIcon.set(layout.projectDirectory.file("apps/soundpack/pack.png"))
    expectedModVersion.set(providers.gradleProperty("mod_version"))
    expectedMinecraftVersion.set(providers.gradleProperty("minecraft_version"))
    expectedManifestValues.set(biometunesManifestValues)
}

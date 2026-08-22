import groovy.json.JsonSlurper
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.19" apply false
    kotlin("jvm") version "2.4.10" apply false
    kotlin("plugin.serialization") version "2.4.10" apply false
    id("com.modrinth.minotaur") version "2.9.0" apply false
}

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

    @get:Input
    abstract val expectedModVersion: Property<String>

    @get:Input
    abstract val expectedMinecraftVersion: Property<String>

    @TaskAction
    fun verifyArchives() {
        val soundpackRoot = canonicalSoundpack.get().asFile.toPath()
        val catalogFile = canonicalCatalog.get().asFile.toPath()
        val licenseFile = repositoryLicense.get().asFile.toPath()
        val catalogBytes = Files.readAllBytes(catalogFile)
        val soundsFile = soundpackRoot.resolve("assets/biometunes/sounds.json")
        val soundsBytes = Files.readAllBytes(soundsFile)
        val creditsFile = soundpackRoot.resolve("README.md")
        val creditsBytes = Files.readAllBytes(creditsFile)
        val languageFile = soundpackRoot.resolve("assets/biometunes/lang/en_us.json")
        val languageBytes = Files.readAllBytes(languageFile)

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

        val jar = readArchive("Fabric JAR", modArchive.get().asFile.toPath())
        val zip = readArchive("standalone soundpack ZIP", soundpackArchive.get().asFile.toPath())
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
        jar.requireBytes("META-INF/LICENSE_BIOMETUNES", Files.readAllBytes(licenseFile))
        zip.requireBytes("META-INF/LICENSE_BIOMETUNES", Files.readAllBytes(licenseFile))
        zip.requireBytes("LICENSE", Files.readAllBytes(soundpackRoot.resolve("LICENSE")))
        zip.requireBytes("pack.mcmeta", Files.readAllBytes(soundpackRoot.resolve("pack.mcmeta")))

        val fabricMetadata = parseObject(jar.bytes("fabric.mod.json"), "Fabric metadata")
        requireValue(
            fabricMetadata["version"] == expectedModVersion.get(),
            "Fabric JAR version ${fabricMetadata["version"]} does not match ${expectedModVersion.get()}",
        )
        val dependencies = asObject(fabricMetadata["depends"], "Fabric dependencies")
        requireValue(
            dependencies["minecraft"] == "~${expectedMinecraftVersion.get()}",
            "Fabric JAR Minecraft dependency ${dependencies["minecraft"]} does not match " +
                expectedMinecraftVersion.get(),
        )
        val packMetadata = parseObject(zip.bytes("pack.mcmeta"), "soundpack metadata")
        val pack = asObject(packMetadata["pack"], "soundpack metadata pack object")
        requireValue(
            pack["description"].toString().contains(expectedMinecraftVersion.get()),
            "soundpack metadata does not identify Minecraft ${expectedMinecraftVersion.get()}",
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

        logger.lifecycle(
            "Verified BiomeTunes {} distributions: {} tracks, exact OGG inventory, no duplicates, " +
                "and canonical audio identity",
            expectedModVersion.get(),
            expectedOggs.size,
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
    expectedModVersion.set(providers.gradleProperty("mod_version"))
    expectedMinecraftVersion.set(providers.gradleProperty("minecraft_version"))
}

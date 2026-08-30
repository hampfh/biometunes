plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.modrinth.minotaur")
}

version = property("mod_version") as String
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    maven("https://maven.terraformersmc.com/releases/")
}

loom {
    splitEnvironmentSourceSets()
    mods {
        create("biometunes") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    compileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
    runtimeOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")
    testImplementation(kotlin("test"))
}

val minecraftVersion = property("minecraft_version") as String

// Defined once in the root build so the archive validator expands exactly the set of
// placeholders processResources declares here.
@Suppress("UNCHECKED_CAST")
val manifestValues = rootProject.extra["biometunesManifestValues"] as Map<String, String>

// Modrinth release copy lives beside this build file so it is reviewable in the repository
// rather than typed into the web editor. Pre-1.0 versions publish as beta.
val modrinthBody = layout.projectDirectory.file("modrinth/description.md")
val modrinthChangelog = layout.projectDirectory.file("modrinth/changelog-${project.version}.md")

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set(providers.environmentVariable("MODRINTH_PROJECT_ID"))
    versionNumber.set(project.version.toString())
    versionName.set("BiomeTunes ${project.version} for Minecraft $minecraftVersion")
    versionType.set(if (project.version.toString().startsWith("0.")) "beta" else "release")
    uploadFile.set(tasks.named("jar"))
    gameVersions.add(minecraftVersion)
    loaders.add("fabric")
    changelog.set(providers.fileContents(modrinthChangelog).asText)
    syncBodyFrom.set(providers.fileContents(modrinthBody).asText)
    dependencies {
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
        optional.project("modmenu")
    }
}

// Minotaur splits publication across two tasks: `modrinth` uploads the version and never
// reads syncBodyFrom, while `modrinthSyncBody` is the only task that updates the project
// page. Without this the description would never leave the repository.
tasks.named("modrinth") {
    finalizedBy(tasks.named("modrinthSyncBody"))
}

// Run as part of `check` so a version bump without a matching changelog fails during the
// build, not after the release workflow has already published the GitHub release.
val verifyReleaseCopy = tasks.register("verifyReleaseCopy") {
    group = "verification"
    description = "Checks the Modrinth description and this version's changelog exist and are non-empty."
    val body = modrinthBody.asFile
    val changelog = modrinthChangelog.asFile
    val version = project.version.toString()
    inputs.files(body, changelog).optional()
    doLast {
        listOf(
            "Modrinth project description" to body,
            "Modrinth changelog for version $version" to changelog,
        ).forEach { (label, file) ->
            require(file.isFile) { "Missing $label: expected $file" }
            require(file.length() > 0L) { "Empty $label: $file" }
        }
    }
}

tasks.named("modrinth") {
    dependsOn(verifyReleaseCopy)
    doFirst {
        // Unset GitHub secrets arrive as empty strings, which would otherwise surface as an
        // opaque 401 from Modrinth instead of naming the missing credential.
        listOf("MODRINTH_TOKEN", "MODRINTH_PROJECT_ID").forEach { name ->
            require(!System.getenv(name).isNullOrBlank()) { "$name is unset or empty" }
        }
    }
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("biometunes.repoRoot", rootProject.projectDir.absolutePath)
}

tasks.check {
    dependsOn(rootProject.tasks.named("verifyDistributionArchivesContract"))
    dependsOn(verifyReleaseCopy)
}

tasks.processResources {
    manifestValues.forEach { (key, value) -> inputs.property(key, value) }
    filesMatching("fabric.mod.json") {
        // expand() is Groovy templating; without this a future \" or \\ in the JSON is eaten.
        expand(manifestValues) { escapeBackslash = true }
    }
    from(rootProject.file("apps/soundpack/assets")) {
        exclude("biometunes/raw-soundtracks/**")
        into("assets")
    }
    from(rootProject.file("apps/soundpack/pack.png")) {
        into("assets/biometunes")
        rename { "icon.png" }
    }
    from(rootProject.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE_BIOMETUNES" }
    }
    from(rootProject.file("apps/soundpack/README.md")) {
        into("META-INF")
        rename { "BIOMETUNES-CREDITS.md" }
    }
}

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

// Keep fabric.mod.json's declared versions sourced from gradle.properties so a
// dependency bump is a one-line change instead of an edit in two places.
val manifestValues = mapOf(
    "version" to project.version.toString(),
    "minecraft_version" to minecraftVersion,
    "loader_version" to property("loader_version") as String,
    "fabric_kotlin_version" to property("fabric_kotlin_version") as String,
    "modmenu_version" to property("modmenu_version") as String,
)

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

// Fail closed rather than silently publishing a version with an empty changelog or
// blanking the project page, which is what an absent file provider would do.
tasks.named("modrinth") {
    doFirst {
        require(modrinthChangelog.asFile.isFile) {
            "Missing Modrinth changelog for version ${project.version}: expected ${modrinthChangelog.asFile}"
        }
        require(modrinthBody.asFile.isFile) {
            "Missing Modrinth project description: expected ${modrinthBody.asFile}"
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
}

tasks.processResources {
    manifestValues.forEach { (key, value) -> inputs.property(key, value) }
    filesMatching("fabric.mod.json") {
        expand(manifestValues)
    }
    from(rootProject.file("apps/soundpack/assets")) { into("assets") }
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

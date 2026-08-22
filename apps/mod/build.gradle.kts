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

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set(providers.environmentVariable("MODRINTH_PROJECT_ID"))
    versionNumber.set(project.version.toString())
    versionName.set("BiomeTunes ${project.version} for Minecraft 26.2")
    versionType.set("release")
    uploadFile.set(tasks.named("jar"))
    gameVersions.add("26.2")
    loaders.add("fabric")
    dependencies {
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
        optional.project("modmenu")
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
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
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

import org.gradle.api.tasks.bundling.Zip

plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
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

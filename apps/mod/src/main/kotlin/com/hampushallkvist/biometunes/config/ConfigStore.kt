package com.hampushallkvist.biometunes.config

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConfigStore(
    private val path: Path,
    private val warn: (String, Throwable) -> Unit,
) {
    private val codec = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun load(): BiomeTunesConfig {
        if (Files.notExists(path)) return BiomeTunesConfig()
        return runCatching {
            codec.decodeFromString<BiomeTunesConfig>(Files.readString(path)).normalized()
        }.getOrElse { error ->
            warn("Could not read $path; using defaults without modifying the file", error)
            BiomeTunesConfig()
        }
    }

    fun save(config: BiomeTunesConfig): Result<Unit> {
        val temporary = path.resolveSibling("${path.fileName}.tmp")
        return runCatching {
            path.parent?.let(Files::createDirectories)
            Files.writeString(
                temporary,
                codec.encodeToString(config.normalized()),
                CREATE,
                TRUNCATE_EXISTING,
            )
            try {
                Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, REPLACE_EXISTING)
            }
            Unit
        }.onFailure { error ->
            runCatching { Files.deleteIfExists(temporary) }
            warn("Could not save $path", error)
        }
    }
}

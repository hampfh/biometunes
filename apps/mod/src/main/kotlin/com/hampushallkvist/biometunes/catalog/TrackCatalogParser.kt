package com.hampushallkvist.biometunes.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object TrackCatalogParser {
    private val codec = Json {
        ignoreUnknownKeys = false
    }

    fun parse(json: String): Result<TrackCatalog> = runCatching {
        val raw = codec.decodeFromString<RawTrackCatalog>(json)
        requireNoDuplicateBiomeKeys(json)
        validateAndConvert(raw)
    }

    private fun validateAndConvert(raw: RawTrackCatalog): TrackCatalog {
        val tracks = buildMap<TrackId, TrackDefinition> {
            raw.tracks.forEachIndexed { index, track ->
                validateTrackId(track.id, "tracks[$index].id")
                validateResourceIdentifier(track.soundEvent, "tracks[$index].sound_event")
                require(track.title.isNotBlank()) {
                    "tracks[$index].title must be nonblank for track '${track.id}'"
                }
                require(track.artist.isNotBlank()) {
                    "tracks[$index].artist must be nonblank for track '${track.id}'"
                }

                val id = TrackId(track.id)
                require(id !in this) { "tracks[$index].id '${track.id}' is duplicated" }
                put(
                    id,
                    TrackDefinition(
                        id = id,
                        soundEvent = track.soundEvent,
                        title = track.title,
                        artist = track.artist,
                    ),
                )
            }
        }

        val bosses = raw.bosses.mapValues { (boss, track) ->
            validateTrackKey(boss, "bosses key")
            trackIdReference(track, "bosses['$boss']", tracks)
        }
        val biomes = raw.biomes.mapValues { (biome, track) ->
            validateResourceIdentifier(biome, "biomes key")
            trackIdReference(track, "biomes['$biome']", tracks)
        }
        val biomeTagKeys = mutableSetOf<String>()
        val biomeTags = raw.biomeTags.mapIndexed { index, tag ->
            validateResourceIdentifier(tag.tag, "biome_tags[$index].tag")
            require(biomeTagKeys.add(tag.tag)) {
                "biome_tags[$index].tag '${tag.tag}' is duplicated"
            }
            tag.tag to trackIdReference(tag.track, "biome_tags[$index].track", tracks)
        }
        val dimensions = raw.dimensions.mapValues { (dimension, track) ->
            validateResourceIdentifier(dimension, "dimensions key")
            trackIdReference(track, "dimensions['$dimension']", tracks)
        }
        val fallback = trackIdReference(raw.fallback, "fallback", tracks)

        return TrackCatalog(
            tracks = tracks,
            bosses = bosses,
            biomes = biomes,
            biomeTags = biomeTags,
            dimensions = dimensions,
            fallback = fallback,
        )
    }

    private fun validateTrackId(value: String, field: String) {
        require(TRACK_KEY.matches(value)) { "$field '$value' must match [a-z0-9_.-]+" }
    }

    private fun validateTrackKey(value: String, field: String) = validateTrackId(value, field)

    private fun validateResourceIdentifier(value: String, field: String) {
        require(RESOURCE_IDENTIFIER.matches(value)) {
            "$field '$value' must be a lowercase namespaced resource identifier"
        }
    }

    private fun trackIdReference(
        value: String,
        field: String,
        tracks: Map<TrackId, TrackDefinition>,
    ): TrackId {
        validateTrackId(value, field)
        val id = TrackId(value)
        require(id in tracks) { "$field references unknown track '$value'" }
        return id
    }

    private fun requireNoDuplicateBiomeKeys(json: String) {
        val duplicate = JsonObjectKeyReader(json).firstDuplicateInRootObject() ?: return
        val message = when (duplicate.scope) {
            "biomes" -> "biomes key '${duplicate.key}' is duplicated"
            "root" -> "root property '${duplicate.key}' is duplicated"
            else -> error("Unexpected JSON duplicate scope '${duplicate.scope}'")
        }
        throw IllegalArgumentException(message)
    }

    private val TRACK_KEY = Regex("[a-z0-9_.-]+")
    private val RESOURCE_IDENTIFIER = Regex("[a-z0-9_.-]+:[a-z0-9/._-]+")
}

@Serializable
private data class RawTrackCatalog(
    val tracks: List<RawTrackDefinition>,
    val bosses: Map<String, String>,
    val biomes: Map<String, String>,
    @SerialName("biome_tags") val biomeTags: List<RawBiomeTag>,
    val dimensions: Map<String, String>,
    val fallback: String,
)

@Serializable
private data class RawTrackDefinition(
    val id: String,
    @SerialName("sound_event") val soundEvent: String,
    val title: String,
    val artist: String,
)

@Serializable
private data class RawBiomeTag(
    val tag: String,
    val track: String,
)

private class JsonObjectKeyReader(private val source: String) {
    private var index = 0

    fun firstDuplicateInRootObject(): DuplicateKey? {
        skipWhitespace()
        expect('{')
        skipWhitespace()
        if (consume('}')) return null
        val rootKeys = mutableSetOf<String>()

        while (true) {
            val key = readString()
            val isDuplicateRootKey = !rootKeys.add(key)
            skipWhitespace()
            expect(':')
            skipWhitespace()
            val duplicateBiomeKey = if (key == "biomes") duplicateKeyInObject() else {
                skipValue()
                null
            }
            if (isDuplicateRootKey) return DuplicateKey("root", key)
            if (duplicateBiomeKey != null) return DuplicateKey("biomes", duplicateBiomeKey)
            skipWhitespace()
            if (consume('}')) return null
            expect(',')
            skipWhitespace()
        }
    }

    private fun duplicateKeyInObject(): String? {
        expect('{')
        skipWhitespace()
        if (consume('}')) return null
        val keys = mutableSetOf<String>()

        while (true) {
            val key = readString()
            if (!keys.add(key)) return key
            skipWhitespace()
            expect(':')
            skipWhitespace()
            skipValue()
            skipWhitespace()
            if (consume('}')) return null
            expect(',')
            skipWhitespace()
        }
    }

    private fun skipValue() {
        skipWhitespace()
        when (source[index]) {
            '{' -> skipObject()
            '[' -> skipArray()
            '"' -> readString()
            else -> {
                while (index < source.length && source[index] !in ",]}") index++
            }
        }
        skipWhitespace()
    }

    private fun skipObject() {
        expect('{')
        skipWhitespace()
        if (consume('}')) return
        while (true) {
            readString()
            skipWhitespace()
            expect(':')
            skipValue()
            if (consume('}')) return
            expect(',')
            skipWhitespace()
        }
    }

    private fun skipArray() {
        expect('[')
        skipWhitespace()
        if (consume(']')) return
        while (true) {
            skipValue()
            if (consume(']')) return
            expect(',')
            skipWhitespace()
        }
    }

    private fun readString(): String {
        expect('"')
        val value = StringBuilder()
        while (source[index] != '"') {
            val character = source[index++]
            if (character != '\\') {
                value.append(character)
                continue
            }
            when (val escaped = source[index++]) {
                '"', '\\', '/' -> value.append(escaped)
                'b' -> value.append('\b')
                'f' -> value.append('\u000C')
                'n' -> value.append('\n')
                'r' -> value.append('\r')
                't' -> value.append('\t')
                'u' -> value.append(source.substring(index, index + 4).toInt(16).toChar().also { index += 4 })
                else -> error("Unsupported escape sequence: \\$escaped")
            }
        }
        index++
        return value.toString()
    }

    private fun expect(character: Char) {
        check(index < source.length && source[index] == character) { "Expected '$character' in catalog JSON" }
        index++
    }

    private fun consume(character: Char): Boolean =
        (index < source.length && source[index] == character).also { if (it) index++ }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }

    data class DuplicateKey(val scope: String, val key: String)
}

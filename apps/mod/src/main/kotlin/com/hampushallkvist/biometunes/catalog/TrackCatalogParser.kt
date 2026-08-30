package com.hampushallkvist.biometunes.catalog

import com.hampushallkvist.biometunes.environment.EnvironmentalProfile
import com.hampushallkvist.biometunes.environment.EnvironmentalSampling
import com.hampushallkvist.biometunes.environment.EnvironmentalThresholds
import com.hampushallkvist.biometunes.environment.EnvironmentalWeights
import com.hampushallkvist.biometunes.environment.ShelteredAudioSettings
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
                require(track.id != SILENCE_TRACK) {
                    "tracks[$index].id '$SILENCE_TRACK' is reserved for silence pool entries"
                }
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

        val globalSilence = validateSilence(raw.silence, "silence")

        val bosses = raw.bosses.mapValues { (boss, pool) ->
            validateTrackKey(boss, "bosses key")
            validatePool(pool, "bosses['$boss']", tracks)
        }
        val biomes = raw.biomes.mapValues { (biome, pool) ->
            validateResourceIdentifier(biome, "biomes key")
            validatePool(pool, "biomes['$biome']", tracks)
        }
        val biomeTagKeys = mutableSetOf<String>()
        val biomeTags = raw.biomeTags.mapIndexed { index, tag ->
            validateResourceIdentifier(tag.tag, "biome_tags[$index].tag")
            require(biomeTagKeys.add(tag.tag)) {
                "biome_tags[$index].tag '${tag.tag}' is duplicated"
            }
            tag.tag to validatePool(tag.tracks, "biome_tags[$index].tracks", tracks)
        }
        val dimensions = raw.dimensions.mapValues { (dimension, pool) ->
            validateResourceIdentifier(dimension, "dimensions key")
            validatePool(pool, "dimensions['$dimension']", tracks)
        }
        val environmentalProfiles = raw.environmentalProfiles.mapValues { (dimension, profile) ->
            validateResourceIdentifier(dimension, "environmental_profiles key")
            validateEnvironmentalProfile(dimension, profile, tracks, biomes)
        }
        val fallback = validatePool(raw.fallback, "fallback", tracks)

        return TrackCatalog(
            tracks = tracks,
            globalSilence = globalSilence,
            bosses = bosses,
            biomes = biomes,
            biomeTags = biomeTags,
            dimensions = dimensions,
            environmentalProfiles = environmentalProfiles,
            fallback = fallback,
        )
    }

    private fun validateEnvironmentalProfile(
        dimension: String,
        raw: RawEnvironmentalProfile,
        tracks: Map<TrackId, TrackDefinition>,
        biomes: Map<String, TrackPool>,
    ): EnvironmentalProfile {
        val field = "environmental_profiles['$dimension']"
        val nativeUndergroundBiomes = raw.nativeUndergroundBiomes.mapIndexed { index, biome ->
            validateResourceIdentifier(biome, "$field.native_underground_biomes[$index]")
            require(biome in biomes) {
                "$field.native_underground_biomes[$index] references biome '$biome' without an exact biome pool"
            }
            biome
        }.toSet()
        require(nativeUndergroundBiomes.size == raw.nativeUndergroundBiomes.size) {
            "$field.native_underground_biomes contains a duplicate biome"
        }

        require(raw.sampling.nearbyRadius in 1..16) {
            "$field.sampling.nearby_radius must be from 1 through 16"
        }
        require(raw.sampling.enclosureRadius in 1..16) {
            "$field.sampling.enclosure_radius must be from 1 through 16"
        }
        require(raw.sampling.surfaceDepthScale in 1..256) {
            "$field.sampling.surface_depth_scale must be from 1 through 256"
        }

        val weights = raw.weights
        val weightValues = listOf(
            "nearby_sky_exposure" to weights.nearbySkyExposure,
            "sky_light" to weights.skyLight,
            "local_openness" to weights.localOpenness,
            "surface_depth" to weights.surfaceDepth,
            "local_enclosure" to weights.localEnclosure,
            "sky_darkness" to weights.skyDarkness,
            "block_darkness" to weights.blockDarkness,
        )
        weightValues.forEach { (name, value) ->
            require(value.isFinite() && value in 0.0..10.0) {
                "$field.weights.$name must be finite and from 0 through 10"
            }
        }
        require(weights.nearbySkyExposure + weights.skyLight + weights.localOpenness > 0.0) {
            "$field.weights exposure weights must have a positive total"
        }
        require(
            weights.surfaceDepth + weights.localEnclosure + weights.skyDarkness + weights.blockDarkness > 0.0,
        ) {
            "$field.weights depth weights must have a positive total"
        }

        val thresholds = raw.thresholds
        listOf(
            "outside_enter_exposure" to thresholds.outsideEnterExposure,
            "outside_exit_exposure" to thresholds.outsideExitExposure,
            "subterranean_enter_depth" to thresholds.subterraneanEnterDepth,
            "subterranean_exit_depth" to thresholds.subterraneanExitDepth,
            "subterranean_enter_max_exposure" to thresholds.subterraneanEnterMaxExposure,
            "subterranean_exit_max_exposure" to thresholds.subterraneanExitMaxExposure,
        ).forEach { (name, value) ->
            require(value.isFinite() && value in 0.0..1.0) {
                "$field.thresholds.$name must be finite and from 0 through 1"
            }
        }
        require(thresholds.outsideEnterExposure > thresholds.outsideExitExposure) {
            "$field.thresholds.outside_enter_exposure must be greater than outside_exit_exposure"
        }
        require(thresholds.subterraneanEnterDepth > thresholds.subterraneanExitDepth) {
            "$field.thresholds.subterranean_enter_depth must be greater than subterranean_exit_depth"
        }
        require(thresholds.subterraneanEnterMaxExposure < thresholds.subterraneanExitMaxExposure) {
            "$field.thresholds.subterranean_enter_max_exposure must be less than subterranean_exit_max_exposure"
        }

        require(raw.smoothingSeconds.isFinite() && raw.smoothingSeconds in 0.0..10.0) {
            "$field.smoothing_seconds must be finite and from 0 through 10"
        }
        require(raw.shelteredAudio.maximumGainMultiplier.isFinite() &&
            raw.shelteredAudio.maximumGainMultiplier in 0.0f..1.0f) {
            "$field.sheltered_audio.maximum_gain_multiplier must be finite and from 0 through 1"
        }
        require(raw.shelteredAudio.maximumHighFrequencyGain.isFinite() &&
            raw.shelteredAudio.maximumHighFrequencyGain in 0.0f..1.0f) {
            "$field.sheltered_audio.maximum_high_frequency_gain must be finite and from 0 through 1"
        }

        return EnvironmentalProfile(
            nativeUndergroundBiomes = nativeUndergroundBiomes,
            subterraneanTracks = validatePool(raw.subterraneanTracks, "$field.subterranean_tracks", tracks),
            sampling = EnvironmentalSampling(
                nearbyRadius = raw.sampling.nearbyRadius,
                enclosureRadius = raw.sampling.enclosureRadius,
                surfaceDepthScale = raw.sampling.surfaceDepthScale,
            ),
            weights = EnvironmentalWeights(
                nearbySkyExposure = weights.nearbySkyExposure,
                skyLight = weights.skyLight,
                localOpenness = weights.localOpenness,
                surfaceDepth = weights.surfaceDepth,
                localEnclosure = weights.localEnclosure,
                skyDarkness = weights.skyDarkness,
                blockDarkness = weights.blockDarkness,
            ),
            thresholds = EnvironmentalThresholds(
                outsideEnterExposure = thresholds.outsideEnterExposure,
                outsideExitExposure = thresholds.outsideExitExposure,
                subterraneanEnterDepth = thresholds.subterraneanEnterDepth,
                subterraneanExitDepth = thresholds.subterraneanExitDepth,
                subterraneanEnterMaxExposure = thresholds.subterraneanEnterMaxExposure,
                subterraneanExitMaxExposure = thresholds.subterraneanExitMaxExposure,
            ),
            smoothingSeconds = raw.smoothingSeconds,
            shelteredAudio = ShelteredAudioSettings(
                maximumGainMultiplier = raw.shelteredAudio.maximumGainMultiplier,
                maximumHighFrequencyGain = raw.shelteredAudio.maximumHighFrequencyGain,
            ),
        )
    }

    private fun validatePool(
        raw: List<RawPoolEntry>,
        field: String,
        tracks: Map<TrackId, TrackDefinition>,
    ): TrackPool {
        require(raw.isNotEmpty()) { "$field must contain at least one entry" }
        val referencedTracks = mutableSetOf<TrackId>()
        var hasSilence = false
        val entries = raw.mapIndexed { index, entry ->
            val entryField = "$field[$index]"
            require(entry.weight >= 0) { "$entryField.weight must be non-negative" }
            if (entry.track == SILENCE_TRACK) {
                require(!hasSilence) { "$field contains more than one silence entry" }
                hasSilence = true
                requireNotNull(entry.durationSeconds) {
                    "$entryField.duration_seconds is required for silence"
                }.let { validateSilence(RawSilence(entry.weight, it), entryField) }
            } else {
                require(entry.durationSeconds == null) {
                    "$entryField.duration_seconds is only valid for silence"
                }
                val id = trackIdReference(entry.track, "$entryField.track", tracks)
                require(referencedTracks.add(id)) {
                    "$field references track '${id.value}' more than once"
                }
                WeightedTrack(id, entry.weight)
            }
        }
        require(entries.any { it.weight > 0 }) { "$field must have a positive total weight" }
        return TrackPool(entries)
    }

    private fun validateSilence(raw: RawSilence, field: String): WeightedSilence {
        require(raw.weight >= 0) { "$field.weight must be non-negative" }
        require(raw.durationSeconds.min >= 0) { "$field.duration_seconds.min must be non-negative" }
        require(raw.durationSeconds.max >= raw.durationSeconds.min) {
            "$field.duration_seconds.max must be greater than or equal to min"
        }
        return WeightedSilence(
            weight = raw.weight,
            durationSeconds = SilenceDurationRange(raw.durationSeconds.min, raw.durationSeconds.max),
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
    private const val SILENCE_TRACK = "silence"
}

@Serializable
private data class RawTrackCatalog(
    val tracks: List<RawTrackDefinition>,
    val silence: RawSilence,
    val bosses: Map<String, List<RawPoolEntry>>,
    val biomes: Map<String, List<RawPoolEntry>>,
    @SerialName("biome_tags") val biomeTags: List<RawBiomeTag>,
    val dimensions: Map<String, List<RawPoolEntry>>,
    @SerialName("environmental_profiles")
    val environmentalProfiles: Map<String, RawEnvironmentalProfile>,
    val fallback: List<RawPoolEntry>,
)

@Serializable
private data class RawEnvironmentalProfile(
    @SerialName("native_underground_biomes") val nativeUndergroundBiomes: List<String>,
    @SerialName("subterranean_tracks") val subterraneanTracks: List<RawPoolEntry>,
    val sampling: RawEnvironmentalSampling,
    val weights: RawEnvironmentalWeights,
    val thresholds: RawEnvironmentalThresholds,
    @SerialName("smoothing_seconds") val smoothingSeconds: Double,
    @SerialName("sheltered_audio") val shelteredAudio: RawShelteredAudioSettings,
)

@Serializable
private data class RawEnvironmentalSampling(
    @SerialName("nearby_radius") val nearbyRadius: Int,
    @SerialName("enclosure_radius") val enclosureRadius: Int,
    @SerialName("surface_depth_scale") val surfaceDepthScale: Int,
)

@Serializable
private data class RawEnvironmentalWeights(
    @SerialName("nearby_sky_exposure") val nearbySkyExposure: Double,
    @SerialName("sky_light") val skyLight: Double,
    @SerialName("local_openness") val localOpenness: Double,
    @SerialName("surface_depth") val surfaceDepth: Double,
    @SerialName("local_enclosure") val localEnclosure: Double,
    @SerialName("sky_darkness") val skyDarkness: Double,
    @SerialName("block_darkness") val blockDarkness: Double,
)

@Serializable
private data class RawEnvironmentalThresholds(
    @SerialName("outside_enter_exposure") val outsideEnterExposure: Double,
    @SerialName("outside_exit_exposure") val outsideExitExposure: Double,
    @SerialName("subterranean_enter_depth") val subterraneanEnterDepth: Double,
    @SerialName("subterranean_exit_depth") val subterraneanExitDepth: Double,
    @SerialName("subterranean_enter_max_exposure") val subterraneanEnterMaxExposure: Double,
    @SerialName("subterranean_exit_max_exposure") val subterraneanExitMaxExposure: Double,
)

@Serializable
private data class RawShelteredAudioSettings(
    @SerialName("maximum_gain_multiplier") val maximumGainMultiplier: Float,
    @SerialName("maximum_high_frequency_gain") val maximumHighFrequencyGain: Float,
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
    val tracks: List<RawPoolEntry>,
)

@Serializable
private data class RawPoolEntry(
    val track: String,
    val weight: Int,
    @SerialName("duration_seconds") val durationSeconds: RawDurationRange? = null,
)

@Serializable
private data class RawSilence(
    val weight: Int,
    @SerialName("duration_seconds") val durationSeconds: RawDurationRange,
)

@Serializable
private data class RawDurationRange(
    val min: Int,
    val max: Int,
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

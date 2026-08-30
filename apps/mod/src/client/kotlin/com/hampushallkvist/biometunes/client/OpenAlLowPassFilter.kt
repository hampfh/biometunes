package com.hampushallkvist.biometunes.client

import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.EXTEfx

interface LowPassBackend {
    fun contextToken(): Any

    fun supported(): Boolean

    fun create(): Int

    fun setHighFrequencyGain(filter: Int, value: Float)

    fun attach(source: Int, filter: Int)

    fun detach(source: Int)

    fun delete(filter: Int)
}

enum class AudioFilterStatus {
    INACTIVE,
    EFX_ACTIVE,
    GAIN_ONLY_UNSUPPORTED,
    GAIN_ONLY_ERROR,
}

data class AudioTreatmentDiagnostics(
    val status: AudioFilterStatus,
    val requestedHighFrequencyGain: Float,
    val attachedVoiceCount: Int,
    val effectiveGainMultiplier: Float = 1.0f,
    val reverbStatus: ReverbStatus = ReverbStatus.INACTIVE,
    val requestedReverbSend: Float = 0.0f,
    val reverbVoiceCount: Int = 0,
)

class OpenAlLowPassFilter(
    private val backend: LowPassBackend,
    private val onFailure: (Throwable) -> Unit,
) {
    private var contextToken: Any? = null
    private var filterId: Int? = null
    private var lastHighFrequencyGain: Float? = null
    private val appliedHighFrequencyGainBySource = mutableMapOf<Int, Float>()
    private var unavailableStatus: AudioFilterStatus? = null
    private var failureReported = false

    @Volatile
    var diagnostics = AudioTreatmentDiagnostics(
        status = AudioFilterStatus.INACTIVE,
        requestedHighFrequencyGain = 1.0f,
        attachedVoiceCount = 0,
    )
        private set

    fun apply(source: Int, highFrequencyGain: Float) {
        val requested = highFrequencyGain.coerceIn(0.0f, 1.0f)
        try {
            refreshContext()
        } catch (error: Throwable) {
            fail(error, requested)
            return
        }

        unavailableStatus?.let { status ->
            updateDiagnostics(status, requested)
            return
        }

        if (requested >= 1.0f) {
            detach(source, requested)
            return
        }

        try {
            val filter = ensureFilter(requested) ?: return
            if (lastHighFrequencyGain != requested) {
                backend.setHighFrequencyGain(filter, requested)
                lastHighFrequencyGain = requested
            }
            if (appliedHighFrequencyGainBySource[source] != requested) {
                backend.attach(source, filter)
                appliedHighFrequencyGainBySource[source] = requested
            }
            updateDiagnostics(AudioFilterStatus.EFX_ACTIVE, requested)
        } catch (error: Throwable) {
            fail(error, requested)
        }
    }

    fun reset(activeSources: IntArray) {
        try {
            if (filterId != null) {
                (activeSources.asIterable() + appliedHighFrequencyGainBySource.keys)
                    .distinct()
                    .forEach(backend::detach)
                backend.delete(filterId!!)
            }
        } catch (error: Throwable) {
            if (!failureReported) onFailure(error)
        } finally {
            clearState(clearContext = true)
        }
    }

    private fun refreshContext() {
        val current = backend.contextToken()
        if (contextToken === current) return
        clearState(clearContext = false)
        contextToken = current
    }

    private fun ensureFilter(requested: Float): Int? {
        filterId?.let { return it }
        if (!backend.supported()) {
            unavailableStatus = AudioFilterStatus.GAIN_ONLY_UNSUPPORTED
            updateDiagnostics(AudioFilterStatus.GAIN_ONLY_UNSUPPORTED, requested)
            return null
        }
        return backend.create().also { filterId = it }
    }

    private fun detach(source: Int, requested: Float) {
        try {
            if (appliedHighFrequencyGainBySource.remove(source) != null) backend.detach(source)
            val status = if (appliedHighFrequencyGainBySource.isEmpty()) {
                AudioFilterStatus.INACTIVE
            } else {
                AudioFilterStatus.EFX_ACTIVE
            }
            updateDiagnostics(status, requested)
        } catch (error: Throwable) {
            fail(error, requested)
        }
    }

    private fun fail(error: Throwable, requested: Float) {
        unavailableStatus = AudioFilterStatus.GAIN_ONLY_ERROR
        appliedHighFrequencyGainBySource.clear()
        if (!failureReported) {
            failureReported = true
            onFailure(error)
        }
        updateDiagnostics(AudioFilterStatus.GAIN_ONLY_ERROR, requested)
    }

    private fun updateDiagnostics(status: AudioFilterStatus, requested: Float) {
        diagnostics = AudioTreatmentDiagnostics(status, requested, appliedHighFrequencyGainBySource.size)
    }

    private fun clearState(clearContext: Boolean) {
        if (clearContext) contextToken = null
        filterId = null
        lastHighFrequencyGain = null
        appliedHighFrequencyGainBySource.clear()
        unavailableStatus = null
        failureReported = false
        diagnostics = AudioTreatmentDiagnostics(AudioFilterStatus.INACTIVE, 1.0f, 0)
    }
}

class LwjglLowPassBackend : LowPassBackend {
    override fun contextToken(): Any = AL.getCapabilities()

    override fun supported(): Boolean = AL.getCapabilities().ALC_EXT_EFX

    override fun create(): Int {
        val filter = EXTEfx.alGenFilters()
        checkOpenAl("create low-pass filter")
        EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS)
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, 1.0f)
        checkOpenAl("configure low-pass filter")
        return filter
    }

    override fun setHighFrequencyGain(filter: Int, value: Float) {
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, value)
        checkOpenAl("set low-pass high-frequency gain")
    }

    override fun attach(source: Int, filter: Int) {
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter)
        checkOpenAl("attach low-pass filter")
    }

    override fun detach(source: Int) {
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL)
        checkOpenAl("detach low-pass filter")
    }

    override fun delete(filter: Int) {
        EXTEfx.alDeleteFilters(filter)
        checkOpenAl("delete low-pass filter")
    }

    private fun checkOpenAl(operation: String) {
        val error = AL10.alGetError()
        check(error == AL10.AL_NO_ERROR) { "OpenAL error $error while attempting to $operation" }
    }
}

package com.hampushallkvist.biometunes.client

import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import org.lwjgl.openal.EXTEfx

data class ReverbResources(
    val effect: Int,
    val slot: Int,
)

interface ReverbBackend {
    fun contextToken(): Any

    fun supported(): Boolean

    fun create(): ReverbResources

    fun setSendGain(slot: Int, value: Float)

    fun attach(source: Int, slot: Int)

    fun detach(source: Int)

    fun delete(resources: ReverbResources)
}

enum class ReverbStatus {
    INACTIVE,
    EFX_ACTIVE,
    UNSUPPORTED,
    ERROR,
}

data class ReverbDiagnostics(
    val status: ReverbStatus,
    val requestedSend: Float,
    val attachedVoiceCount: Int,
)

class OpenAlReverbSend(
    private val backend: ReverbBackend,
    private val onFailure: (Throwable) -> Unit,
) {
    private var contextToken: Any? = null
    private var resources: ReverbResources? = null
    private var lastSend: Float? = null
    private val attachedSources = mutableSetOf<Int>()
    private var unavailableStatus: ReverbStatus? = null
    private var failureReported = false

    @Volatile
    var diagnostics = ReverbDiagnostics(ReverbStatus.INACTIVE, 0.0f, 0)
        private set

    fun apply(source: Int, send: Float) {
        val requested = send.coerceIn(0.0f, 1.0f)
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

        if (requested <= 0.0f) {
            detach(source, requested)
            return
        }

        try {
            val current = ensureResources(requested) ?: return
            if (lastSend != requested) {
                backend.setSendGain(current.slot, requested)
                lastSend = requested
            }
            if (attachedSources.add(source)) backend.attach(source, current.slot)
            updateDiagnostics(ReverbStatus.EFX_ACTIVE, requested)
        } catch (error: Throwable) {
            fail(error, requested)
        }
    }

    fun reset(activeSources: IntArray) {
        try {
            resources?.let { current ->
                (activeSources.asIterable() + attachedSources).distinct().forEach(backend::detach)
                backend.delete(current)
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

    private fun ensureResources(requested: Float): ReverbResources? {
        resources?.let { return it }
        if (!backend.supported()) {
            unavailableStatus = ReverbStatus.UNSUPPORTED
            updateDiagnostics(ReverbStatus.UNSUPPORTED, requested)
            return null
        }
        return backend.create().also { resources = it }
    }

    private fun detach(source: Int, requested: Float) {
        try {
            if (attachedSources.remove(source)) backend.detach(source)
            val status = if (attachedSources.isEmpty()) {
                ReverbStatus.INACTIVE
            } else {
                ReverbStatus.EFX_ACTIVE
            }
            updateDiagnostics(status, requested)
        } catch (error: Throwable) {
            fail(error, requested)
        }
    }

    private fun fail(error: Throwable, requested: Float) {
        unavailableStatus = ReverbStatus.ERROR
        attachedSources.clear()
        if (!failureReported) {
            failureReported = true
            onFailure(error)
        }
        updateDiagnostics(ReverbStatus.ERROR, requested)
    }

    private fun updateDiagnostics(status: ReverbStatus, requested: Float) {
        diagnostics = ReverbDiagnostics(status, requested, attachedSources.size)
    }

    private fun clearState(clearContext: Boolean) {
        if (clearContext) contextToken = null
        resources = null
        lastSend = null
        attachedSources.clear()
        unavailableStatus = null
        failureReported = false
        diagnostics = ReverbDiagnostics(ReverbStatus.INACTIVE, 0.0f, 0)
    }
}

class LwjglReverbBackend : ReverbBackend {
    override fun contextToken(): Any = AL.getCapabilities()

    override fun supported(): Boolean = AL.getCapabilities().ALC_EXT_EFX

    override fun create(): ReverbResources {
        val effect = EXTEfx.alGenEffects()
        checkOpenAl("create reverb effect")
        try {
            configureDarkRoom(effect)
            val slot = EXTEfx.alGenAuxiliaryEffectSlots()
            try {
                checkOpenAl("create reverb effect slot")
                EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, effect)
                EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, 0.0f)
                checkOpenAl("configure reverb effect slot")
                return ReverbResources(effect, slot)
            } catch (error: Throwable) {
                EXTEfx.alDeleteAuxiliaryEffectSlots(slot)
                throw error
            }
        } catch (error: Throwable) {
            EXTEfx.alDeleteEffects(effect)
            throw error
        }
    }

    override fun setSendGain(slot: Int, value: Float) {
        EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, value)
        checkOpenAl("set reverb send gain")
    }

    override fun attach(source: Int, slot: Int) {
        AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, slot, 0, EXTEfx.AL_FILTER_NULL)
        checkOpenAl("attach reverb send")
    }

    override fun detach(source: Int) {
        AL11.alSource3i(
            source,
            EXTEfx.AL_AUXILIARY_SEND_FILTER,
            EXTEfx.AL_EFFECTSLOT_NULL,
            0,
            EXTEfx.AL_FILTER_NULL,
        )
        checkOpenAl("detach reverb send")
    }

    override fun delete(resources: ReverbResources) {
        EXTEfx.alDeleteAuxiliaryEffectSlots(resources.slot)
        checkOpenAl("delete reverb effect slot")
        EXTEfx.alDeleteEffects(resources.effect)
        checkOpenAl("delete reverb effect")
    }

    private fun configureDarkRoom(effect: Int) {
        EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DENSITY, 0.6f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DIFFUSION, 0.7f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_GAIN, 1.0f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_GAINHF, 0.1f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DECAY_TIME, 0.45f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DECAY_HFRATIO, 0.25f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.25f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, 0.01f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 0.4f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, 0.02f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_AIR_ABSORPTION_GAINHF, 0.892f)
        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_ROOM_ROLLOFF_FACTOR, 0.0f)
        EXTEfx.alEffecti(effect, EXTEfx.AL_REVERB_DECAY_HFLIMIT, AL10.AL_TRUE)
        checkOpenAl("configure dark room reverb")
    }

    private fun checkOpenAl(operation: String) {
        val error = AL10.alGetError()
        check(error == AL10.AL_NO_ERROR) { "OpenAL error $error while attempting to $operation" }
    }
}

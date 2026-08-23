package com.hampushallkvist.biometunes.playback

import com.hampushallkvist.biometunes.catalog.TrackDefinition

interface AudioHandle

interface AudioAdapter {
    fun start(track: TrackDefinition, initialGain: Float): AudioHandle?
    fun setGain(handle: AudioHandle, gain: Float)
    fun stop(handle: AudioHandle)
    fun isPlaying(handle: AudioHandle): Boolean
    fun setVanillaMusicSuppressed(suppressed: Boolean)
}

data class PlaybackOptions(val volume: Float, val crossfadeTicks: Int)

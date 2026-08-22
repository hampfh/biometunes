package com.hampushallkvist.biometunes.catalog

class ReloadableTrackCatalog {
    @Volatile
    var current: TrackCatalog? = null
        private set

    fun reload(json: String): Result<TrackCatalog> =
        TrackCatalogParser.parse(json).onSuccess { current = it }
}

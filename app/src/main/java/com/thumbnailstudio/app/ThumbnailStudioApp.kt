package com.thumbnailstudio.app

import android.app.Application
import com.thumbnailstudio.app.render.ImagePipeline

/** Keeps startup cheap: no SDK initialisers, no network, no analytics. */
class ThumbnailStudioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ImagePipeline.clearCache()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            ImagePipeline.clearCache()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ImagePipeline.clearCache()
    }
}

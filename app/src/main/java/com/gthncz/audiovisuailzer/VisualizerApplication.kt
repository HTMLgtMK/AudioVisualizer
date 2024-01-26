package com.gthncz.audiovisuailzer

import android.app.Application
import android.content.Context

class VisualizerApplication: Application() {

    companion object {
        @JvmStatic @Volatile
        private var mApp: VisualizerApplication? = null

        @JvmStatic
        fun get(): VisualizerApplication? = mApp
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        mApp = this
    }

}
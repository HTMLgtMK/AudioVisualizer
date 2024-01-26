package com.gthncz.audiovisuailzer.player

import androidx.annotation.IntDef

@IntDef(
    PlayerState.STATE_IDLE,
    PlayerState.STATE_INITIALIZED,
    PlayerState.STATE_PREPARING,
    PlayerState.STATE_BUFFERING,
    PlayerState.STATE_PREPARED,
    PlayerState.STATE_STARTED,
    PlayerState.STATE_COMPLETED,
    PlayerState.STATE_PAUSED,
    PlayerState.STATE_ERROR
)
annotation class PlayerState {
    companion object {
        const val STATE_IDLE = 0
        const val STATE_INITIALIZED = 1
        const val STATE_PREPARING = 2
        const val STATE_BUFFERING = 3
        const val STATE_PREPARED = 4
        const val STATE_STARTED = 5
        const val STATE_PAUSED = 6
        const val STATE_COMPLETED = 7
        const val STATE_ERROR = 8
    }
}

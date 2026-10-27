package com.raulhrn.vox.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlin.math.min

/** Plays the user's audio file, with variable speed and a smoothed position clock. */
class SongPlayer(private val context: Context, private val uri: Uri) {
    private var player: MediaPlayer? = null

    @Volatile
    var ready = false
        private set

    @Volatile
    var failed = false
        private set

    var durationMs = 0
        private set

    private var playing = false
    private var speed = 1f
    private var lastPos = 0
    private var lastPosNs = 0L

    fun prepare(onReady: () -> Unit) {
        if (player != null) return
        val p = MediaPlayer()
        player = p
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            p.setDataSource(context, uri)
            p.setOnPreparedListener {
                ready = true
                durationMs = it.duration.coerceAtLeast(0)
                onReady()
            }
            p.setOnErrorListener { _, _, _ ->
                failed = true
                ready = false
                true
            }
            p.setOnCompletionListener { playing = false }
            p.prepareAsync()
        } catch (_: Exception) {
            failed = true
        }
    }

    fun play(fromMs: Int, speed: Float) {
        val p = player ?: return
        if (!ready) return
        try {
            p.seekTo(fromMs.toLong(), MediaPlayer.SEEK_CLOSEST)
            if (speed != this.speed || speed != 1f) p.playbackParams = p.playbackParams.setSpeed(speed)
            p.start()
            this.speed = speed
            playing = true
            lastPos = fromMs
            lastPosNs = System.nanoTime()
        } catch (_: Exception) {
            failed = true
        }
    }

    fun pause() {
        val p = player ?: return
        playing = false
        try {
            if (ready && p.isPlaying) p.pause()
        } catch (_: Exception) {
        }
    }

    fun setVolume(v: Float) {
        try {
            player?.setVolume(v, v)
        } catch (_: Exception) {
        }
    }

    /**
     * Position in ms. MediaPlayer updates its position in coarse steps, so between updates
     * the value is extrapolated (at most 120 ms) to keep the moving roll smooth.
     */
    fun positionMs(): Float {
        val p = player ?: return lastPos.toFloat()
        val pos = try {
            p.currentPosition
        } catch (_: Exception) {
            return lastPos.toFloat()
        }
        val now = System.nanoTime()
        if (pos != lastPos) {
            lastPos = pos
            lastPosNs = now
            return pos.toFloat()
        }
        if (!playing) return pos.toFloat()
        val ahead = min(120f, (now - lastPosNs) / 1_000_000f * speed)
        return pos + ahead
    }

    fun release() {
        ready = false
        playing = false
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}

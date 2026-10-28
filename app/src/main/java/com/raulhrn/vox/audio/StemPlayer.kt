package com.raulhrn.vox.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.PlaybackParams
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.raulhrn.vox.core.Biquad
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/**
 * Plays the user's song and can separate the voice from the instrumental on the fly.
 *
 * Lead vocals are normally mixed to the centre of the stereo image, so:
 * - "Sem voz" keeps the side signal (L-R) plus the low end of the centre, which removes the
 *   centred voice but keeps bass and kick (the classic karaoke trick, stereo files only);
 * - "Voz em destaque" lifts the vocal band of the centre and lowers everything else.
 *
 * Decoding is streamed (MediaCodec) into an AudioTrack, so memory stays flat. The playback
 * head is the clock; it is calibrated once at non-1x speeds because devices differ on whether
 * the head counts content or output frames.
 */
class StemPlayer(private val context: Context, private val uri: Uri) {
    enum class Mix(val label: String) {
        ORIGINAL("Original"),
        INSTRUMENTAL("Sem voz"),
        VOICE("Voz em destaque"),
    }

    @Volatile
    var mix = Mix.ORIGINAL

    @Volatile
    private var volume = 1f

    @Volatile
    var ready = false
        private set

    @Volatile
    var failed = false
        private set

    /** Voice removal needs two different channels. */
    @Volatile
    var stereo = true
        private set

    @Volatile
    var durationMs = 0
        private set

    private var worker: Worker? = null

    @Volatile
    private var lastPosition = 0f

    fun prepare(onReady: () -> Unit) {
        if (ready || failed) return
        val main = Handler(Looper.getMainLooper())
        Thread {
            val ex = MediaExtractor()
            try {
                ex.setDataSource(context, uri, null)
                val format = audioFormat(ex) ?: throw IllegalArgumentException("sem áudio")
                durationMs = if (format.containsKey(MediaFormat.KEY_DURATION)) (format.getLong(MediaFormat.KEY_DURATION) / 1000).toInt() else 0
                stereo = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) >= 2
                ready = true
                main.post(onReady)
            } catch (_: Exception) {
                failed = true
            } finally {
                ex.release()
            }
        }.start()
    }

    fun play(fromMs: Int, speed: Float) {
        if (!ready) return
        stop()
        lastPosition = fromMs.toFloat()
        worker = Worker(fromMs, speed).also { it.start() }
    }

    fun pause() = stop()

    fun setVolume(v: Float) {
        volume = v
    }

    fun positionMs(): Float = worker?.positionMs() ?: lastPosition

    fun release() = stop()

    private fun stop() {
        val w = worker ?: return
        lastPosition = w.positionMs()
        w.alive = false
        worker = null
    }

    private fun audioFormat(ex: MediaExtractor): MediaFormat? {
        for (i in 0 until ex.trackCount) {
            val f = ex.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                ex.selectTrack(i)
                return f
            }
        }
        return null
    }

    private inner class Worker(private val fromMs: Int, private val speed: Float) : Thread("cantorial-player") {
        @Volatile
        var alive = true

        @Volatile
        private var track: AudioTrack? = null

        @Volatile
        private var rate = 44100

        @Volatile
        private var headScale = 1f

        @Volatile
        private var startedNs = 0L

        @Volatile
        private var calibrated = false

        fun positionMs(): Float {
            val t = track ?: return fromMs.toFloat()
            val head = try {
                t.playbackHeadPosition
            } catch (_: Exception) {
                return fromMs.toFloat()
            }
            if (!calibrated && speed != 1f && head > 0) {
                val elapsed = (System.nanoTime() - startedNs) / 1e9f
                if (elapsed > 0.8f) {
                    // Content frames advance at `speed` per real second, output frames at 1.
                    val ratio = head.toFloat() / rate / elapsed
                    headScale = if (abs(ratio - speed) < abs(ratio - 1f)) 1f else speed
                    calibrated = true
                }
            }
            return fromMs + head * 1000f / rate * headScale
        }

        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val ex = MediaExtractor()
            var codec: MediaCodec? = null
            var at: AudioTrack? = null
            try {
                ex.setDataSource(context, uri, null)
                val format = audioFormat(ex) ?: return
                ex.seekTo(fromMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
                codec = decoder
                decoder.configure(format, null, null, 0)
                decoder.start()

                var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                var pcmFloat = false
                val fromUs = fromMs * 1000L
                var bass = Biquad.lowPass(sampleRate.toFloat(), 150f)
                var vocalHigh = Biquad.highPass(sampleRate.toFloat(), 180f)
                var vocalLow = Biquad.lowPass(sampleRate.toFloat(), 4500f)
                var out = ShortArray(8192)
                var written = 0L

                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                while (alive && !outputDone) {
                    if (!inputDone) {
                        val inIndex = decoder.dequeueInputBuffer(5_000)
                        if (inIndex >= 0) {
                            val buf = decoder.getInputBuffer(inIndex)!!
                            val size = ex.readSampleData(buf, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, ex.sampleTime, 0)
                                ex.advance()
                            }
                        }
                    }
                    val outIndex = decoder.dequeueOutputBuffer(info, 5_000)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val f = decoder.outputFormat
                        sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        pcmFloat = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        bass = Biquad.lowPass(sampleRate.toFloat(), 150f)
                        vocalHigh = Biquad.highPass(sampleRate.toFloat(), 180f)
                        vocalLow = Biquad.lowPass(sampleRate.toFloat(), 4500f)
                        continue
                    }
                    if (outIndex < 0) continue
                    val buffer = decoder.getOutputBuffer(outIndex)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val ch = max(1, channels)
                        val bytesPerSample = if (pcmFloat) 4 else 2
                        val frames = info.size / bytesPerSample / ch
                        // Skip what lies before the seek target (seeking lands on a sync frame).
                        val skip = if (info.presentationTimeUs < fromUs) {
                            ((fromUs - info.presentationTimeUs) * sampleRate / 1_000_000L).toInt().coerceAtMost(frames)
                        } else {
                            0
                        }
                        if (frames - skip > 0) {
                            if (at == null) {
                                at = createTrack(sampleRate)
                                rate = sampleRate
                                track = at
                                startedNs = System.nanoTime()
                            }
                            if (out.size < (frames - skip) * 2) out = ShortArray((frames - skip) * 2)
                            val fb = if (pcmFloat) buffer.order(ByteOrder.nativeOrder()).asFloatBuffer() else null
                            val sb = if (pcmFloat) null else buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                            val mode = if (mix == Mix.INSTRUMENTAL && ch < 2) Mix.ORIGINAL else mix
                            val gain = volume
                            var o = 0
                            for (i in skip until frames) {
                                val base = i * ch
                                val l: Float
                                val r: Float
                                if (fb != null) {
                                    l = fb.get(base)
                                    r = if (ch > 1) fb.get(base + 1) else l
                                } else {
                                    l = sb!!.get(base) / 32768f
                                    r = if (ch > 1) sb.get(base + 1) / 32768f else l
                                }
                                val m = (l + r) * 0.5f
                                val side = (l - r) * 0.5f
                                // Filters always run so switching modes is click-free.
                                val low = bass.process(m)
                                val voice = vocalLow.process(vocalHigh.process(m))
                                var outL: Float
                                var outR: Float
                                when (mode) {
                                    Mix.ORIGINAL -> {
                                        outL = l
                                        outR = r
                                    }
                                    Mix.INSTRUMENTAL -> {
                                        outL = side * 1.4f + low
                                        outR = -side * 1.4f + low
                                    }
                                    Mix.VOICE -> {
                                        val rest = m - voice
                                        outL = voice * 1.2f + rest * 0.3f + side * 0.25f
                                        outR = voice * 1.2f + rest * 0.3f - side * 0.25f
                                    }
                                }
                                out[o++] = ((outL * gain).coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                                out[o++] = ((outR * gain).coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                            }
                            var off = 0
                            while (off < o && alive) {
                                val n = at!!.write(out, off, o - off)
                                if (n <= 0) break
                                off += n
                            }
                            written += o / 2
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
                // Let the tail of the song play out.
                val t = at
                while (alive && t != null && t.playbackHeadPosition < written) Thread.sleep(30)
            } catch (_: Exception) {
            } finally {
                track = null
                try {
                    at?.pause()
                    at?.flush()
                } catch (_: Exception) {
                }
                at?.release()
                try {
                    codec?.stop()
                } catch (_: Exception) {
                }
                codec?.release()
                ex.release()
            }
        }

        private fun createTrack(sampleRate: Int): AudioTrack {
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                )
                .setBufferSizeInBytes(max(minBuf, sampleRate / 5 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            if (speed != 1f) {
                try {
                    t.playbackParams = PlaybackParams().setSpeed(speed).setPitch(1f)
                } catch (_: Exception) {
                }
            }
            t.play()
            return t
        }
    }
}

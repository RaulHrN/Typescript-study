package com.raulhrn.vox.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.raulhrn.vox.core.NoteEvent
import com.raulhrn.vox.core.Segmenter
import com.raulhrn.vox.core.Biquad
import com.raulhrn.vox.core.MelodyExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Extracts the sung melody of an audio file: streaming decode (MediaCodec), anti-alias
 * filtering and decimation to ~22 kHz per channel, vocal melody extraction (which tells the
 * voice apart from the instruments) and note segmentation. Memory use stays flat regardless
 * of the song length because no PCM is kept.
 */
object FileAnalyzer {
    /** [voicedRatio]: share of the song where a voice was found. */
    class Result(val notes: List<NoteEvent>, val durationMs: Int, val voicedRatio: Float)

    suspend fun analyze(context: Context, uri: Uri, onProgress: (Float) -> Unit): Result = withContext(Dispatchers.Default) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) throw IllegalArgumentException("Arquivo sem áudio")
            extractor.selectTrack(trackIndex)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()

            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var pcmFloat = false
            var melodyExtractor: MelodyExtractor? = null
            var factor = 1
            var accL = 0f
            var accR = 0f
            var accCount = 0
            var lowL: Biquad? = null
            var lowR: Biquad? = null

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var lastProgress = -1
            while (!outputDone) {
                ensureActive()
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = decoder.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = decoder.dequeueOutputBuffer(info, 10_000)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val f = decoder.outputFormat
                    sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    pcmFloat = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                } else if (outIndex >= 0) {
                    if (melodyExtractor == null) {
                        factor = max(1, (sampleRate / 22050f).roundToInt())
                        melodyExtractor = MelodyExtractor(sampleRate.toFloat() / factor)
                        if (factor > 1) {
                            lowL = Biquad.lowPass(sampleRate.toFloat(), 0.42f * sampleRate / factor)
                            lowR = Biquad.lowPass(sampleRate.toFloat(), 0.42f * sampleRate / factor)
                        }
                    }
                    val ex = melodyExtractor
                    val out = decoder.getOutputBuffer(outIndex)
                    if (out != null && info.size > 0) {
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        val ch = max(1, channels)
                        // Stereo is kept: the extractor uses it to find the centred lead voice.
                        fun feed(l: Float, r: Float) {
                            accL += lowL?.process(l) ?: l
                            accR += lowR?.process(r) ?: r
                            if (++accCount == factor) {
                                ex.push(accL / factor, accR / factor)
                                accL = 0f
                                accR = 0f
                                accCount = 0
                            }
                        }
                        if (pcmFloat) {
                            val fb = out.order(ByteOrder.nativeOrder()).asFloatBuffer()
                            val n = fb.remaining() - fb.remaining() % ch
                            var i = 0
                            while (i < n) {
                                val l = fb.get(i)
                                feed(l, if (ch > 1) fb.get(i + 1) else l)
                                i += ch
                            }
                        } else {
                            val sb = out.order(ByteOrder.nativeOrder()).asShortBuffer()
                            val n = sb.remaining() - sb.remaining() % ch
                            val scale = 1f / 32768f
                            var i = 0
                            while (i < n) {
                                val l = sb.get(i) * scale
                                feed(l, if (ch > 1) sb.get(i + 1) * scale else l)
                                i += ch
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    if (durationUs > 0) {
                        val p = (info.presentationTimeUs * 100 / durationUs).toInt().coerceIn(0, 100)
                        if (p != lastProgress) {
                            lastProgress = p
                            onProgress(p / 100f)
                        }
                    }
                }
            }

            val ex = melodyExtractor ?: throw IllegalArgumentException("Não foi possível ler o áudio")
            val melody = ex.finish()
            val notes = Segmenter.segment(melody.track, melody.hopMs, melody.firstFrameMs, onsets = Segmenter.onsets(melody.levels))
                .filter { it.midi in 36..90 }
            val duration = if (durationUs > 0) (durationUs / 1000).toInt() else (notes.lastOrNull()?.endMs ?: 0)
            Result(notes, duration, melody.voicedRatio)
        } finally {
            try {
                codec?.stop()
            } catch (_: Exception) {
            }
            codec?.release()
            extractor.release()
        }
    }
}

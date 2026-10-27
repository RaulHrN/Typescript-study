package com.raulhrn.vox.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.raulhrn.vox.core.NoteEvent
import com.raulhrn.vox.core.Segmenter
import com.raulhrn.vox.core.TrackAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Extracts the main melody of an audio file: streaming decode (MediaCodec), downmix,
 * decimation to ~22 kHz, pitch tracking and note segmentation. Memory use stays flat
 * regardless of the song length because no PCM is kept.
 */
object FileAnalyzer {
    class Result(val notes: List<NoteEvent>, val durationMs: Int)

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
            var analyzer: TrackAnalyzer? = null
            var factor = 1
            var acc = 0f
            var accCount = 0

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
                    if (analyzer == null) {
                        factor = max(1, (sampleRate / 22050f).roundToInt())
                        analyzer = TrackAnalyzer(sampleRate.toFloat() / factor, minClarity = 0.8f, highPassHz = 110f)
                    }
                    val a = analyzer
                    val out = decoder.getOutputBuffer(outIndex)
                    if (out != null && info.size > 0) {
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        val ch = max(1, channels)
                        val inv = 1f / ch
                        if (pcmFloat) {
                            val fb = out.order(ByteOrder.nativeOrder()).asFloatBuffer()
                            val n = fb.remaining() - fb.remaining() % ch
                            var i = 0
                            while (i < n) {
                                var s = 0f
                                for (c in 0 until ch) s += fb.get(i + c)
                                acc += s * inv
                                if (++accCount == factor) {
                                    a.push(acc / factor)
                                    acc = 0f
                                    accCount = 0
                                }
                                i += ch
                            }
                        } else {
                            val sb = out.order(ByteOrder.nativeOrder()).asShortBuffer()
                            val n = sb.remaining() - sb.remaining() % ch
                            val scale = inv / 32768f
                            var i = 0
                            while (i < n) {
                                var s = 0
                                for (c in 0 until ch) s += sb.get(i + c)
                                acc += s * scale
                                if (++accCount == factor) {
                                    a.push(acc / factor)
                                    acc = 0f
                                    accCount = 0
                                }
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

            val a = analyzer ?: throw IllegalArgumentException("Não foi possível ler o áudio")
            val track = a.track()
            val notes = Segmenter.segment(track, a.hopMs, a.firstFrameMs, onsets = Segmenter.onsets(a.levels()))
                .filter { it.midi in 36..90 }
            val duration = if (durationUs > 0) (durationUs / 1000).toInt() else (notes.lastOrNull()?.endMs ?: 0)
            Result(notes, duration)
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

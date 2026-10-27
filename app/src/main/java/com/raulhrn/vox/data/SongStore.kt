package com.raulhrn.vox.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.raulhrn.vox.core.HarmonyMode
import com.raulhrn.vox.core.Key
import com.raulhrn.vox.core.NoteEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

data class Song(
    val id: String,
    val title: String,
    val audioUri: String?,
    val notes: List<NoteEvent>,
    val key: Key,
    val mode: HarmonyMode,
    val octave: Int,
    val createdAt: Long,
    val durationMs: Int,
)

/** Songs saved as one small JSON file. Reads and writes happen off the main thread. */
object SongStore {
    val songs = mutableStateListOf<Song>()
    val loaded = mutableStateOf(false)

    private lateinit var file: File
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        if (::file.isInitialized) return
        file = File(context.filesDir, "songs.json")
        io.execute {
            val list = try {
                read()
            } catch (_: Exception) {
                emptyList()
            }
            main.post {
                songs.clear()
                songs.addAll(list)
                loaded.value = true
            }
        }
    }

    fun find(id: String): Song? = songs.firstOrNull { it.id == id }

    fun upsert(song: Song) {
        val i = songs.indexOfFirst { it.id == song.id }
        if (i >= 0) songs[i] = song else songs.add(0, song)
        save()
    }

    fun delete(id: String) {
        songs.removeAll { it.id == id }
        save()
    }

    private fun save() {
        val snapshot = songs.toList()
        io.execute {
            try {
                val tmp = File(file.parentFile, "songs.json.tmp")
                tmp.writeText(encode(snapshot))
                if (!tmp.renameTo(file)) {
                    file.writeText(tmp.readText())
                    tmp.delete()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun read(): List<Song> {
        if (!file.exists()) return emptyList()
        val root = JSONObject(file.readText())
        val arr = root.optJSONArray("songs") ?: return emptyList()
        val out = ArrayList<Song>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val packed = o.getJSONArray("notes")
            val notes = ArrayList<NoteEvent>(packed.length() / 3)
            var k = 0
            while (k + 2 < packed.length()) {
                notes.add(NoteEvent(packed.getInt(k), packed.getInt(k + 1), packed.getInt(k + 2)))
                k += 3
            }
            out.add(
                Song(
                    id = o.getString("id"),
                    title = o.optString("title", "Sem título"),
                    audioUri = if (o.isNull("audio")) null else o.optString("audio").ifBlank { null },
                    notes = notes,
                    key = Key(o.optInt("tonic", 0), o.optBoolean("minor", false)),
                    mode = HarmonyMode.entries.firstOrNull { it.name == o.optString("mode") } ?: HarmonyMode.THIRD_ABOVE,
                    octave = o.optInt("octave", 0),
                    createdAt = o.optLong("created", 0L),
                    durationMs = o.optInt("duration", notes.lastOrNull()?.endMs ?: 0),
                ),
            )
        }
        return out
    }

    private fun encode(list: List<Song>): String {
        val arr = JSONArray()
        for (s in list) {
            val packed = JSONArray()
            for (n in s.notes) {
                packed.put(n.startMs)
                packed.put(n.durMs)
                packed.put(n.midi)
            }
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("audio", s.audioUri ?: JSONObject.NULL)
                    .put("tonic", s.key.tonic)
                    .put("minor", s.key.minor)
                    .put("mode", s.mode.name)
                    .put("octave", s.octave)
                    .put("created", s.createdAt)
                    .put("duration", s.durationMs)
                    .put("notes", packed),
            )
        }
        return JSONObject().put("version", 1).put("songs", arr).toString()
    }
}

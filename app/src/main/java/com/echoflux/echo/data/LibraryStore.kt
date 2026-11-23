package com.echoflux.echo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Post(
    val id: Long,
    val image: String,
    val songId: Long,
    val caption: String,
    val startMs: Long = 0L,
    val lenMs: Long = 30000L
)

class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("echo_library", Context.MODE_PRIVATE)

    fun loadName(): String = prefs.getString("name", "") ?: ""
    fun saveName(name: String) { prefs.edit().putString("name", name).apply() }
    fun loadAvatarName(): String? = prefs.getString("avatar", null)
    fun saveAvatarName(name: String?) { prefs.edit().putString("avatar", name).apply() }
    fun profileSeen(): Boolean = prefs.getBoolean("profileSeen", false)
    fun setProfileSeen() { prefs.edit().putBoolean("profileSeen", true).apply() }

    // ---- Profile extras ----

    fun loadBio(): String = prefs.getString("bio", "") ?: ""
    fun saveBio(bio: String) { prefs.edit().putString("bio", bio).apply() }

    fun loadCoverName(): String? = prefs.getString("cover", null)
    fun saveCoverName(name: String?) { prefs.edit().putString("cover", name).apply() }

    fun loadAnthem(): Long? =
        if (prefs.contains("anthem")) prefs.getLong("anthem", -1L) else null

    fun saveAnthem(id: Long?) {
        val e = prefs.edit()
        if (id == null) e.remove("anthem") else e.putLong("anthem", id)
        e.apply()
    }

    fun loadTop(): List<Long> =
        (prefs.getString("top", "") ?: "")
            .split(",")
            .mapNotNull { it.toLongOrNull() }

    fun saveTop(ids: List<Long>) {
        prefs.edit().putString("top", ids.joinToString(",")).apply()
    }

    // ---- Posts (photo + song + caption, stored on this phone only) ----

    fun loadPosts(): List<Post> {
        val raw = prefs.getString("posts", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Post(
                    o.getLong("id"), o.getString("image"), o.getLong("song"),
                    o.optString("caption", ""),
                    o.optLong("start", 0L), o.optLong("len", 30000L)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePosts(posts: List<Post>) {
        val arr = JSONArray()
        posts.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("image", p.image)
                    .put("song", p.songId)
                    .put("caption", p.caption)
                    .put("start", p.startMs)
                    .put("len", p.lenMs)
            )
        }
        prefs.edit().putString("posts", arr.toString()).apply()
    }

    // ---- Playlist covers ----

    fun loadPlaylistCovers(): Map<String, String> {
        val raw = prefs.getString("plcovers", null) ?: return emptyMap()
        val out = LinkedHashMap<String, String>()
        try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.getString(k)
            }
        } catch (e: Exception) {
            return emptyMap()
        }
        return out
    }

    fun savePlaylistCovers(covers: Map<String, String>) {
        val obj = JSONObject()
        for ((k, v) in covers) obj.put(k, v)
        prefs.edit().putString("plcovers", obj.toString()).apply()
    }

    // ---- Favorites and playlists ----

    fun loadFavorites(): Set<Long> =
        (prefs.getStringSet("fav", emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun saveFavorites(ids: Set<Long>) {
        prefs.edit().putStringSet("fav", ids.map { it.toString() }.toSet()).apply()
    }

    fun loadPlaylists(): Map<String, List<Long>> {
        val raw = prefs.getString("playlists", null) ?: return emptyMap()
        val out = LinkedHashMap<String, List<Long>>()
        try {
            val obj = JSONObject(raw)
            val names = obj.keys()
            while (names.hasNext()) {
                val name = names.next()
                val arr = obj.getJSONArray(name)
                out[name] = (0 until arr.length()).map { arr.getLong(it) }
            }
        } catch (e: Exception) {
            return emptyMap()
        }
        return out
    }

    fun savePlaylists(playlists: Map<String, List<Long>>) {
        val obj = JSONObject()
        for ((name, ids) in playlists) {
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            obj.put(name, arr)
        }
        prefs.edit().putString("playlists", obj.toString()).apply()
    }
}

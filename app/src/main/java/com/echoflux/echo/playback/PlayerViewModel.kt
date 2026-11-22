package com.echoflux.echo.playback

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.echoflux.echo.data.LibraryStore
import com.echoflux.echo.data.MusicRepository
import com.echoflux.echo.data.Post
import com.echoflux.echo.data.Song
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun mediaItemOf(s: Song): MediaItem =
    MediaItem.Builder()
        .setMediaId(s.id.toString())
        .setUri(s.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(s.title)
                .setArtist(s.artist)
                .setArtworkUri(s.albumArtUri)
                .build()
        )
        .build()

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository(app)
    private val store = LibraryStore(app)
    private var controller: MediaController? = null

    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var queue by mutableStateOf<List<Song>>(emptyList())
        private set
    var queueIndex by mutableStateOf(0)
        private set
    var current by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF)
        private set
    var favorites by mutableStateOf<Set<Long>>(store.loadFavorites())
        private set
    var playlists by mutableStateOf<Map<String, List<Long>>>(store.loadPlaylists())
        private set

    // ---- Profile state ----
    var userName by mutableStateOf(store.loadName())
        private set
    var bio by mutableStateOf(store.loadBio())
        private set
    var avatarFile by mutableStateOf<File?>(
        store.loadAvatarName()?.let { File(app.filesDir, it) }?.takeIf { it.exists() }
    )
        private set
    var coverFile by mutableStateOf<File?>(
        store.loadCoverName()?.let { File(app.filesDir, it) }?.takeIf { it.exists() }
    )
        private set
    var anthemId by mutableStateOf<Long?>(store.loadAnthem())
        private set
    var topIds by mutableStateOf<List<Long>>(store.loadTop())
        private set
    var posts by mutableStateOf<List<Post>>(store.loadPosts())
        private set
    var profileSeen by mutableStateOf(store.profileSeen())
        private set

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    current = queue.firstOrNull { it.id.toString() == item?.mediaId }
                    queueIndex = c.currentMediaItemIndex
                }

                override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                    shuffle = enabled
                }

                override fun onRepeatModeChanged(mode: Int) {
                    repeatMode = mode
                }
            })
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (true) {
                controller?.let {
                    positionMs = it.currentPosition
                    durationMs = it.duration.coerceAtLeast(0L)
                }
                delay(if (isPlaying) 100 else 250)
            }
        }
    }

    fun load() {
        viewModelScope.launch { songs = repo.loadSongs() }
    }

    // ---- Playback ----

    fun playFrom(list: List<Song>, index: Int, shuffleOn: Boolean? = null) {
        val c = controller ?: return
        if (list.isEmpty()) return
        if (shuffleOn != null) c.shuffleModeEnabled = shuffleOn
        queue = list
        c.setMediaItems(list.map { mediaItemOf(it) }, index, 0L)
        c.prepare()
        c.play()
    }

    fun shufflePlay(list: List<Song>) {
        if (list.isEmpty()) return
        playFrom(list, list.indices.random(), true)
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        if (queue.isEmpty()) {
            playFrom(listOf(song), 0)
            return
        }
        val at = (c.currentMediaItemIndex + 1).coerceAtMost(queue.size)
        c.addMediaItem(at, mediaItemOf(song))
        queue = queue.toMutableList().apply { add(at, song) }
    }

    fun jumpTo(index: Int) {
        val c = controller ?: return
        c.seekToDefaultPosition(index)
        c.play()
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    // ---- Favorites and playlists ----

    fun toggleFavorite(song: Song) {
        favorites = if (song.id in favorites) favorites - song.id else favorites + song.id
        store.saveFavorites(favorites)
    }

    fun createPlaylist(name: String, first: Song? = null) {
        val n = name.trim()
        if (n.isEmpty()) return
        if (n in playlists) {
            if (first != null) addToPlaylist(n, first)
            return
        }
        val ids: List<Long> = if (first != null) listOf(first.id) else emptyList()
        playlists = playlists + (n to ids)
        store.savePlaylists(playlists)
    }

    fun addToPlaylist(name: String, song: Song) {
        val cur = playlists[name] ?: return
        if (song.id in cur) return
        playlists = playlists + (name to (cur + song.id))
        store.savePlaylists(playlists)
    }

    fun deletePlaylist(name: String) {
        playlists = playlists - name
        store.savePlaylists(playlists)
    }

    // ---- Profile ----

    fun saveName(name: String) {
        userName = name.trim()
        store.saveName(userName)
    }

    fun saveProfile(name: String, bioText: String) {
        saveName(name)
        bio = bioText.trim()
        store.saveBio(bio)
    }

    fun markProfileSeen() {
        profileSeen = true
        store.setProfileSeen()
    }

    private suspend fun copyImage(uri: Uri, prefix: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val out = File(ctx.filesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
                if (out.length() > 0) out else null
            } catch (e: Exception) {
                null
            }
        }

    fun setAvatar(uri: Uri) {
        viewModelScope.launch {
            val file = copyImage(uri, "avatar")
            if (file != null) {
                avatarFile?.delete()
                avatarFile = file
                store.saveAvatarName(file.name)
            }
        }
    }

    fun setCover(uri: Uri) {
        viewModelScope.launch {
            val file = copyImage(uri, "cover")
            if (file != null) {
                coverFile?.delete()
                coverFile = file
                store.saveCoverName(file.name)
            }
        }
    }

    fun removeCover() {
        coverFile?.delete()
        coverFile = null
        store.saveCoverName(null)
    }

    fun setAnthem(id: Long?) {
        anthemId = id
        store.saveAnthem(id)
    }

    fun setTopSongs(ids: List<Long>) {
        topIds = ids.take(5)
        store.saveTop(topIds)
    }

    fun postFile(p: Post): File = File(getApplication<Application>().filesDir, p.image)

    fun addPost(uri: Uri, songId: Long, caption: String) {
        viewModelScope.launch {
            val file = copyImage(uri, "post") ?: return@launch
            val post = Post(System.currentTimeMillis(), file.name, songId, caption.trim())
            posts = listOf(post) + posts
            store.savePosts(posts)
        }
    }

    fun deletePost(id: Long) {
        posts.firstOrNull { it.id == id }?.let { postFile(it).delete() }
        posts = posts.filterNot { it.id == id }
        store.savePosts(posts)
    }

    override fun onCleared() {
        controller?.release()
    }
}

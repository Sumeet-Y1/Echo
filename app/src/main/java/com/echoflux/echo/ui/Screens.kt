package com.echoflux.echo.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.echoflux.echo.data.Song
import com.echoflux.echo.playback.PlayerViewModel
import com.echoflux.echo.ui.theme.GlassBorder
import com.echoflux.echo.ui.theme.Ink
import com.echoflux.echo.ui.theme.Lime
import com.echoflux.echo.ui.theme.OnLime
import com.echoflux.echo.ui.theme.TextMid
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

sealed interface Detail {
    data class AlbumD(val albumId: Long) : Detail
    data class ArtistD(val name: String) : Detail
    data class PlaylistD(val name: String) : Detail
    object FavoritesD : Detail
}

class PlaylistUi {
    var picker by mutableStateOf<Song?>(null)
    var newOpen by mutableStateOf(false)
    var newFor by mutableStateOf<Song?>(null)
}

private data class AlbumItem(val id: Long, val name: String, val artist: String, val songs: List<Song>)
private data class ArtistItem(val name: String, val songs: List<Song>)

private fun buildAlbums(songs: List<Song>): List<AlbumItem> =
    songs.groupBy { it.albumId }.values
        .map { g -> AlbumItem(g.first().albumId, g.first().album, g.first().artist, g) }
        .sortedBy { it.name.lowercase() }

private fun buildRecent(songs: List<Song>): List<AlbumItem> =
    songs.groupBy { it.albumId }.values
        .map { g -> AlbumItem(g.first().albumId, g.first().album, g.first().artist, g) }
        .sortedByDescending { a -> a.songs.maxOf { it.id } }
        .take(15)

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun resolveDetail(d: Detail, vm: PlayerViewModel): Pair<String, List<Song>> {
    val all = vm.songs
    return when (d) {
        is Detail.AlbumD -> {
            val l = all.filter { it.albumId == d.albumId }
            (l.firstOrNull()?.album ?: "Album") to l
        }
        is Detail.ArtistD -> d.name to all.filter { it.artist == d.name }
        is Detail.PlaylistD -> {
            val byId = all.associateBy { it.id }
            d.name to (vm.playlists[d.name] ?: emptyList()).mapNotNull { byId[it] }
        }
        Detail.FavoritesD -> "Favorites" to all.filter { it.id in vm.favorites }
    }
}

/** How many albums the Home carousel shows, so the pager state can be created up in the app. */
fun recentAlbumCount(songs: List<Song>): Int =
    songs.map { it.albumId }.distinct().size.coerceAtMost(15)

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(
    vm: PlayerViewModel,
    pagerState: PagerState,
    listState: LazyListState,
    bottomPad: Dp,
    onOpen: (Detail) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onProfile: () -> Unit
) {
    val songs = vm.songs
    val recent = remember(songs) { buildRecent(songs) }
    var onlyFav by rememberSaveable { mutableStateOf(false) }
    val shown = if (onlyFav) songs.filter { it.id in vm.favorites } else songs

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = bottomPad)
    ) {
        item(key = "hero") {
            val centeredArt = recent.getOrNull(pagerState.currentPage)?.songs?.firstOrNull()?.albumArtUri
            Box(Modifier.fillMaxWidth()) {
                Crossfade(
                    targetState = centeredArt,
                    modifier = Modifier.matchParentSize(),
                    animationSpec = tween(700),
                    label = "homeBackdrop"
                ) { uri -> ArtBackdrop(uri, Modifier.fillMaxSize(), 0.6f) }
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            0f to Ink.copy(alpha = 0.35f),
                            0.5f to Ink.copy(alpha = 0.1f),
                            1f to Ink
                        )
                    )
                )
                Column(Modifier.statusBarsPadding().padding(bottom = 20.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(vm, 48.dp, Modifier.clickable { onProfile() })
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                greeting(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                if (vm.userName.isBlank()) "Hi there" else "Hi, ${vm.userName}",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        GlassCircle(onClick = { vm.shufflePlay(songs) }) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle all songs",
                                tint = Color.White
                            )
                        }
                    }
                    if (recent.isNotEmpty()) {
                        Text(
                            "Recently added",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                        AlbumCarousel(recent, pagerState) { onOpen(Detail.AlbumD(it)) }
                    }
                }
            }
        }
        item(key = "songsHeader") {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)) {
                Text("Songs", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EchoChip("All", !onlyFav) { onlyFav = false }
                    EchoChip("Favorites", onlyFav) { onlyFav = true }
                }
            }
        }
        if (shown.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    if (onlyFav) "No favorites yet. Tap the heart on a song."
                    else "No music found on this phone"
                )
            }
        }
        itemsIndexed(shown, key = { _, s -> s.id }) { index, song ->
            SongRow(vm, song, { vm.playFrom(shown, index) }, onAddToPlaylist)
        }
    }
}

@Composable
private fun AlbumCarousel(albums: List<AlbumItem>, pagerState: PagerState, onOpen: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 92.dp),
        pageSpacing = 4.dp,
        key = { albums[it].id }
    ) { page ->
        val a = albums[page]
        Column(
            Modifier
                .graphicsLayer {
                    val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val d = offset.absoluteValue.coerceIn(0f, 1f)
                    val s = lerp(1f, 0.8f, d)
                    scaleX = s
                    scaleY = s
                    alpha = lerp(1f, 0.5f, d)
                    rotationY = offset.coerceIn(-1f, 1f) * 24f
                    cameraDistance = 14f * density
                }
                .clickable {
                    if (pagerState.currentPage == page) onOpen(a.id)
                    else scope.launch { pagerState.animateScrollToPage(page) }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArtImage(
                a.songs.first().albumArtUri,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(1.dp, GlassBorder, RoundedCornerShape(30.dp)),
                RoundedCornerShape(30.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                a.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                a.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

// ---------------------------------------------------------------- Search

@Composable
fun SearchScreen(vm: PlayerViewModel, bottomPad: Dp, onAddToPlaylist: (Song) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val songs = vm.songs
    val results = remember(songs, q) {
        if (q.isEmpty()) emptyList()
        else songs.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Search")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Songs, artists, albums", color = TextMid) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMid) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lime,
                unfocusedBorderColor = GlassBorder,
                cursorColor = Lime
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        when {
            q.isEmpty() -> EmptyState("Type to search your music")
            results.isEmpty() -> EmptyState("No matches for \"$q\"")
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad)) {
                itemsIndexed(results, key = { _, s -> s.id }) { index, song ->
                    SongRow(vm, song, { vm.playFrom(results, index) }, onAddToPlaylist)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Browse

@Composable
fun BrowseScreen(vm: PlayerViewModel, bottomPad: Dp, onOpen: (Detail) -> Unit) {
    var mode by rememberSaveable { mutableStateOf(0) }
    val songs = vm.songs
    val albums = remember(songs) { buildAlbums(songs) }
    val artists = remember(songs) {
        songs.groupBy { it.artist }
            .map { ArtistItem(it.key, it.value) }
            .sortedBy { it.name.lowercase() }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Browse")
        Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EchoChip("Albums", mode == 0) { mode = 0 }
            EchoChip("Artists", mode == 1) { mode = 1 }
        }
        Spacer(Modifier.height(8.dp))
        if (mode == 0) {
            if (albums.isEmpty()) EmptyState("No albums yet")
            else LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPad),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(albums, key = { it.id }) { a ->
                    Column(Modifier.clickable { onOpen(Detail.AlbumD(a.id)) }) {
                        ArtImage(
                            a.songs.first().albumArtUri,
                            Modifier.fillMaxWidth().aspectRatio(1f),
                            RoundedCornerShape(24.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            a.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            a.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMid
                        )
                    }
                }
            }
        } else {
            if (artists.isEmpty()) EmptyState("No artists yet")
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad)) {
                items(artists, key = { it.name }) { a ->
                    ListRow(
                        leading = {
                            ArtImage(a.songs.first().albumArtUri, Modifier.size(56.dp), CircleShape)
                        },
                        title = a.name,
                        subtitle = "${a.songs.size} songs",
                        onClick = { onOpen(Detail.ArtistD(a.name)) }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Playlists

@Composable
fun PlaylistsScreen(
    vm: PlayerViewModel,
    bottomPad: Dp,
    onOpen: (Detail) -> Unit,
    onNew: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Playlists")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad)) {
            item(key = "favorites") {
                ListRow(
                    leading = { IconTile(Icons.Default.Favorite) },
                    title = "Favorites",
                    subtitle = "${vm.favorites.size} songs",
                    onClick = { onOpen(Detail.FavoritesD) }
                )
            }
            items(vm.playlists.keys.toList(), key = { it }) { name ->
                ListRow(
                    leading = { IconTile(Icons.AutoMirrored.Filled.QueueMusic) },
                    title = name,
                    subtitle = "${vm.playlists[name]?.size ?: 0} songs",
                    onClick = { onOpen(Detail.PlaylistD(name)) },
                    trailing = {
                        IconButton(onClick = { vm.deletePlaylist(name) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete playlist $name",
                                tint = TextMid
                            )
                        }
                    }
                )
            }
            item(key = "new") {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    LimeButton("New playlist", Icons.Default.Add, onNew)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Detail page

@Composable
fun DetailScreen(
    d: Detail,
    vm: PlayerViewModel,
    bottomPad: Dp,
    onBack: () -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    val (title, list) = resolveDetail(d, vm)
    val art = list.firstOrNull()?.albumArtUri

    Box(Modifier.fillMaxSize().background(Ink).blockTouches()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad)) {
            item(key = "top") {
                Box(Modifier.fillMaxWidth()) {
                    ArtBackdrop(art, Modifier.matchParentSize(), 0.55f)
                    Box(
                        Modifier.matchParentSize().background(
                            Brush.verticalGradient(
                                0f to Ink.copy(alpha = 0.3f),
                                1f to Ink
                            )
                        )
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            GlassCircle(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        ArtImage(art, Modifier.size(200.dp), RoundedCornerShape(32.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${list.size} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LimeButton("Play", Icons.Default.PlayArrow, {
                                vm.playFrom(list, 0, false)
                            }, enabled = list.isNotEmpty())
                            GlassCircle(onClick = { vm.shufflePlay(list) }, size = 48.dp) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "Shuffle play",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
            if (list.isEmpty()) {
                item(key = "empty") { EmptyState("Nothing here yet") }
            }
            itemsIndexed(list, key = { _, s -> s.id }) { index, song ->
                SongRow(vm, song, { vm.playFrom(list, index) }, onAddToPlaylist)
            }
        }
    }
}

// ---------------------------------------------------------------- Playlist dialogs

@Composable
fun PlaylistDialogs(vm: PlayerViewModel, ui: PlaylistUi) {
    ui.picker?.let { s ->
        AlertDialog(
            onDismissRequest = { ui.picker = null },
            title = { Text("Add to playlist") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = {
                        ui.newFor = s
                        ui.newOpen = true
                        ui.picker = null
                    }) { Text("New playlist", color = Lime) }
                    vm.playlists.keys.forEach { name ->
                        TextButton(onClick = {
                            vm.addToPlaylist(name, s)
                            ui.picker = null
                        }) { Text(name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { ui.picker = null }) { Text("Cancel") } }
        )
    }

    if (ui.newOpen) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { ui.newOpen = false; ui.newFor = null },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Playlist name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Lime,
                        cursorColor = Lime
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createPlaylist(name, ui.newFor)
                    ui.newOpen = false
                    ui.newFor = null
                }) { Text("Create", color = Lime) }
            },
            dismissButton = {
                TextButton(onClick = { ui.newOpen = false; ui.newFor = null }) { Text("Cancel") }
            }
        )
    }
}

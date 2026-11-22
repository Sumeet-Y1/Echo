@file:OptIn(ExperimentalMaterial3Api::class)

package com.echoflux.echo.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.echoflux.echo.data.Post
import com.echoflux.echo.data.Song
import com.echoflux.echo.playback.PlayerViewModel
import com.echoflux.echo.ui.theme.Glass
import com.echoflux.echo.ui.theme.GlassBorder
import com.echoflux.echo.ui.theme.Ink
import com.echoflux.echo.ui.theme.Lime
import com.echoflux.echo.ui.theme.OnLime
import com.echoflux.echo.ui.theme.Surface1
import com.echoflux.echo.ui.theme.Surface2
import com.echoflux.echo.ui.theme.TextHigh
import com.echoflux.echo.ui.theme.TextMid

// =====================================================================
// First-run welcome sheet (name + photo). Shown once.
// =====================================================================

@Composable
fun ProfileSheet(vm: PlayerViewModel, firstRun: Boolean, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(vm.userName) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.setAvatar(uri) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface1) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (firstRun) "Welcome to Echo" else "Your profile",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.clickable {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                contentAlignment = Alignment.BottomEnd
            ) {
                Avatar(vm, 104.dp)
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .border(2.dp, Surface1, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change photo",
                        tint = Lime,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap the photo to change it",
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                singleLine = true,
                label = { Text("Your name") },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lime,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = Lime
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            LimeButton(
                if (firstRun) "Let's go" else "Save",
                Icons.Default.Check,
                { vm.saveName(name); onDismiss() }
            )
        }
    }
}

// =====================================================================
// Profile page
// =====================================================================

private enum class PickMode { Anthem, Top }

private fun formatRuntime(ms: Long): String {
    val totalMin = ms / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun ProfileScreen(vm: PlayerViewModel, bottomPad: Dp, onBack: () -> Unit) {
    val songs = vm.songs
    val byId = remember(songs) { songs.associateBy { it.id } }
    val anthem = vm.anthemId?.let { byId[it] }
    val topIds = vm.topIds
    val top = remember(topIds, byId) { topIds.mapNotNull { byId[it] } }
    val runtime = remember(songs) { formatRuntime(songs.sumOf { it.durationMs }) }
    val topArtists = remember(songs) {
        songs.groupingBy { it.artist }.eachCount()
            .filterKeys { !it.startsWith("<") && !it.equals("Unknown artist", true) }
            .entries
            .sortedByDescending { it.value }
            .take(6)
    }

    var showEdit by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf<PickMode?>(null) }
    var newPost by remember { mutableStateOf(false) }
    var openPost by remember { mutableStateOf<Post?>(null) }
    val postRows = remember(vm.posts) { vm.posts.chunked(2) }

    Box(Modifier.fillMaxSize().background(Ink).blockTouches()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPad)) {
            item(key = "header") {
                ProfileHeader(vm, anthem, onBack) { showEdit = true }
            }
            item(key = "stats") {
                Column {
                    Spacer(Modifier.height(22.dp))
                    StatsStrip(
                        listOf(
                            songs.size.toString() to "Songs",
                            vm.favorites.size.toString() to "Favorites",
                            vm.playlists.size.toString() to "Playlists",
                            runtime to "Of music"
                        )
                    )
                }
            }
            item(key = "anthem") {
                Column {
                    SectionHeader("My anthem", if (anthem != null) "Change" else null) {
                        pick = PickMode.Anthem
                    }
                    if (anthem != null) {
                        AnthemCard(vm, anthem)
                    } else {
                        EmptyPickCard("Pick the song that sums you up") { pick = PickMode.Anthem }
                    }
                }
            }
            item(key = "top") {
                Column {
                    SectionHeader("Top tracks", if (top.isNotEmpty()) "Edit" else null) {
                        pick = PickMode.Top
                    }
                    if (top.isEmpty()) {
                        EmptyPickCard("Pin up to 5 songs you can't live without") {
                            pick = PickMode.Top
                        }
                    } else {
                        top.forEachIndexed { i, s ->
                            TopRow(vm, i, s) { vm.playFrom(top, i) }
                        }
                    }
                }
            }
            item(key = "postsHeader") {
                Column {
                    SectionHeader("Posts", "New post") { newPost = true }
                    if (postRows.isEmpty()) {
                        EmptyPickCard("Share a photo with a song from your library") {
                            newPost = true
                        }
                    }
                }
            }
            postRows.forEach { row ->
                item(key = "post_${row.first().id}") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { p ->
                            PostTile(vm, p, byId[p.songId], Modifier.weight(1f)) { openPost = p }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (topArtists.isNotEmpty()) {
                item(key = "artists") {
                    Column {
                        SectionHeader("Artists you collect", null) {}
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            topArtists.forEach { e -> ArtistChip(e.key, e.value) }
                        }
                    }
                }
            }
            item(key = "footer") {
                Text(
                    "Everything on this page is stored only on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp)
                )
            }
        }
    }

    if (showEdit) {
        EditProfileSheet(vm) { showEdit = false }
    }
    if (newPost) {
        NewPostSheet(vm) { newPost = false }
    }
    openPost?.let { p ->
        PostViewerSheet(vm, p) { openPost = null }
    }

    when (pick) {
        PickMode.Anthem -> SongPickerSheet(
            songs = songs,
            title = "My anthem",
            subtitle = "One song that sums you up",
            multi = false,
            max = 1,
            initial = listOfNotNull(vm.anthemId),
            onClear = if (vm.anthemId != null) ({ vm.setAnthem(null); pick = null }) else null,
            onDone = { vm.setAnthem(it.firstOrNull()); pick = null },
            onDismiss = { pick = null }
        )
        PickMode.Top -> SongPickerSheet(
            songs = songs,
            title = "Top tracks",
            subtitle = "Pick up to 5, in the order you love them",
            multi = true,
            max = 5,
            initial = vm.topIds,
            onClear = null,
            onDone = { vm.setTopSongs(it); pick = null },
            onDismiss = { pick = null }
        )
        null -> {}
    }
}

@Composable
private fun ProfileHeader(
    vm: PlayerViewModel,
    anthem: Song?,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val cover = vm.coverFile
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            when {
                cover != null -> AsyncImage(
                    model = cover,
                    contentDescription = "Cover photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                anthem != null -> ArtBackdrop(anthem.albumArtUri, Modifier.fillMaxSize(), 0.75f)
                else -> Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Lime.copy(alpha = 0.28f), Ink))
                    )
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Ink.copy(alpha = 0.35f),
                        0.55f to Color.Transparent,
                        1f to Ink
                    )
                )
            )
        }

        Column(
            Modifier.fillMaxWidth().statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCircle(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                GlassCircle(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit profile", tint = Color.White)
                }
            }
            Spacer(Modifier.height(96.dp))
            Box(
                Modifier
                    .size(116.dp)
                    .background(Ink, CircleShape)
                    .border(3.dp, Lime, CircleShape)
                    .padding(6.dp)
            ) {
                Avatar(vm, 104.dp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (vm.userName.isBlank()) "Music lover" else vm.userName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(6.dp))
            val noBio = vm.bio.isBlank()
            Text(
                if (noBio) "Add a bio with the pencil" else vm.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = if (noBio) TextMid else Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 36.dp)
            )
        }
    }
}

@Composable
private fun StatsStrip(items: List<Pair<String, String>>) {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(26.dp))
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { i, (value, label) ->
            if (i > 0) {
                Box(Modifier.width(1.dp).height(30.dp).background(GlassBorder))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    value,
                    color = Lime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = TextMid)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, color = Lime) }
        }
    }
}

@Composable
private fun AnthemCard(vm: PlayerViewModel, song: Song) {
    val isCurrent = vm.current?.id == song.id
    val play: () -> Unit = {
        if (isCurrent) vm.toggle() else vm.playFrom(listOf(song), 0)
    }
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Surface1)
            .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
            .clickable { play() }
    ) {
        ArtBackdrop(song.albumArtUri, Modifier.matchParentSize(), 0.4f)
        Box(
            Modifier.matchParentSize().background(
                Brush.horizontalGradient(
                    0f to Ink.copy(alpha = 0.2f),
                    1f to Ink.copy(alpha = 0.75f)
                )
            )
        )
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtImage(song.albumArtUri, Modifier.size(76.dp), RoundedCornerShape(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "On repeat",
                    color = Lime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent && vm.isPlaying) {
                        EqBars(true, Lime)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        song.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .bouncyClick { play() }
                    .clip(CircleShape)
                    .background(Lime),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCurrent && vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrent && vm.isPlaying) "Pause" else "Play anthem",
                    tint = OnLime
                )
            }
        }
    }
}

@Composable
private fun EmptyPickCard(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Lime),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = OnLime)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
    }
}

@Composable
private fun TopRow(vm: PlayerViewModel, index: Int, song: Song, onPlay: () -> Unit) {
    val isCurrent = vm.current?.id == song.id
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${index + 1}",
            color = Lime,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
        ArtImage(song.albumArtUri, Modifier.size(52.dp), RoundedCornerShape(16.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) Lime else TextHigh
            )
            Text(
                song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
        }
        if (isCurrent) EqBars(vm.isPlaying, Lime)
    }
}

@Composable
private fun ArtistChip(name: String, count: Int) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Glass)
            .border(1.dp, GlassBorder, CircleShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(count.toString(), style = MaterialTheme.typography.bodySmall, color = Lime)
    }
}

// =====================================================================
// Edit profile sheet
// =====================================================================

@Composable
private fun EditProfileSheet(vm: PlayerViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(vm.userName) }
    var bio by remember { mutableStateOf(vm.bio) }
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.setAvatar(uri) }
    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.setCover(uri) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Lime,
        unfocusedBorderColor = GlassBorder,
        cursorColor = Lime
    )
    val cover = vm.coverFile

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Edit profile", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface2)
                    .clickable {
                        coverPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    AsyncImage(
                        model = cover,
                        contentDescription = "Cover photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Ink.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Lime,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (cover != null) "Change cover" else "Add cover photo",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
            if (cover != null) {
                TextButton(onClick = { vm.removeCover() }) {
                    Text("Remove cover", color = TextMid)
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }

            Box(
                Modifier.clickable {
                    avatarPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                contentAlignment = Alignment.BottomEnd
            ) {
                Avatar(vm, 96.dp)
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .border(2.dp, Surface1, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change photo",
                        tint = Lime,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Profile photo", style = MaterialTheme.typography.bodySmall, color = TextMid)
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                singleLine = true,
                label = { Text("Your name") },
                shape = RoundedCornerShape(20.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 120) bio = it },
                minLines = 2,
                maxLines = 4,
                label = { Text("Bio") },
                supportingText = { Text("${bio.length}/120", color = TextMid) },
                shape = RoundedCornerShape(20.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            LimeButton(
                "Save changes",
                Icons.Default.Check,
                { vm.saveProfile(name, bio); onDismiss() }
            )
        }
    }
}

// =====================================================================
// Song picker (anthem = pick one, top tracks = pick up to 5)
// =====================================================================

@Composable
private fun SongPickerSheet(
    songs: List<Song>,
    title: String,
    subtitle: String,
    multi: Boolean,
    max: Int,
    initial: List<Long>,
    onClear: (() -> Unit)?,
    onDone: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var chosen by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val shown = remember(songs, q) {
        if (q.isEmpty()) songs
        else songs.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMid)
                }
                if (onClear != null) {
                    TextButton(onClick = onClear) { Text("Remove", color = TextMid) }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search your songs", color = TextMid) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMid)
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lime,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = Lime
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
            )
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                items(shown, key = { it.id }) { s ->
                    val idx = chosen.indexOf(s.id)
                    val on = idx >= 0
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!multi) {
                                    onDone(listOf(s.id))
                                } else {
                                    chosen = if (on) chosen - s.id
                                    else if (chosen.size < max) chosen + s.id
                                    else chosen
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtImage(s.albumArtUri, Modifier.size(46.dp), RoundedCornerShape(14.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (on) Lime else TextHigh
                            )
                            Text(
                                s.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMid
                            )
                        }
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (on) Lime else Color.Transparent)
                                .border(1.5.dp, if (on) Lime else GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (on) {
                                if (multi) {
                                    Text(
                                        "${idx + 1}",
                                        color = OnLime,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = OnLime,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (multi) {
                Box(
                    Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LimeButton(
                        "Save ${chosen.size} of $max",
                        Icons.Default.Check,
                        { onDone(chosen) }
                    )
                }
            }
        }
    }
}

// =====================================================================
// Posts: a photo + a song from your library + a caption
// =====================================================================

@Composable
private fun PostTile(
    vm: PlayerViewModel,
    post: Post,
    song: Song?,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isCurrent = song != null && vm.current?.id == song.id
    Box(
        modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(22.dp))
            .background(Surface2)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = vm.postFile(post),
            contentDescription = "Post photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.5f to Color.Transparent,
                    1f to Ink.copy(alpha = 0.85f)
                )
            )
        )
        if (song != null) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtImage(song.albumArtUri, Modifier.size(26.dp), RoundedCornerShape(8.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrent && vm.isPlaying) {
                    Spacer(Modifier.width(6.dp))
                    EqBars(true, Lime)
                }
            }
        }
    }
}

@Composable
private fun NewPostSheet(vm: PlayerViewModel, onDismiss: () -> Unit) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var songId by remember { mutableStateOf<Long?>(null) }
    var caption by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) imageUri = uri }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val song = remember(vm.songs, songId) { vm.songs.firstOrNull { it.id == songId } }
    val pickPhoto = {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("New post", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(0.8f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface2)
                    .clickable { pickPhoto() },
                contentAlignment = Alignment.Center
            ) {
                val uri = imageUri
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Chosen photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Lime)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Choose a photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMid
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Glass)
                    .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                    .clickable { showPicker = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (song != null) {
                    ArtImage(song.albumArtUri, Modifier.size(44.dp), RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            song.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMid
                        )
                    }
                    Text("Change", color = Lime, style = MaterialTheme.typography.labelLarge)
                } else {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Lime),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = OnLime)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Attach a song from your library",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 140) caption = it },
                minLines = 2,
                maxLines = 4,
                label = { Text("Caption") },
                supportingText = { Text("${caption.length}/140", color = TextMid) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lime,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = Lime
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            val readyUri = imageUri
            val readySong = songId
            if (readyUri != null && readySong != null) {
                LimeButton(
                    "Post",
                    Icons.Default.Check,
                    { vm.addPost(readyUri, readySong, caption); onDismiss() }
                )
            } else {
                Text(
                    "Choose a photo and a song to post",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
            }
        }
    }

    if (showPicker) {
        SongPickerSheet(
            songs = vm.songs,
            title = "Attach a song",
            subtitle = "It plays when you tap the post",
            multi = false,
            max = 1,
            initial = listOfNotNull(songId),
            onClear = null,
            onDone = { songId = it.firstOrNull(); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun PostViewerSheet(vm: PlayerViewModel, post: Post, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val song = remember(vm.songs, post.songId) { vm.songs.firstOrNull { it.id == post.songId } }
    val isCurrent = song != null && vm.current?.id == song.id

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = vm.postFile(post),
                contentDescription = "Post photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.9f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Surface2)
            )
            if (post.caption.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(14.dp))
            if (song != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Glass)
                        .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtImage(song.albumArtUri, Modifier.size(48.dp), RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            song.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMid
                        )
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .bouncyClick {
                                if (isCurrent) vm.toggle() else vm.playFrom(listOf(song), 0)
                            }
                            .clip(CircleShape)
                            .background(Lime),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isCurrent && vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isCurrent && vm.isPlaying) "Pause" else "Play",
                            tint = OnLime
                        )
                    }
                }
            } else {
                Text(
                    "This song is no longer on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.deletePost(post.id); onDismiss() }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = TextMid,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Delete post", color = TextMid)
            }
        }
    }
}

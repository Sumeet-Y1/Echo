@file:OptIn(ExperimentalMaterial3Api::class)

package com.echoflux.echo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.echoflux.echo.playback.PlayerViewModel
import com.echoflux.echo.ui.theme.Glass
import com.echoflux.echo.ui.theme.GlassBorder
import com.echoflux.echo.ui.theme.Ink
import com.echoflux.echo.ui.theme.Lime
import com.echoflux.echo.ui.theme.OnLime
import com.echoflux.echo.ui.theme.Surface1
import com.echoflux.echo.ui.theme.TextHigh
import com.echoflux.echo.ui.theme.TextMid

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun NowPlayingScreen(vm: PlayerViewModel, onClose: () -> Unit) {
    val song = vm.current ?: return
    val context = LocalContext.current

    BackHandler { onClose() }

    // Dominant dark color from the album art tints the whole screen
    var accent by remember { mutableStateOf(Color(0xFF2A3318)) }
    LaunchedEffect(song.id) {
        val request = ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .allowHardware(false)
            .size(200)
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap = (result as? SuccessResult)?.drawable?.toBitmap()
        if (bitmap != null) {
            val p = Palette.from(bitmap).generate()
            val fallback = 0xFF2A3318.toInt()
            val argb = p.getDarkVibrantColor(p.getDarkMutedColor(p.getDominantColor(fallback)))
            accent = Color(argb)
        }
    }
    val tint by animateColorAsState(accent, tween(800), label = "tint")

    // The disc spins while playing and eases to a stop when paused
    var angle by remember { mutableStateOf(0f) }
    LaunchedEffect(vm.isPlaying) {
        if (vm.isPlaying) {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                angle = (angle + (now - last) / 1_000_000_000f * 14f) % 360f
                last = now
            }
        }
    }
    val discScale by animateFloatAsState(
        if (vm.isPlaying) 1f else 0.9f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "discScale"
    )

    var showQueue by remember { mutableStateOf(false) }
    val fav = song.id in vm.favorites

    Box(Modifier.fillMaxSize().background(Ink).blockTouches()) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tint, Ink))))
        Crossfade(
            targetState = song.albumArtUri,
            animationSpec = tween(800),
            label = "playerBackdrop"
        ) { uri -> ArtBackdrop(uri, Modifier.fillMaxSize(), 0.45f) }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to Ink.copy(alpha = 0.6f),
                    1f to Ink
                )
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCircle(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, "Close player", tint = Color.White)
                }
                Text(
                    "Now playing",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                GlassCircle(onClick = { vm.toggleFavorite(song) }) {
                    Icon(
                        if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (fav) "Remove from favorites" else "Add to favorites",
                        tint = if (fav) Lime else Color.White
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = discScale
                        scaleY = discScale
                        rotationZ = angle
                    }
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            ) {
                ArtImage(song.albumArtUri, Modifier.fillMaxSize(), CircleShape)
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Ink)
                        .border(2.dp, GlassBorder, CircleShape)
                )
            }

            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 }) togetherWith
                        fadeOut(tween(200))
                },
                label = "songInfo"
            ) { s ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        s.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        s.artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            SeekSection(vm)
            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (vm.shuffle) Lime else Color.White.copy(alpha = 0.5f)
                    )
                }
                GlassCircle(onClick = { vm.previous() }, size = 56.dp) {
                    Icon(Icons.Default.SkipPrevious, "Previous song", tint = Color.White)
                }
                Box(
                    Modifier
                        .size(80.dp)
                        .bouncyClick { vm.toggle() }
                        .clip(CircleShape)
                        .background(Lime),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = vm.isPlaying, label = "playPause") { playing ->
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = OnLime,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                GlassCircle(onClick = { vm.next() }, size = 56.dp) {
                    Icon(Icons.Default.SkipNext, "Next song", tint = Color.White)
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.5f) else Lime
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Glass)
                    .border(1.dp, GlassBorder, CircleShape)
                    .clickable { showQueue = true }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Queue", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = Surface1
        ) {
            Text(
                "Queue",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = vm.queueIndex.coerceAtLeast(0)
            )
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), state = listState) {
                itemsIndexed(vm.queue) { i, s ->
                    val isNow = s.id == song.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.jumpTo(i) }
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
                                color = if (isNow) Lime else TextHigh
                            )
                            Text(
                                s.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMid
                            )
                        }
                        if (isNow) EqBars(vm.isPlaying, Lime)
                    }
                }
            }
        }
    }
}

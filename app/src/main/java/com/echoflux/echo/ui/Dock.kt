package com.echoflux.echo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.echoflux.echo.playback.PlayerViewModel
import com.echoflux.echo.ui.theme.GlassBorder
import com.echoflux.echo.ui.theme.Lime
import com.echoflux.echo.ui.theme.OnLime
import com.echoflux.echo.ui.theme.Surface1
import com.echoflux.echo.ui.theme.TextMid

enum class Dest(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Search("Search", Icons.Default.Search),
    Browse("Browse", Icons.Default.Album),
    Playlists("Playlists", Icons.AutoMirrored.Filled.QueueMusic)
}

@Composable
fun BottomDock(
    vm: PlayerViewModel,
    dest: Dest,
    onDest: (Dest) -> Unit,
    onOpenPlayer: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = vm.current != null,
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(350)),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250))
        ) {
            MiniCard(vm, onOpenPlayer)
        }
        NavPill(dest, onDest)
    }
}

@Composable
private fun MiniCard(vm: PlayerViewModel, onOpen: () -> Unit) {
    val song = vm.current ?: return

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Surface1.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).clickable { onOpen() }
    ) {
        Box {
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtImage(song.albumArtUri, Modifier.size(46.dp), CircleShape)
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
                        .clip(CircleShape)
                        .background(Lime)
                        .clickable { vm.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (vm.isPlaying) "Pause" else "Play",
                        tint = OnLime
                    )
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next song")
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .drawBehind {
                        val frac = if (vm.durationMs > 0)
                            (vm.positionMs.toFloat() / vm.durationMs).coerceIn(0f, 1f) else 0f
                        drawRect(Lime, size = Size(size.width * frac, size.height))
                    }
            )
        }
    }
}

@Composable
private fun NavPill(dest: Dest, onDest: (Dest) -> Unit) {
    Surface(
        shape = CircleShape,
        color = Surface1.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Dest.entries.forEach { d ->
                val selected = d == dest
                val bg by animateColorAsState(
                    if (selected) Lime else Color.Transparent,
                    tween(250),
                    label = "navBg"
                )
                val fg by animateColorAsState(
                    if (selected) OnLime else TextMid,
                    tween(250),
                    label = "navFg"
                )
                Box(
                    Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onDest(d) }
                        .animateContentSize(tween(250))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(d.icon, contentDescription = d.label, tint = fg)
                        if (selected) {
                            Text(
                                d.label,
                                color = fg,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

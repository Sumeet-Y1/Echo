package com.echoflux.echo.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.echoflux.echo.data.Song
import com.echoflux.echo.playback.PlayerViewModel
import com.echoflux.echo.ui.theme.Glass
import com.echoflux.echo.ui.theme.GlassBorder
import com.echoflux.echo.ui.theme.Lime
import com.echoflux.echo.ui.theme.OnLime
import com.echoflux.echo.ui.theme.Surface2
import com.echoflux.echo.ui.theme.TextHigh
import com.echoflux.echo.ui.theme.TextMid

// Change this to whatever name you want in the greeting
const val USER_NAME = "echoflux"

@Composable
fun Modifier.blockTouches(): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {}

@Composable
fun ArtImage(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier
            .clip(shape)
            .background(Surface2),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = TextMid.copy(alpha = 0.5f)
        )
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun EqBars(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    val a by t.animateFloat(
        0.25f, 1f,
        infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    val b by t.animateFloat(
        0.25f, 1f,
        infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse),
        label = "b"
    )
    val c by t.animateFloat(
        0.25f, 1f,
        infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "c"
    )
    Row(
        modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(a, b, c).forEach { h ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(if (playing) h else 0.3f)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun GlassCircle(onClick: () -> Unit, size: Dp = 44.dp, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(size)
            .bouncyClick(onClick)
            .clip(CircleShape)
            .background(Glass)
            .border(1.dp, GlassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun EchoChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Lime else Surface2, tween(250), label = "chipBg")
    val fg by animateColorAsState(if (selected) OnLime else TextHigh, tween(250), label = "chipFg")
    Box(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LimeButton(text: String, icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (enabled) Lime else Surface2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OnLime, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = OnLime, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
    )
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextMid)
    }
}

@Composable
fun IconTile(icon: ImageVector) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Lime)
    }
}

@Composable
fun ListRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
        }
        trailing()
    }
}

@Composable
fun SongRow(
    vm: PlayerViewModel,
    song: Song,
    onPlay: () -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    val isCurrent = vm.current?.id == song.id
    val fav = song.id in vm.favorites
    var menu by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtImage(song.albumArtUri, Modifier.size(56.dp), RoundedCornerShape(16.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) Lime else TextHigh
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) {
                    EqBars(vm.isPlaying, Lime)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
            }
        }
        if (fav) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Favorite",
                tint = Lime,
                modifier = Modifier.size(16.dp)
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = TextMid)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (fav) "Remove from favorites" else "Add to favorites") },
                    onClick = { vm.toggleFavorite(song); menu = false }
                )
                DropdownMenuItem(
                    text = { Text("Play next") },
                    onClick = { vm.playNext(song); menu = false }
                )
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    onClick = { onAddToPlaylist(song); menu = false }
                )
            }
        }
    }
}

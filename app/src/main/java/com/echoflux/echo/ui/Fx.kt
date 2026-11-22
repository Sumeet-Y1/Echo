package com.echoflux.echo.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.echoflux.echo.playback.PlayerViewModel
import com.echoflux.echo.ui.theme.Lime
import com.echoflux.echo.ui.theme.OnLime

/**
 * Soft backdrop made from a tiny 64px copy of the art, stretched up.
 * Looks blurred, costs almost nothing, and works on every Android version.
 */
@Composable
fun ArtBackdrop(model: Any?, modifier: Modifier = Modifier, alpha: Float = 0.5f) {
    val context = LocalContext.current
    val request = remember(model) {
        ImageRequest.Builder(context).data(model).size(64).build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = alpha,
        modifier = modifier
    )
}

/** Springy press animation: the element squishes in and bounces back. */
@Composable
fun Modifier.bouncyClick(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/**
 * Seek bar drawn directly on the canvas. The progress is read while drawing,
 * so the screen never has to recompose as the song plays.
 */
@Composable
fun SeekBar(progress: () -> Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val dragging by remember { derivedStateOf { dragFraction != null } }
    val thumbScale by animateFloatAsState(
        if (dragging) 1.7f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "thumb"
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { o -> onSeek((o.x / size.width).coerceIn(0f, 1f)) })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragFraction = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragFraction?.let { onSeek(it) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
            .drawBehind {
                val frac = dragFraction ?: progress().coerceIn(0f, 1f)
                val trackH = 6.dp.toPx()
                val cy = size.height / 2f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(0f, cy - trackH / 2f),
                    size = Size(size.width, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )
                drawRoundRect(
                    color = Lime,
                    topLeft = Offset(0f, cy - trackH / 2f),
                    size = Size(size.width * frac, trackH),
                    cornerRadius = CornerRadius(trackH / 2f)
                )
                drawCircle(
                    color = Lime,
                    radius = 7.dp.toPx() * thumbScale,
                    center = Offset(size.width * frac, cy)
                )
            }
    )
}

private fun fmt(sec: Long): String = "%d:%02d".format(sec / 60, sec % 60)

/** Seek bar plus the two time labels. Only the labels recompose, once per second. */
@Composable
fun SeekSection(vm: PlayerViewModel) {
    val posSec by remember { derivedStateOf { vm.positionMs / 1000 } }
    val durSec by remember { derivedStateOf { vm.durationMs / 1000 } }

    SeekBar(
        progress = { if (vm.durationMs > 0) vm.positionMs.toFloat() / vm.durationMs else 0f },
        onSeek = { f -> vm.seekTo((f * vm.durationMs).toLong()) }
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(fmt(posSec), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(fmt(durSec), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

/** Profile picture, or the first letter of the name when there's no photo. */
@Composable
fun Avatar(vm: PlayerViewModel, size: Dp, modifier: Modifier = Modifier) {
    val file = vm.avatarFile
    val initial = vm.userName.trim().firstOrNull()?.uppercase() ?: "E"
    Box(
        modifier.size(size).clip(CircleShape).background(Lime),
        contentAlignment = Alignment.Center
    ) {
        if (file != null) {
            AsyncImage(
                model = file,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                initial,
                color = OnLime,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

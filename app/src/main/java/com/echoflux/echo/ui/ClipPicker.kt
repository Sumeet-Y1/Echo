package com.echoflux.echo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.echoflux.echo.data.Song
import com.echoflux.echo.playback.PlayerViewModel
import com.echoflux.echo.ui.theme.Glass
import com.echoflux.echo.ui.theme.GlassBorder
import com.echoflux.echo.ui.theme.Lime
import com.echoflux.echo.ui.theme.OnLime
import com.echoflux.echo.ui.theme.TextHigh
import com.echoflux.echo.ui.theme.TextMid
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private fun clipClock(ms: Long): String {
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/**
 * Instagram-style clip picker.
 * The song is drawn as a bar timeline. The lime window is the clip: drag it or tap the bar
 * to move it, use the chips to change its length. The bars inside the window dance while
 * the preview plays. The parent owns startMs and lenMs and gets every change in onChange.
 */
@Composable
fun ClipPicker(
    vm: PlayerViewModel,
    song: Song,
    startMs: Long,
    lenMs: Long,
    onChange: (Long, Long) -> Unit
) {
    val dur = song.durationMs.coerceAtLeast(1000L)
    val len = lenMs.coerceIn(1000L, dur)
    val start = startMs.coerceIn(0L, (dur - len).coerceAtLeast(0L))

    // Preview: waits a moment after the last change, then plays the clip in a loop.
    LaunchedEffect(song.id, start, len) {
        delay(300)
        vm.playClip(song, start, len)
    }
    // Only when the picker leaves the screen: put the listener's music back.
    DisposableEffect(Unit) { onDispose { vm.stopClip() } }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pick your clip", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${clipClock(start)} - ${clipClock(start + len)}  |  ${len / 1000}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = Lime
                )
            }
            GlassCircle(onClick = { vm.toggle() }, size = 44.dp) {
                Icon(
                    if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (vm.isPlaying) "Pause preview" else "Play preview",
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        ClipTimeline(vm, song, start, len) { newStart -> onChange(newStart, len) }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0:00", style = MaterialTheme.typography.bodySmall, color = TextMid)
            Text(clipClock(dur), style = MaterialTheme.typography.bodySmall, color = TextMid)
        }
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 20, 30, 60).forEach { sec ->
                val newLen = (sec * 1000L).coerceAtMost(dur)
                LenChip(sec, len == newLen) {
                    val maxStart = (dur - newLen).coerceAtLeast(0L)
                    onChange(start.coerceAtMost(maxStart), newLen)
                }
            }
        }
        Text(
            "Drag or tap the bar to move the clip",
            style = MaterialTheme.typography.bodySmall,
            color = TextMid,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun LenChip(sec: Int, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Lime else Glass, tween(220), label = "chipBg")
    val fg by animateColorAsState(if (on) OnLime else TextHigh, tween(220), label = "chipFg")
    val bd by animateColorAsState(if (on) Lime else GlassBorder, tween(220), label = "chipBd")
    Row(
        Modifier
            .bouncyClick { onClick() }
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, bd, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("${sec}s", style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun ClipTimeline(
    vm: PlayerViewModel,
    song: Song,
    start: Long,
    len: Long,
    onStart: (Long) -> Unit
) {
    val dur = song.durationMs.coerceAtLeast(1000L)
    val bars = remember(song.id) {
        val r = java.util.Random(song.id)
        FloatArray(56) { 0.22f + r.nextFloat() * 0.78f }
    }
    var widthPx by remember { mutableStateOf(1f) }
    val curStart by rememberUpdatedState(start)
    val onStartNow by rememberUpdatedState(onStart)
    val maxStart = (dur - len).coerceAtLeast(0L)

    // Moves the bars inside the window. Only read while playing, so a paused picker is idle.
    val transition = rememberInfiniteTransition(label = "clipBars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    // The window grows or shrinks smoothly when you change the length chip.
    val lenFrac by animateFloatAsState(len.toFloat() / dur, tween(320), label = "lenFrac")

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Glass)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(dur, len) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = (dragAmount / widthPx * dur).toLong()
                    onStartNow((curStart + delta).coerceIn(0L, maxStart))
                }
            }
            .pointerInput(dur, len) {
                detectTapGestures { pos ->
                    val center = (pos.x / widthPx * dur).toLong()
                    onStartNow((center - len / 2).coerceIn(0L, maxStart))
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val n = bars.size
        val slot = w / n
        val s = start.toFloat() / dur
        val e = (s + lenFrac).coerceAtMost(1f)
        val live = vm.isPlaying
        val ph = if (live) phase else 0f
        val twoPi = 2f * PI.toFloat()

        for (i in 0 until n) {
            val cx = (i + 0.5f) * slot
            val f = cx / w
            val inside = f >= s && f <= e
            val wave = if (inside && live) 0.84f + 0.16f * sin(ph * twoPi + i * 0.55f) else 1f
            val bh = bars[i] * h * 0.78f * wave
            drawLine(
                color = if (inside) Lime else Color.White.copy(alpha = 0.28f),
                start = Offset(cx, (h - bh) / 2f),
                end = Offset(cx, (h + bh) / 2f),
                strokeWidth = slot * 0.55f,
                cap = StrokeCap.Round
            )
        }

        val glow = if (live) 0.6f + 0.25f * sin(ph * twoPi) else 0.7f
        drawRoundRect(
            color = Lime.copy(alpha = glow),
            topLeft = Offset(s * w, 2f),
            size = Size((e - s) * w, h - 4f),
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 3f)
        )

        // Playhead: read here so only the drawing updates, not the whole screen.
        val p = vm.positionMs
        if (p in start..(start + len)) {
            val x = p.toFloat() / dur * w
            drawLine(
                color = Color.White,
                start = Offset(x, 6f),
                end = Offset(x, h - 6f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

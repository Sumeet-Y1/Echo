package com.echoflux.echo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import com.echoflux.echo.playback.PlayerViewModel
import com.echoflux.echo.ui.BottomDock
import com.echoflux.echo.ui.BrowseScreen
import com.echoflux.echo.ui.Dest
import com.echoflux.echo.ui.Detail
import com.echoflux.echo.ui.DetailScreen
import com.echoflux.echo.ui.HomeScreen
import com.echoflux.echo.ui.LimeButton
import com.echoflux.echo.ui.NowPlayingScreen
import com.echoflux.echo.ui.PlaylistDialogs
import com.echoflux.echo.ui.ProfileScreen
import com.echoflux.echo.ui.ProfileSheet
import com.echoflux.echo.ui.PlaylistUi
import com.echoflux.echo.ui.PlaylistsScreen
import com.echoflux.echo.ui.SearchScreen
import com.echoflux.echo.ui.recentAlbumCount
import com.echoflux.echo.ui.theme.EchoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            EchoTheme {
                val vm: PlayerViewModel = viewModel()
                EchoApp(vm)
            }
        }
    }
}

@Composable
fun EchoApp(vm: PlayerViewModel) {
    val context = LocalContext.current
    val audioPerm = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPerm) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> granted = result[audioPerm] == true }

    fun askPermission() {
        val perms = mutableListOf(audioPerm)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        launcher.launch(perms.toTypedArray())
    }

    LaunchedEffect(Unit) { if (!granted) askPermission() }
    LaunchedEffect(granted) { if (granted) vm.load() }

    var dest by remember { mutableStateOf(Dest.Home) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var lastDetail by remember { mutableStateOf<Detail?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    val playlistUi = remember { PlaylistUi() }
    var showProfile by remember { mutableStateOf(false) }
    var showProfilePage by remember { mutableStateOf(false) }
    LaunchedEffect(granted) { if (granted && !vm.profileSeen) showProfile = true }

    // Hoisted so the carousel and scroll position survive tab switches
    val homePager = rememberPagerState { recentAlbumCount(vm.songs) }
    val homeList = rememberLazyListState()

    LaunchedEffect(detail) { if (detail != null) lastDetail = detail }
    BackHandler(enabled = detail != null) { detail = null }
    BackHandler(enabled = showProfilePage) { showProfilePage = false }

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = (if (vm.current != null) 176.dp else 104.dp) + navInset
    val addToPlaylist: (com.echoflux.echo.data.Song) -> Unit = { playlistUi.picker = it }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!granted) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Echo needs access to the music on your phone",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(12.dp))
                    LimeButton("Allow access", Icons.Default.MusicNote, { askPermission() })
                }
            } else {
                AnimatedContent(
                    targetState = dest,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                    label = "tabs"
                ) { d ->
                    when (d) {
                        Dest.Home -> HomeScreen(
                            vm, homePager, homeList, bottomPad,
                            { detail = it }, addToPlaylist,
                            { showProfilePage = true }
                        )
                        Dest.Search -> SearchScreen(vm, bottomPad, addToPlaylist)
                        Dest.Browse -> BrowseScreen(vm, bottomPad) { detail = it }
                        Dest.Playlists -> PlaylistsScreen(
                            vm, bottomPad, { detail = it },
                            { playlistUi.newFor = null; playlistUi.newOpen = true }
                        )
                    }
                }
            }

            val shownDetail = detail ?: lastDetail
            AnimatedVisibility(
                visible = detail != null,
                enter = slideInHorizontally(tween(320)) { it / 2 } + fadeIn(tween(320)),
                exit = slideOutHorizontally(tween(260)) { it / 2 } + fadeOut(tween(260))
            ) {
                shownDetail?.let { d ->
                    DetailScreen(d, vm, bottomPad, { detail = null }, addToPlaylist)
                }
            }

            AnimatedVisibility(
                visible = showProfilePage,
                enter = slideInVertically(tween(350)) { it / 6 } + fadeIn(tween(350)),
                exit = slideOutVertically(tween(280)) { it / 6 } + fadeOut(tween(280))
            ) {
                ProfileScreen(vm, bottomPad) { showProfilePage = false }
            }

            if (granted) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    BottomDock(
                        vm = vm,
                        dest = dest,
                        onDest = { dest = it; detail = null; showProfilePage = false },
                        onOpenPlayer = { showPlayer = true }
                    )
                }
            }

            AnimatedVisibility(
                visible = showPlayer && vm.current != null,
                enter = slideInVertically(tween(380)) { it } + fadeIn(tween(380)),
                exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
            ) {
                NowPlayingScreen(vm) { showPlayer = false }
            }
        }
    }

    PlaylistDialogs(vm, playlistUi)

    if (showProfile) {
        ProfileSheet(vm, firstRun = !vm.profileSeen) {
            vm.markProfileSeen()
            showProfile = false
        }
    }
}

package com.echonote.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.echonote.app.ui.screens.NoteDetailScreen
import com.echonote.app.ui.screens.NoteListScreen
import com.echonote.app.ui.screens.RecordScreen
import com.echonote.app.ui.screens.SettingsScreen
import com.echonote.app.ui.theme.EchoNoteTheme
import com.echonote.app.ui.theme.NoteTagColors
import com.echonote.app.util.BackgroundStyle
import com.echonote.app.util.ThemeSettings
import com.echonote.app.util.scale

private const val ROUTE_LIST = "list"
private const val ROUTE_RECORD = "record"
private const val ROUTE_DETAIL = "detail/{noteId}"
private const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    private val pendingNoteId = mutableStateOf<Long?>(null)
    private val pendingStartRecording = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            EchoNoteTheme {
                val app = LocalContext.current.applicationContext as EchoNoteApp
                val settings by app.themePreferences.settings.collectAsState()
                val brush = appBackgroundBrush(settings)
                val density = LocalDensity.current
                // Surface keeps its default (opaque, theme-correct) color as the base fill.
                // The gradient/radial/mesh brush is only ever semi-transparent, so painting it
                // straight onto a Color.Transparent Surface let the native window background
                // (always light, since it's set by the Android XML theme, independent of the
                // app's runtime dark/light choice) show through the un-tinted portions - washing
                // out dark mode and leaving light-on-light or dark-text-on-dark-background spots.
                // Layering it on top of the real background in a Box keeps the tint but
                // guarantees the base underneath always matches colorScheme.background.
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, density.fontScale * settings.fontSize.scale()),
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (brush != null) {
                                Box(modifier = Modifier.fillMaxSize().background(brush))
                            }
                            EchoNoteNavHost(pendingNoteId, pendingStartRecording)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_OPEN_NOTE_ID, -1L)
        if (noteId >= 0) pendingNoteId.value = noteId
        if (intent.getBooleanExtra(EXTRA_START_RECORDING, false)) pendingStartRecording.value = true
    }

    companion object {
        const val EXTRA_OPEN_NOTE_ID = "open_note_id"
        const val EXTRA_START_RECORDING = "start_recording"
    }
}

@Composable
private fun appBackgroundBrush(settings: ThemeSettings): Brush? {
    val base = NoteTagColors.getOrElse(settings.baseColorIndex) { NoteTagColors.first() }
    val accent = NoteTagColors.getOrElse(settings.accentColorIndex) { NoteTagColors.first() }
    val background = MaterialTheme.colorScheme.background
    return when (settings.backgroundStyle) {
        BackgroundStyle.SOLID -> null
        BackgroundStyle.GRADIENT -> Brush.verticalGradient(
            listOf(base.copy(alpha = 0.16f), background),
        )
        BackgroundStyle.RADIAL -> Brush.radialGradient(
            colors = listOf(base.copy(alpha = 0.35f), background),
            radius = 1400f,
        )
        BackgroundStyle.MESH -> Brush.linearGradient(
            colors = listOf(base.copy(alpha = 0.22f), accent.copy(alpha = 0.12f), background),
        )
    }
}

@Composable
private fun EchoNoteNavHost(
    pendingNoteId: MutableState<Long?>,
    pendingStartRecording: MutableState<Boolean>,
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingNoteId.value) {
        pendingNoteId.value?.let { id ->
            navController.navigate("detail/$id")
            pendingNoteId.value = null
        }
    }
    LaunchedEffect(pendingStartRecording.value) {
        if (pendingStartRecording.value) {
            navController.navigate(ROUTE_RECORD)
            pendingStartRecording.value = false
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST,
        enterTransition = { fadeSlideIn() },
        exitTransition = { fadeSlideOut() },
        popEnterTransition = { fadeSlideIn() },
        popExitTransition = { fadeSlideOut() },
    ) {
        composable(ROUTE_LIST) {
            NoteListScreen(
                onNoteClick = { id -> navController.navigate("detail/$id") },
                onRecordClick = { navController.navigate(ROUTE_RECORD) },
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(ROUTE_RECORD) {
            RecordScreen(
                onFinished = { noteId ->
                    navController.popBackStack()
                    if (noteId != null) navController.navigate("detail/$noteId")
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            NoteDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun AnimatedContentTransitionScope<*>.fadeSlideIn() =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Up,
        animationSpec = tween(280),
    )

private fun AnimatedContentTransitionScope<*>.fadeSlideOut() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Down,
        animationSpec = tween(280),
    )

package com.echonote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.echonote.app.ui.screens.NoteDetailScreen
import com.echonote.app.ui.screens.NoteListScreen
import com.echonote.app.ui.screens.RecordScreen
import com.echonote.app.ui.theme.EchoNoteTheme

private const val ROUTE_LIST = "list"
private const val ROUTE_RECORD = "record"
private const val ROUTE_DETAIL = "detail/{noteId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EchoNoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EchoNoteNavHost()
                }
            }
        }
    }
}

@Composable
private fun EchoNoteNavHost() {
    val navController = rememberNavController()
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
            )
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

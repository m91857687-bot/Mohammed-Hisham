package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WebCodeStudioApp()
                }
            }
        }
    }
}

@Serializable
object HomeRoute

@Serializable
data class EditorRoute(val projectId: Int)

@Serializable
object AboutRoute

@Composable
fun WebCodeStudioApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ProjectRepository(database.projectDao()) }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                repository = repository,
                onNavigateToEditor = { projectId ->
                    navController.navigate(EditorRoute(projectId))
                },
                onNavigateToAbout = {
                    navController.navigate(AboutRoute)
                }
            )
        }
        composable<EditorRoute> { backStackEntry ->
            val route: EditorRoute = backStackEntry.toRoute()
            EditorScreen(
                projectId = route.projectId,
                repository = repository,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable<AboutRoute> {
            AboutScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}


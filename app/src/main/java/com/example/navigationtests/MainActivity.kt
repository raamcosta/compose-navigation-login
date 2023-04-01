package com.example.navigationtests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.navigationtests.ui.theme.NavigationTestsTheme

val LocalDiContainer = staticCompositionLocalOf<DiContainer> { error("No DI Container provided") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NavigationTestsTheme {

                CompositionLocalProvider(
                    LocalDiContainer provides (applicationContext as App).diContainer
                ) {
                    MainComposable()
                }
            }
        }
    }
}

@Composable
fun MainComposable() {
    val navController = rememberNavController()

    val current = navController.currentBackStackEntryAsState().value
    println("current = $current")
    navController.backQueue.print()

    /*
     * I don't love this... but I love the usages, of the method.
     * One alternative would be to call Authenticated directly but we'd
     * have to pass either NavController or a lambda to navigate to login
     * every time :/
     *
     * There's probably a better way..
     */
    fun NavGraphBuilder.authenticated(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        deepLinks: List<NavDeepLink> = emptyList(),
        screenContent: @Composable (NavBackStackEntry) -> Unit
    ) {
        composable(
            route = route,
            arguments = arguments,
            deepLinks = deepLinks,
            content = { backStackEntry ->
                backStackEntry.Authenticated(
                    navigateToLogin = { navController.navigate("login") },
                    screenComposable = screenContent
                )
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        authenticated("home") {
            HomeScreen(
                navigateToSettings = { navController.navigate("settings") }
            )
        }

        authenticated("settings") {
            SettingsScreen()
        }

        composable("login") {
            LoginScreen(
                popBackStack = navController::popBackStack
            )
        }
    }
}

@Composable
private fun NavBackStackEntry.Authenticated(
    navigateToLogin: () -> Unit,
    screenComposable: @Composable (NavBackStackEntry) -> Unit
) {
    val diContainer = LocalDiContainer.current
    val userRepo = remember { diContainer.userRepository }

    LaunchedEffect(this) {
        userRepo.loggedInUser.collect { loggedInUser ->
            if (loggedInUser == null) {
                navigateToLogin()
            }
        }
    }

    screenComposable(this)
}

private fun Collection<NavBackStackEntry>.print(prefix: String = "stack") {
    val stack = map { it.destination.route }.toTypedArray().contentToString()
    println("$prefix = $stack")
}

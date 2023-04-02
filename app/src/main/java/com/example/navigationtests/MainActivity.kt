package com.example.navigationtests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.navigationtests.ui.theme.NavigationTestsTheme

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
    fun NavGraphBuilder.authenticatedComposable(
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
        authenticatedComposable("home") {
            val diContainer: DiContainer = LocalDiContainer.current
            val viewModel: HomeViewModel = viewModel { HomeViewModel(diContainer) }
            val state = viewModel.uiState.collectAsState().value

            HomeScreen(
                navigateToSettings = { navController.navigate("settings") },
                onLogoutClick = viewModel::onLogoutClick,
                state = state
            )
        }

        authenticatedComposable("settings") {
            val diContainer: DiContainer = LocalDiContainer.current
            val viewModel: HomeViewModel = viewModel { HomeViewModel(diContainer) }
            val state = viewModel.uiState.collectAsState().value
            SettingsScreen(
                state = state,
                onLogoutClick = viewModel::onLogoutClick
            )
        }

        composable("login") {
            val diContainer: DiContainer = LocalDiContainer.current
            val viewModel: LoginViewModel = viewModel { LoginViewModel(diContainer) }
            val currentState = viewModel.uiState.collectAsState().value

            BackHandler {
                // No op: user can't leave this screen without logging in
                // We could maybe let him put app on background or similar
            }

            LoginScreen(
                currentState = currentState,
                onLoginClick = viewModel::onLoginClick,
                onUsernameInputChange = viewModel::onUsernameInputChange,
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

    when (val userState = userRepo.loggedInUser.collectAsState().value) {
        is UserRepository.UserState.Loading -> Box(Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Loading auth state..."
            )
        }

        is UserRepository.UserState.LoggedOut -> {
            LaunchedEffect(userState) {
                navigateToLogin()
            }
        }

        is UserRepository.UserState.LoggedIn -> screenComposable(this)
    }
}

private fun Collection<NavBackStackEntry>.print(prefix: String = "stack") {
    val stack = map { it.destination.route }.toTypedArray().contentToString()
    println("$prefix = $stack")
}

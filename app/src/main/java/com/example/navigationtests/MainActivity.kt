package com.example.navigationtests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.navigationtests.ui.theme.NavigationTestsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val diContainer = (application as App).diContainer
        splashScreen.showWhileCheckingLoginState(diContainer.userRepository)

        setContent {
            NavigationTestsTheme {
                CompositionLocalProvider(LocalDiContainer provides diContainer) {
                    MainComposable()
                }
            }
        }
    }

    private fun SplashScreen.showWhileCheckingLoginState(
        userRepository: UserRepository,
    ) {
        var userState = userRepository.loggedInUser.value

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val determinedUserState = userRepository
                    .loggedInUser
                    .first { it !is UserRepository.UserState.Loading }
                userState = determinedUserState
            }
        }

        // Keep the splash screen on-screen until the auth status is determined.
        setKeepOnScreenCondition {
            userState == UserRepository.UserState.Loading
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
                    navController = navController,
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
            val state = viewModel.uiState.collectAsStateWithLifecycle().value

            HomeScreen(
                navigateToSettings = { navController.navigate("settings") },
                onLogoutClick = viewModel::onLogoutClick,
                state = state
            )
        }

        authenticatedComposable("settings") {
            val diContainer: DiContainer = LocalDiContainer.current
            val viewModel: HomeViewModel = viewModel { HomeViewModel(diContainer) }
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            SettingsScreen(
                state = state,
                onLogoutClick = viewModel::onLogoutClick
            )
        }

        composable("login") {
            val diContainer: DiContainer = LocalDiContainer.current
            val viewModel: LoginViewModel = viewModel { LoginViewModel(diContainer) }
            val currentState = viewModel.uiState.collectAsStateWithLifecycle().value

            LoginScreen(
                onLoginClick = viewModel::onLoginClick,
                onUsernameInputChange = viewModel::onUsernameInputChange,
                onLoginComplete = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.id)
                        restoreState = true
                    }
                },
                currentState = currentState
            )
        }
    }
}

@Composable
private fun NavBackStackEntry.Authenticated(
    navController: NavController,
    screenComposable: @Composable (NavBackStackEntry) -> Unit
) {
    val diContainer = LocalDiContainer.current
    val userRepo = remember { diContainer.userRepository }
    val userState by userRepo.loggedInUser.collectAsStateWithLifecycle()

    when (userState) {
        is UserRepository.UserState.Loading -> Unit
        is UserRepository.UserState.LoggedOut -> {
            LaunchedEffect(userState) {
                navController.navigate("login") {
                    popUpTo(navController.graph.id) {
                        saveState = true
                    }

                    launchSingleTop = true
                }
            }
        }
        is UserRepository.UserState.LoggedIn -> screenComposable(this)
    }
}

private fun Collection<NavBackStackEntry>.print(prefix: String = "stack") {
    val stack = map { it.destination.route }.toTypedArray().contentToString()
    println("$prefix = $stack")
}

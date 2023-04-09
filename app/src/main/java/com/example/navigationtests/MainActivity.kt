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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.navigationtests.destinations.LoginScreenDestination
import com.example.navigationtests.ui.theme.NavigationTestsTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.scope.DestinationScope
import com.ramcosta.composedestinations.wrapper.DestinationWrapper
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

    DestinationsNavHost(
        navController = navController,
        navGraph = NavGraphs.root
    )
}


object AuthenticatedScreenWrapper : DestinationWrapper {

    @Composable
    override fun <T> DestinationScope<T>.Wrap(screenContent: @Composable () -> Unit) {
        val diContainer = LocalDiContainer.current
        val userRepo = remember(diContainer) { diContainer.userRepository }
        val userState by userRepo.loggedInUser.collectAsStateWithLifecycle()

        when (userState) {
            is UserRepository.UserState.Loading -> Unit
            is UserRepository.UserState.LoggedOut -> {
                LaunchedEffect(userState) {
                    navController.navigate(LoginScreenDestination) {
                        popUpTo(NavGraphs.root) {
                            saveState = true
                        }

                        launchSingleTop = true
                    }
                }
            }
            is UserRepository.UserState.LoggedIn -> screenContent()
        }
    }
}

private fun Collection<NavBackStackEntry>.print(prefix: String = "stack") {
    val stack = map { it.destination.route }.toTypedArray().contentToString()
    println("$prefix = $stack")
}

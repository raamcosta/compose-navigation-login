package com.example.navigationtests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.navigationtests.destinations.LoginScreenDestination
import com.example.navigationtests.ui.theme.NavigationTestsTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.scope.DestinationScope
import com.ramcosta.composedestinations.wrapper.DestinationWrapper

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

    DestinationsNavHost(
        navController = navController,
        navGraph = NavGraphs.root
    )
}


object AuthenticatedScreenWrapper: DestinationWrapper {

    @Composable
    override fun <T> DestinationScope<T>.Wrap(screenContent: @Composable () -> Unit) {
        val diContainer = LocalDiContainer.current
        val userRepo = remember(diContainer) { diContainer.userRepository }
        val userState by userRepo.loggedInUser.collectAsStateWithLifecycle()
        when (userState) {
            is UserRepository.UserState.Loading -> Box(Modifier.fillMaxSize()) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "Loading auth state..."
                )
            }

            is UserRepository.UserState.LoggedOut -> {
                LaunchedEffect(userState) {
                    destinationsNavigator.navigate(LoginScreenDestination)
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

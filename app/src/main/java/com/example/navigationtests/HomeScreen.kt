package com.example.navigationtests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.navigationtests.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RootNavGraph(start = true)
@Destination(
    wrappers = [AuthenticatedScreenWrapper::class]
)
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    diContainer: DiContainer = LocalDiContainer.current,
    viewModel: HomeViewModel = viewModel { HomeViewModel(diContainer) }
) {

    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    HomeScreenContent(
        navigateToSettings = { navigator.navigate(SettingsScreenDestination) },
        onLogoutClick = viewModel::onLogoutClick,
        state = state
    )
}

@Composable
fun HomeScreenContent(
    navigateToSettings: () -> Unit,
    onLogoutClick: () -> Unit,
    state: HomeViewModel.UiState?,
) {
    Box(Modifier.fillMaxSize()) {

        if (state == null) {
            Text(modifier = Modifier.align(Alignment.Center), text = "Loading...")
        } else {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Hello ${state.firstName} ${state.lastName} (${state.username})"
            )

            Button(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = onLogoutClick
            ) {
                Text("Logout")
            }

            Button(
                modifier = Modifier.align(Alignment.TopStart),
                onClick = navigateToSettings
            ) {
                Text("Settings")
            }
        }
    }
}

class HomeViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState?>(null)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.loggedInUser.collect { userState ->
                val newState = when (userState) {
                    is UserRepository.UserState.LoggedIn -> UiState(
                        userState.user.username,
                        userState.user.firstName,
                        userState.user.lastName
                    )
                    is UserRepository.UserState.Loading,
                    is UserRepository.UserState.LoggedOut -> null
                }
                _uiState.update { newState }
            }
        }
    }

    fun onLogoutClick() {
        userRepository.logout()
    }

    class UiState(
        val username: String,
        val firstName: String,
        val lastName: String
    )

    companion object {
        operator fun invoke(diContainer: DiContainer): HomeViewModel {
            return HomeViewModel(
                diContainer.userRepository
            )
        }
    }
}
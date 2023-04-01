package com.example.navigationtests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(
    navigateToSettings: () -> Unit,
    diContainer: DiContainer = LocalDiContainer.current,
    viewModel: HomeViewModel = viewModel { HomeViewModel(diContainer) }
) {
    val state = viewModel.uiState.collectAsState().value

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
                onClick = viewModel::onLogoutClick
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
            userRepository.loggedInUser.collect { user ->
                if (user == null) {
                    _uiState.update { null }
                } else {
                    _uiState.update { UiState(user.username, user.firstName, user.lastName) }
                }
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
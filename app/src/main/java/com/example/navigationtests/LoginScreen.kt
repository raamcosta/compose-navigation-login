package com.example.navigationtests

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    popBackStack: () -> Unit,
    diContainer: DiContainer = LocalDiContainer.current,
    viewModel: LoginViewModel = viewModel { LoginViewModel(diContainer) }
) {
    val currentState = viewModel.uiState.collectAsState().value

    LaunchedEffect (currentState.done) {
        if (currentState.done) {
            popBackStack()
        }
    }

    if (currentState.done) return

    Box(Modifier.fillMaxSize()) {
        if (currentState.loading) {
            Text(modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center, text = "Logging in...")
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (currentState.error != null) {
                    Text(text = stringResource(currentState.error))
                }

                TextField(value = currentState.currentUsernameInput, onValueChange = viewModel::onUsernameInputChange)
                Button(onClick = viewModel::onLoginClick) {
                    Text("Login")
                }
            }
        }
    }
}

class LoginViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UiState(
            currentUsernameInput = "megaZord123" /*to make testing easier*/,
            error = null,
            loading = false,
            done = false
        )
    )
    val uiState = _uiState.asStateFlow()

    fun onLoginClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            userRepository.login(_uiState.value.currentUsernameInput)
                .onFailure {
                    _uiState.update { it.copy(error = R.string.unknown_username, loading = false) }
                }
                .onSuccess {
                    _uiState.update { it.copy(done = true, loading = false) }
                }
        }
    }

    fun onUsernameInputChange(newInput: String) {
        _uiState.update { it.copy(currentUsernameInput = newInput, error = null) }
    }

    data class UiState(
        val currentUsernameInput: String,
        @StringRes val error: Int?,
        val loading: Boolean,
        val done: Boolean
    )

    companion object {
        operator fun invoke(diContainer: DiContainer): LoginViewModel {
            return LoginViewModel(
                diContainer.userRepository
            )
        }
    }
}
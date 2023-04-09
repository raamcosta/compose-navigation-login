package com.example.navigationtests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination

@Destination(
    wrappers = [AuthenticatedScreenWrapper::class]
)
@Composable
fun SettingsScreen(
    diContainer: DiContainer = LocalDiContainer.current,
    viewModel: HomeViewModel = viewModel { HomeViewModel(diContainer) }
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    Box(Modifier.fillMaxSize()) {

        if (state == null) {
            Text(modifier = Modifier.align(Alignment.Center), text = "Loading...")
        } else {

            Column(modifier = Modifier.align(Alignment.TopEnd)) {
                Text(
                    text = "${state.firstName} ${state.lastName} (${state.username})"
                )

                Button(
                    onClick = viewModel::onLogoutClick
                ) {
                    Text("Logout")
                }
            }
        }
    }
}
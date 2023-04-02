package com.example.navigationtests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(
    state: HomeViewModel.UiState?,
    onLogoutClick: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {

        if (state == null) {
            Text(modifier = Modifier.align(Alignment.Center), text = "Loading...")
        } else {

            Column(modifier = Modifier.align(Alignment.TopEnd)) {
                Text(
                    text = "${state.firstName} ${state.lastName} (${state.username})"
                )

                Button(
                    onClick = onLogoutClick
                ) {
                    Text("Logout")
                }
            }
        }
    }
}
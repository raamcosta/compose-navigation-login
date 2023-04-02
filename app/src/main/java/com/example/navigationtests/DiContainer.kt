package com.example.navigationtests

import android.app.Application
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDiContainer = staticCompositionLocalOf<DiContainer> { error("No DI Container provided") }

class DiContainer(
    app: Application
) {

    val userRepository = UserRepository(
        app.getSharedPreferences("shared_pref", Context.MODE_PRIVATE)
    )
}
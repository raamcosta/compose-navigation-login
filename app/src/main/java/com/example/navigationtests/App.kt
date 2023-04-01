package com.example.navigationtests

import android.app.Application
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class App : Application() {

    val diContainer: DiContainer = DiContainer(this)
}

class DiContainer(
    app: Application
) {

    val userRepository = UserRepository()
}


class UserRepository {

    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    suspend fun login(username: String): Result<User> {
        // simulating something here... nothing really changes if using password
        delay(1000)
        val user = knownUsers.find { it.username == username }
            ?: return Result.failure(Exception("No known user for this username"))

        _loggedInUser.update { user }
        return Result.success(user)
    }

    fun logout() {
        _loggedInUser.update { null }
    }

    companion object {
        private val knownUsers = listOf(
            User(
                username = "megaZord123",
                firstName = "John",
                lastName = "Doe"
            ),
            User(
                username = "originalUsername",
                firstName = "Mlon",
                lastName = "Eusk"
            )
        )
    }
}

data class User(
    val username: String,
    val firstName: String,
    val lastName: String
)
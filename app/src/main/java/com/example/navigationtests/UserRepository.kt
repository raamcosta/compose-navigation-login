package com.example.navigationtests

import android.content.SharedPreferences
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This is NOT how would do it in a real app! Just in case that wasn't clear :D
 *
 * The public API of this Repository would be the same or similar, but not how we're implementing
 * it. For this sample example app, what matters is what happens around this public API, so
 * the internals don't really matter much.
 */
class UserRepository(
    private val sharedPreferences: SharedPreferences,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _loggedInUser = MutableStateFlow<UserState>(UserState.Loading)
    val loggedInUser: StateFlow<UserState> = _loggedInUser.asStateFlow()

    init {
        coroutineScope.launch {
            // Splash screen will stay on while the auth status is determined
            delay(2.seconds) // simulating something that takes a bit longer
            val loggedInUser = sharedPreferences.getString(LOGGED_IN_USERNAME_KEY, null)
                ?.let { username ->
                    knownUsers.find { it.username == username }
                }

            if (loggedInUser == null) {
                _loggedInUser.update { UserState.LoggedOut }
            } else {
                _loggedInUser.update { UserState.LoggedIn(loggedInUser) }
            }
        }
    }

    suspend fun login(username: String): Result<User> {
        // simulating something here... nothing really changes if using password
        delay(1000)
        val user = knownUsers.find { it.username == username }
            ?: return Result.failure(Exception("No known user for this username"))

        persistLoggedInState(username)
        _loggedInUser.update { UserState.LoggedIn(user) }
        return Result.success(user)
    }

    fun logout() {
        persistLoggedOutState()
        _loggedInUser.update { UserState.LoggedOut }
    }

    private fun persistLoggedOutState() {
        sharedPreferences.edit().apply {
            remove(LOGGED_IN_USERNAME_KEY)
            apply()
        }
    }

    private fun persistLoggedInState(username: String) {
        sharedPreferences.edit().apply {
            putString(LOGGED_IN_USERNAME_KEY, username)
            apply()
        }
    }

    sealed interface UserState {

        data class LoggedIn(val user: User) : UserState

        object Loading : UserState

        object LoggedOut : UserState
    }

    companion object {
        private const val LOGGED_IN_USERNAME_KEY = "LOGGED_IN_USERNAME_KEY"

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
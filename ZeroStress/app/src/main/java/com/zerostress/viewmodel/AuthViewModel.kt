package com.zerostress.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerostress.data.model.Player
import com.zerostress.data.repository.FirestoreRepository
import com.zerostress.data.repository.LocalDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FirestoreRepository()
    private val dataStore = LocalDataStore(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<Player?> = dataStore.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            currentUser.filterNotNull().first().let { player ->
                if (player.isAdmin || player.isConfirmed) {
                    _uiState.update { it.copy(isLoggedIn = true, user = player) }
                }
            }
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val player = repo.login(phone, password)
                if (player == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Invalid phone or password") }
                } else if (!player.isAdmin && !player.isConfirmed) {
                    _uiState.update { it.copy(isLoading = false, error = "Account pending approval. Please wait for admin confirmation.") }
                } else {
                    dataStore.saveUser(player)
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true, user = player) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
            }
        }
    }

    fun register(name: String, phone: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = repo.register("+880$phone", name, password)
                result.fold(
                    onSuccess = { player ->
                        dataStore.saveUser(player)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = player,
                                registrationSuccess = true
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            currentUser.value?.let { repo.logout(it.phone) }
            dataStore.clearUser()
            _uiState.update { AuthUiState() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: Player? = null,
    val error: String? = null,
    val registrationSuccess: Boolean = false
)

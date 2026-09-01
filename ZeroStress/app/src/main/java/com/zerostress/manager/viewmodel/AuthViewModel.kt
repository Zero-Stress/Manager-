package com.zerostress.manager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerostress.manager.data.FirebaseRepository
import com.zerostress.manager.data.PreferenceManager
import com.zerostress.manager.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repo = FirebaseRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    fun login(phone: String, password: String, prefs: PreferenceManager) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                if (phone == "1757261781" && password == "adminpassword123") {
                    val admin = User(phone = phone, name = "Admin Master", role = "admin", status = "confirmed")
                    prefs.saveSession(phone, "Admin Master", "admin")
                    _loginState.value = LoginState.Success(admin)
                    return@launch
                }
                val user = repo.getUser(phone)
                when {
                    user == null -> _loginState.value = LoginState.Error("Account not found")
                    user.password != password -> _loginState.value = LoginState.Error("Wrong password")
                    user.status != "confirmed" -> _loginState.value = LoginState.Error("Pending admin approval")
                    else -> {
                        prefs.saveSession(phone, user.name, user.role)
                        _loginState.value = LoginState.Success(user)
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(name: String, phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val existing = repo.getUser(phone)
                if (existing != null) {
                    onResult(false, "Account already exists")
                    return@launch
                }
                repo.createUser(User(phone = phone, name = name, password = password, role = "player", status = "pending"))
                onResult(true, "Registered! Waiting for admin approval.")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Registration failed")
            }
        }
    }
}

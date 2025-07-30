package com.example.panico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun login(email: String, password: String) {
        _uiState.value = UiState(loading = true)
        viewModelScope.launch {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiState.value = UiState(success = true)
                    } else {
                        _uiState.value = UiState(error = task.exception?.localizedMessage ?: "Error de autenticación")
                    }
                }
        }
    }
} 
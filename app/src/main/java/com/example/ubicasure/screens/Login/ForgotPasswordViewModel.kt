package com.example.ubicasure.screens.Login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ForgotPasswordViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow();

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun sendPasswordReset(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.value.isBlank()) {
            onError("Ingresa un correo válido.")
        } else {
            auth
                .sendPasswordResetEmail(email.value)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener {
                    onError("Error: ${it.message}")
                }
        }
    }
}

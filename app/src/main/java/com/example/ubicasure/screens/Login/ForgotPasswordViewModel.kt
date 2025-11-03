package com.example.ubicasure.screens.Login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ForgotPasswordViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth // Instancia de FirebaseAuth para manejar autenticación

    private val _email = MutableStateFlow("") // Estado mutable para almacenar el email ingresado
    val email: StateFlow<String> = _email.asStateFlow(); // Exposición como StateFlow inmutable para UI

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail // Actualiza el estado del email cuando el usuario lo modifica
    }

    fun sendPasswordReset(
        onSuccess: () -> Unit, // Callback para cuando se envía correctamente el correo
        onError: (String) -> Unit // Callback para manejar errores
    ) {
        if (email.value.isBlank()) {
            onError("Ingresa un correo válido.") // Validación básica: no permitir email vacío
        } else {
            auth
                .sendPasswordResetEmail(email.value) // Enviar correo de restablecimiento vía FirebaseAuth
                .addOnSuccessListener {
                    onSuccess() // Ejecuta callback de éxito si el correo fue enviado
                }
                .addOnFailureListener {
                    onError("Error: ${it.message}") // Ejecuta callback de error con mensaje específico
                }
        }
    }
}

package com.example.ubicasure.screens.Login

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.edit

/**
 * ViewModel que gestiona autenticación y persistencia del usuario con FirebaseAuth y SharedPreferences.
 */
class LoginViewModel : ViewModel() {
    private lateinit var prefs: SharedPreferences
    private val claveUsuario = "usuario"
    private val auth: FirebaseAuth = Firebase.auth

    // Estado que guarda el email del usuario actual (o "sin usuario" si no hay sesión)
    private val _usuario = MutableStateFlow("sin usuario")
    val usuario: StateFlow<String> = _usuario

    private val _rol = MutableStateFlow("user")

    // Contexto de la aplicación, necesario para acceder a SharedPreferences
    private val _context = MutableStateFlow<Context?>(null)
    val context: StateFlow<Context?> = _context

    /**
     * Inicializa SharedPreferences usando el contexto proporcionado,
     * y carga el usuario guardado (si existe) en _usuario.
     */
    fun setContext(cont: Context) {
        _context.value = cont
        prefs = context.value!!.getSharedPreferences("mi_app_prefs", Context.MODE_PRIVATE)
        val usuarioGuardado = prefs.getString(claveUsuario, "sin usuario") ?: "sin usuario"
        _usuario.value = usuarioGuardado
    }

    fun signInAuth(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError:   (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                // Al iniciar sesión correctamente, actualizamos el estado y guardamos en prefs
                _usuario.value = result.user?.email ?: "sin usuario"
                val prefs = context.value!!.getSharedPreferences("mi_app_prefs", Context.MODE_PRIVATE)
                prefs.edit { putString("usuario", usuario.value) }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Error al iniciar sesión")
            }
    }

    /**
     * Cierra sesión: restablece _usuario a "sin usuario" y elimina la clave del SharedPreferences.
     */
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _usuario.value = "sin usuario"
        prefs.edit().remove(claveUsuario).apply()
    }
}

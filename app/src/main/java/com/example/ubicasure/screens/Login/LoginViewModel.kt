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

class LoginViewModel : ViewModel() {
    private lateinit var prefs: SharedPreferences
    private val claveUsuario = "usuario"
    private val auth: FirebaseAuth = Firebase.auth

    private val _usuario = MutableStateFlow("sin usuario")
    val usuario: StateFlow<String> = _usuario

    private val _rol = MutableStateFlow("user")

    private val _context = MutableStateFlow<Context?>(null)
    val context: StateFlow<Context?> = _context

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

                _usuario.value = result.user?.email ?: "sin usuario"
                val prefs = context.value!!.getSharedPreferences("mi_app_prefs", Context.MODE_PRIVATE)
                prefs.edit { putString("usuario", usuario.value) }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Error al iniciar sesión")
            }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _usuario.value = "sin usuario"
        prefs.edit().remove(claveUsuario).apply()
    }
}

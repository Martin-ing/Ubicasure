package com.example.ubicasure.screens.Login

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ubicasure.auth.FirebaseTokenProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RegisterViewModel : ViewModel() {
    var paso by mutableStateOf(1)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var imagenUri by mutableStateOf<Uri?>(null)
    var telegramUsername by mutableStateOf("")

    var date by mutableStateOf("")
    var btype by mutableStateOf("")

    private val auth: FirebaseAuth = Firebase.auth

    fun signUpAuth(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // El usuario recién creado queda autenticado en Firebase. El backend
                // recibirá su UID y correo desde el ID Token, no desde el body.
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("RegisterViewModel", e.localizedMessage ?: "")
                onError(e.localizedMessage ?: "Error al registrar usuario")
            }
    }

    fun registerUserProfile(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Se conserva context en la firma para no alterar RegisterScreen.
        @Suppress("UNUSED_VARIABLE")
        val unusedContext = context

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authorization = FirebaseTokenProvider.getAuthorizationHeader()
                val client = OkHttpClient()

                // firebaseUid y email ya NO se mandan en el body. El middleware
                // authenticateUser los obtiene del Firebase ID Token.
                val jsonObject = JSONObject().apply {
                    put("fullName", name.trim())
                    put("phone", phone.trim())
                    put("birthDate", date.trim())
                    put("bloodType", btype.trim())

                    if (telegramUsername.isBlank()) {
                        put("telegramUsername", JSONObject.NULL)
                    } else {
                        put(
                            "telegramUsername",
                            telegramUsername.trim().removePrefix("@")
                        )
                    }
                }

                val requestBody = jsonObject
                    .toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("https://ubicasure-back-402661808663.us-central1.run.app/users/register")
                    .header("Authorization", authorization)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            onSuccess()
                        } else {
                            val message = when (response.code) {
                                401 -> "El token de Firebase no es válido o expiró."
                                403 -> "No tienes permiso para registrar este perfil."
                                409 -> "Este usuario ya tiene un perfil registrado."
                                else -> "Error del servidor: ${response.code}"
                            }

                            onError(
                                if (responseBody.isBlank()) message
                                else "$message - $responseBody"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(
                        "No se pudo autenticar la petición de registro: " +
                                (e.message ?: "error de conexión")
                    )
                }
            }
        }
    }
}

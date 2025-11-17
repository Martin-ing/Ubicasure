package com.example.ubicasure.screens.Login

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.withContext
import org.json.JSONArray

class RegisterViewModel : ViewModel() {
    var paso by mutableStateOf(1)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var imagenUri by mutableStateOf<Uri?>(null)

    var date by mutableStateOf("")
    var btype by mutableStateOf("")
    var idToken: String? by mutableStateOf("")

    private val auth: FirebaseAuth = Firebase.auth

    fun signUpAuth(onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                idToken = result.user?.uid
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("holab", e.localizedMessage ?: "" )
                onError(e.localizedMessage ?: "Error al registrar usuario")
            }
    }

    fun registerUserProfile(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (idToken == null) {
            onError("El token de autenticación no está disponible. Intenta de nuevo.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val json = """
                    {
                        "firebaseUid": "$idToken",
                        "email": "$email",
                        "fullName": "$name",
                        "phone": "$phone",
                        "birthDate": "$date",
                        "bloodType": "$btype"
                    }
                """.trimIndent()

                Log.e("hola", json)

                val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)

                val request = Request.Builder()
                    .url("https://ubicasure-back-402661808663.us-central1.run.app/users/register")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        val errorBody = response.body?.string() ?: "Error desconocido"
                        onError("Error del servidor: ${response.code} - $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Fallo en la conexión: ${e.message}")
                }
            }
        }
    }



}

package com.example.ubicasure.screens.Login

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ubicasure.api.UserApiService
import com.example.ubicasure.auth.FirebaseTokenProvider
import com.example.ubicasure.models.UpdateUserRequest
import com.example.ubicasure.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginViewModel : ViewModel() {

    companion object {
        private const val PREFS_NAME = "mi_app_prefs"

        private const val KEY_USUARIO = "usuario"
        private const val KEY_FIREBASE_UID = "firebaseUid"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "fullName"
        private const val KEY_PHONE = "phone"
        private const val KEY_BIRTH_DATE = "birthDate"
        private const val KEY_BLOOD_TYPE = "bloodType"
        private const val KEY_TELEGRAM_USERNAME = "telegramUsername"
    }

    private lateinit var prefs: SharedPreferences
    private val auth: FirebaseAuth = Firebase.auth

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://ubicasure-back-402661808663.us-central1.run.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val userApi = retrofit.create(UserApiService::class.java)

    private val _usuario = MutableStateFlow("sin usuario")
    val usuario: StateFlow<String> = _usuario

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _rol = MutableStateFlow("user")

    private val _context = MutableStateFlow<Context?>(null)
    val context: StateFlow<Context?> = _context

    fun setContext(cont: Context) {
        _context.value = cont.applicationContext
        prefs = cont.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val usuarioGuardado = prefs.getString(KEY_USUARIO, "sin usuario") ?: "sin usuario"
        _usuario.value = usuarioGuardado
        _userProfile.value = loadStoredUserProfile()
    }

    fun signInAuth(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val authenticatedEmail = result.user?.email

                if (authenticatedEmail.isNullOrBlank()) {
                    auth.signOut()
                    onError("No se pudo obtener el correo del usuario autenticado.")
                    return@addOnSuccessListener
                }

                fetchAndSaveUserProfile(
                    email = authenticatedEmail,
                    onSuccess = onSuccess,
                    onError = { error ->
                        // Evita dejar una sesión de Firebase activa sin datos de perfil cargados.
                        auth.signOut()
                        _usuario.value = "sin usuario"
                        _userProfile.value = null
                        onError(error)
                    }
                )
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Error al iniciar sesión")
            }
    }

    private fun fetchAndSaveUserProfile(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authorization = FirebaseTokenProvider.getAuthorizationHeader()
                val profile = userApi.getUserByEmail(
                    email = email,
                    authorization = authorization
                )

                ensurePrefsInitialized()
                saveUserProfile(profile)

                _userProfile.value = profile
                _usuario.value = profile.email

                onSuccess()
            } catch (e: HttpException) {
                val message = when (e.code()) {
                    404 -> "El usuario inició sesión, pero no existe su perfil en la base de datos."
                    400 -> "El servidor rechazó el correo enviado."
                    401 -> "La sesión de Firebase no es válida o expiró. Vuelve a iniciar sesión."
                    403 -> "No tienes permiso para consultar este perfil."
                    else -> "No se pudieron obtener los datos del usuario. Código ${e.code()}."
                }
                onError(message)
            } catch (e: Exception) {
                onError("No se pudieron obtener los datos del usuario: ${e.message ?: "error de conexión"}")
            }
        }
    }

    fun updateUserProfile(
        fullName: String,
        phone: String,
        birthDate: String,
        bloodType: String,
        telegramUsername: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentProfile = _userProfile.value

        if (currentProfile == null || currentProfile.email.isBlank()) {
            onError("No hay un perfil de usuario cargado.")
            return
        }

        if (
            fullName.isBlank() ||
            phone.isBlank() ||
            birthDate.isBlank() ||
            bloodType.isBlank() ||
            telegramUsername.isBlank()
        ) {
            onError("Todos los campos son obligatorios.")
            return
        }

        viewModelScope.launch {
            try {
                val request = UpdateUserRequest(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    birthDate = birthDate.trim(),
                    bloodType = bloodType.trim(),
                    telegramUsername = telegramUsername.trim()
                )

                val authorization = FirebaseTokenProvider.getAuthorizationHeader()

                val response = userApi.updateUser(
                    email = currentProfile.email,
                    authorization = authorization,
                    request = request
                )

                // Actualizamos también la copia local para que el resto de pantallas
                // use inmediatamente los datos nuevos sin requerir otro inicio de sesión.
                val updatedProfile = currentProfile.copy(
                    fullName = request.fullName,
                    phone = request.phone,
                    birthDate = request.birthDate,
                    bloodType = request.bloodType,
                    telegramUsername = request.telegramUsername
                )

                ensurePrefsInitialized()
                saveUserProfile(updatedProfile)
                _userProfile.value = updatedProfile
                _usuario.value = updatedProfile.email

                onSuccess(
                    response.msg.ifBlank { "Usuario actualizado exitosamente." }
                )
            } catch (e: HttpException) {
                val errorBody = try {
                    e.response()?.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }

                val message = when (e.code()) {
                    400 -> "Faltan datos obligatorios o alguno de los datos enviados no es válido."
                    401 -> "La sesión de Firebase no es válida o expiró. Vuelve a iniciar sesión."
                    403 -> "No tienes permiso para editar este perfil."
                    404 -> "Usuario no encontrado."
                    else -> "No se pudo actualizar el usuario. Código ${e.code()}."
                }

                onError(
                    if (errorBody.isNullOrBlank()) message
                    else "$message $errorBody"
                )
            } catch (e: Exception) {
                onError(
                    "No se pudieron guardar los cambios: ${e.message ?: "error de conexión"}"
                )
            }
        }
    }

    private fun saveUserProfile(profile: UserProfile) {
        prefs.edit {
            putString(KEY_USUARIO, profile.email)
            putString(KEY_FIREBASE_UID, profile.firebaseUid)
            putString(KEY_EMAIL, profile.email)
            putString(KEY_FULL_NAME, profile.fullName)
            putString(KEY_PHONE, profile.phone)
            putString(KEY_BIRTH_DATE, profile.birthDate)
            putString(KEY_BLOOD_TYPE, profile.bloodType)

            if (profile.telegramUsername.isNullOrBlank()) {
                remove(KEY_TELEGRAM_USERNAME)
            } else {
                putString(KEY_TELEGRAM_USERNAME, profile.telegramUsername)
            }
        }
    }

    private fun loadStoredUserProfile(): UserProfile? {
        if (!::prefs.isInitialized) return null

        val email = prefs.getString(KEY_EMAIL, null) ?: return null

        return UserProfile(
            firebaseUid = prefs.getString(KEY_FIREBASE_UID, "") ?: "",
            email = email,
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            birthDate = prefs.getString(KEY_BIRTH_DATE, "") ?: "",
            bloodType = prefs.getString(KEY_BLOOD_TYPE, "") ?: "",
            telegramUsername = prefs.getString(KEY_TELEGRAM_USERNAME, null)
        )
    }

    private fun ensurePrefsInitialized() {
        if (::prefs.isInitialized) return

        val appContext = _context.value
            ?: throw IllegalStateException("El contexto de LoginViewModel no ha sido inicializado.")

        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _usuario.value = "sin usuario"
        _userProfile.value = null

        if (::prefs.isInitialized) {
            prefs.edit {
                remove(KEY_USUARIO)
                remove(KEY_FIREBASE_UID)
                remove(KEY_EMAIL)
                remove(KEY_FULL_NAME)
                remove(KEY_PHONE)
                remove(KEY_BIRTH_DATE)
                remove(KEY_BLOOD_TYPE)
                remove(KEY_TELEGRAM_USERNAME)
            }
        }
    }
}

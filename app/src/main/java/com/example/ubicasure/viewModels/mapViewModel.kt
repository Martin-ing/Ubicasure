package com.example.ubicasure.viewModels

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.ubicasure.models.Station
import com.example.ubicasure.auth.FirebaseTokenProvider
import com.example.ubicasure.api.AlertHttpException
import com.example.ubicasure.api.AlertImageAttachment
import com.example.ubicasure.api.AlertsRepository
import com.example.ubicasure.models.AlertResponse
import com.example.ubicasure.models.StationsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException
import java.io.File
import java.util.Locale

sealed class AlertSendState {
    object Idle : AlertSendState()
    object Sending : AlertSendState()
    data class Success(val response: AlertResponse) : AlertSendState()
    data class Error(val message: String, val deliveryUncertain: Boolean = false) : AlertSendState()
}

interface ChatApi {
    @POST("chats/")
    suspend fun createChat(@Body body: ChatRequest): ChatResponse
}

data class ChatRequest(
    val sender: String,
    val receiver: String
)

data class ChatResponse(
    val users: List<String>
)

class StationRepository {
    private val client = OkHttpClient()

    suspend fun getStations(lat: Double, lon: Double, searchRadius: Int): List<Station> {
        return withContext(Dispatchers.IO) {
            val url =
                "https://ubicasure-back-402661808663.us-central1.run.app/stations?guatemalaCityLocation=$lat,$lon&searchRadius=$searchRadius"

            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(StationsResponse::class.java)
            val stationsResponse = adapter.fromJson(body)

            val estaciones = mutableListOf<Station>()
            stationsResponse?.fire_stations?.let { estaciones.addAll(it) }
            stationsResponse?.police_stations?.let { estaciones.addAll(it) }
            estaciones
        }
    }
}

class MapViewModel : ViewModel() {

    private val repository = StationRepository()
    private val client = OkHttpClient()
    private val alertsRepository = AlertsRepository()

    private val _alertState = MutableStateFlow<AlertSendState>(AlertSendState.Idle)
    val alertState: StateFlow<AlertSendState> = _alertState

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://ubicasure-back-402661808663.us-central1.run.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val chatApi = retrofit.create(ChatApi::class.java)

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _stationsError = MutableStateFlow<String?>(null)
    val stationsError: StateFlow<String?> = _stationsError

    fun obtenerEstaciones(lat: Double, lon: Double, searchRadius: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _stations.value = repository.getStations(lat, lon, searchRadius)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _stationsError.value = "No se pudieron cargar las estaciones. Revisa tu conexión."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStationsError() {
        _stationsError.value = null
    }

    fun sendPanicAlert(context: Context, lat: Double, lon: Double, imageUri: Uri? = null) {
        // Bloquear pulsaciones dobles antes de lanzar la corrutina.
        if (_alertState.value is AlertSendState.Sending ||
            _alertState.value is AlertSendState.Success) return

        if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            _alertState.value = AlertSendState.Error("No hay una ubicación válida para enviar la alerta.")
            return
        }

        val appContext = context.applicationContext
        _alertState.value = AlertSendState.Sending
        viewModelScope.launch {
            var attachment: AlertImageAttachment? = null
            var requestStarted = false
            try {
                withContext(Dispatchers.IO) {
                    attachment = imageUri?.let { copyAlertImage(appContext, it) }
                }
                val authorization = FirebaseTokenProvider.getAuthorizationHeader()
                requestStarted = true
                val response = alertsRepository.sendAlert(authorization, lat, lon, attachment)
                _alertState.value = AlertSendState.Success(response)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AlertHttpException) {
                // El backend inserta antes de terminar el análisis: un 5xx puede
                // ocurrir después del INSERT. No hacer reintentos automáticos.
                val uncertain = e.statusCode >= 500 || e.statusCode == 408
                val message = when (e.statusCode) {
                    401, 403 -> "Tu sesión no es válida. Vuelve a iniciar sesión."
                    404 -> "No se encontró tu usuario. Revisa tu sesión."
                    413 -> "La imagen es demasiado grande. Selecciona una más pequeña."
                    400, 415, 422 -> "No se pudo aceptar la alerta. Revisa la imagen y tu ubicación."
                    429 -> "Hay demasiadas solicitudes. Espera un momento antes de enviar otra alerta."
                    else -> if (uncertain) {
                        "No pudimos confirmar el resultado. La alerta podría haberse registrado; " +
                            "enviarla de nuevo podría duplicarla."
                    } else {
                        "No se pudo enviar la alerta (error ${e.statusCode})."
                    }
                }
                _alertState.value = AlertSendState.Error(message, uncertain)
            } catch (e: Exception) {
                _alertState.value = if (requestStarted) {
                    AlertSendState.Error(
                        "Se perdió la conexión o se agotó el tiempo de espera. La alerta podría " +
                            "haberse registrado; enviarla de nuevo podría duplicarla.",
                        deliveryUncertain = true
                    )
                } else {
                    AlertSendState.Error(
                        when (e) {
                            is IOException -> "No se pudo leer la imagen o conectar. Revisa tu conexión o selecciona otra imagen."
                            is SecurityException -> "No se puede acceder a la imagen. Vuelve a seleccionarla."
                            is IllegalArgumentException -> e.message ?: "La imagen no es válida."
                            else -> "No se pudo preparar la alerta. Revisa tu conexión y tu sesión."
                        }
                    )
                }
            } finally {
                // La imagen se transmite desde disco; no se carga entera en RAM.
                attachment?.file?.delete()
            }
        }
    }

    fun clearAlertResult() {
        if (_alertState.value !is AlertSendState.Sending) {
            _alertState.value = AlertSendState.Idle
        }
    }

    private fun copyAlertImage(context: Context, uri: Uri): AlertImageAttachment {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)?.lowercase(Locale.ROOT)
        require(mimeType != null && mimeType.startsWith("image/")) {
            "No se pudo reconocer la imagen. Selecciona otra imagen."
        }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "img"
        val file = File.createTempFile("alert_", ".$extension", context.cacheDir)
        try {
            val input = resolver.openInputStream(uri)
                ?: throw IOException("No se pudo abrir la imagen")
            input.use { source -> file.outputStream().use { target -> source.copyTo(target) } }
            require(file.length() > 0L) { "La imagen está vacía. Selecciona otra imagen." }
            return AlertImageAttachment(file, mimeType)
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    fun createChat(
        sender: String,
        receiver: String,
        navController: NavController,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val exists = getChatIfExists(sender, receiver)

                if (!exists) {
                    chatApi.createChat(ChatRequest(sender, receiver))
                    onSuccess()
                }

            } catch (e: Exception) {
                onError(e.message ?: "Error al crear chat")
            }
            navController.navigate("chat")
        }
    }

    suspend fun getChatIfExists(user1: String, user2: String): Boolean {
        return withContext(Dispatchers.IO) {
            val url =
                "https://ubicasure-back-402661808663.us-central1.run.app/chats/getByBothUsers/$user1/$user2"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            return@withContext when (response.code) {
                200 -> true
                404 -> false
                else -> false
            }
        }
    }

}

package com.example.ubicasure.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.ubicasure.models.Station
import com.example.ubicasure.models.StationsResponse
import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException

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

    suspend fun getStations(lat: Double, lon: Double): List<Station> {
        return withContext(Dispatchers.IO) {
            val url =
                "https://ubicasure-back-402661808663.us-central1.run.app/stations?guatemalaCityLocation=$lat,$lon&searchRadius=5000"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

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

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://ubicasure-back-402661808663.us-central1.run.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val chatApi = retrofit.create(ChatApi::class.java)

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun obtenerEstaciones(lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getStations(lat, lon)
            _stations.value = result
            _isLoading.value = false
        }
    }

    fun sendPanicAlert(uid: String, lat: Double, lon: Double) {
        val json = """
            {
                "email": "$uid",
                "latitude": $lat,
                "longitude": $lon
            }
        """.trimIndent()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://ubicasure-back-402661808663.us-central1.run.app/alerts")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("Alerta enviada correctamente")
                } else {
                    println("Error al enviar alerta: ${response.code}")
                }
            }
        })
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

package com.example.ubicasure.api

import com.example.ubicasure.models.AlertResponse
import com.example.ubicasure.models.AlertResponseParser
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AlertImageAttachment(val file: File, val mimeType: String)

class AlertHttpException(val statusCode: Int) : IOException("HTTP $statusCode")

class AlertsRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(240, TimeUnit.SECONDS)
        // POST crea un registro: no reintentarlo automáticamente.
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
    private val endpoint: String =
        "https://ubicasure-back-402661808663.us-central1.run.app/alerts"
) {
    suspend fun sendAlert(
        authorization: String,
        latitude: Double,
        longitude: Double,
        image: AlertImageAttachment? = null
    ): AlertResponse {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("latitude", latitude.toString())
            .addFormDataPart("longitude", longitude.toString())

        image?.let {
            multipart.addFormDataPart(
                "images", // Nombre esperado por upload.array("images").
                "alerta.${it.file.extension.ifBlank { "jpg" }}",
                it.file.asRequestBody(it.mimeType.toMediaType())
            )
        }

        val payload = multipart.build()
        val singleAttemptBody = object : RequestBody() {
            override fun contentType() = payload.contentType()
            override fun contentLength() = payload.contentLength()
            override fun writeTo(sink: BufferedSink) = payload.writeTo(sink)
            // Evitar también repeticiones de POST por respuestas 408/503.
            override fun isOneShot() = true
        }
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", authorization)
            .post(singleAttemptBody)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    // Leer/cerrar la respuesta en el hilo de OkHttp, no en la UI.
                    val result = runCatching {
                        response.use {
                            if (!it.isSuccessful) throw AlertHttpException(it.code)
                            val body = try {
                                it.body?.string().orEmpty()
                            } catch (_: IOException) {
                                // Ya llegó un 2xx: la alerta fue aceptada.
                                ""
                            }
                            AlertResponseParser.parse(body, image != null)
                        }
                    }
                    if (continuation.isActive) {
                        result.fold(
                            onSuccess = { continuation.resume(it) },
                            onFailure = { continuation.resumeWithException(it) }
                        )
                    }
                }
            })
        }
    }
}

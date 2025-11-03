package com.example.ubicasure.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File


data class Message(
    val id: String = "",
    val sender: String = "",
    val data: String = "",
    val chatId: String = "",
    val timestamp: String = "",
    val type: String = "",
    val position: String = ""
)

interface MessagesApiService {

    @GET("messages/getByChat/{chatId}")
    suspend fun getMessagesByChat(@Path("chatId") chatId: String): List<Message>

    @POST("messages/")
    suspend fun sendMessage(@Body body: MessageRequest): MessageResponse

    @DELETE("chats/{chatId}")
    suspend fun deleteChat(@Path("chatId") chatId: String): DeleteResponse

    @Multipart
    @POST("messages/")
    suspend fun sendImageMessage(
        @Part image: MultipartBody.Part,
        @Part("chatId") chatId: RequestBody,
        @Part("sender") sender: RequestBody,
        @Part("type") type: RequestBody,
        @Part("position") position: RequestBody
    ): MessageResponse
}

data class MessageRequest(
    val chatId: String,
    val sender: String,
    val type: String = "Texto",
    val position: String = "right",
    val data: String
)

data class MessageResponse(
    val success: Boolean,
    val message: String
)

data class DeleteResponse(
    val success: Boolean,
    val message: String
)

class MessagesViewModel : ViewModel() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://ubicasure-back-402661808663.us-central1.run.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(MessagesApiService::class.java)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    private val db = FirebaseFirestore.getInstance()
    private var messagesListener: ListenerRegistration? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg

    fun loadMessages(chatId: String) {
        _isLoading.value = true
        _errorMsg.value = null
        messagesListener?.remove()

        val query = db.collection("messages")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        messagesListener = query.addSnapshotListener { snapshot, e ->
            _isLoading.value = false

            if (e != null) {
                _errorMsg.value = "Error al escuchar mensajes: ${e.message}"
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val newMessages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                _messages.value = newMessages // Actualiza el StateFlow
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }

    fun sendMessage(
        chatId: String,
        sender: String,
        text: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = MessageRequest(chatId = chatId, sender = sender, data = text)
                val response = api.sendMessage(request)
                if (response.success) onSuccess() else onError(response.message)
            } catch (e: Exception) {
                onError(e.message ?: "Error al enviar mensaje")
            }
        }
    }

    fun deleteChat(chatId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteChat(chatId)
                if (response.success) onSuccess() else onError(response.message)
            } catch (e: Exception) {
                onError(e.message ?: "Error al eliminar chat")
            }
        }
    }

    fun sendImage(
        chatId: String,
        sender: String,
        imageFile: File,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Convertir imagen a MultipartBody
                val requestImageFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val multipartImage = MultipartBody.Part.createFormData(
                    name = "data",
                    filename = imageFile.name,
                    body = requestImageFile
                )

                val chatIdBody = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
                val senderBody = sender.toRequestBody("text/plain".toMediaTypeOrNull())
                val typeBody = "Imagen".toRequestBody("text/plain".toMediaTypeOrNull())
                val positionBody = "right".toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.sendImageMessage(
                    multipartImage,
                    chatIdBody,
                    senderBody,
                    typeBody,
                    positionBody
                )

                if (response.success) {
                    onSuccess()
                } else {
                    onError(response.message)
                }

            } catch (e: Exception) {
                onError(e.message ?: "Error al enviar imagen")
            }
        }
    }

}

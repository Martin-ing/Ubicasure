package com.example.ubicasure.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class Chat(
    val id: String,
    val users: List<String>
)

interface ChatApiService {
    @GET("chats/getByUsername/{username}")
    suspend fun getChatsByUsername(@Path("username") username: String): List<Chat>
}

class ChatViewModel : ViewModel() {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://ubicasure-back-402661808663.us-central1.run.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(ChatApiService::class.java)

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadChats(username: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                val response = api.getChatsByUsername(username)
                _chats.value = response

            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    _chats.value = emptyList()
                    _error.value = null
                } else {
                    _error.value = "Error al cargar chats: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar chats: ${e.message}"

            } finally {
                _loading.value = false
            }
        }
    }

}

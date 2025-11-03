package com.example.ubicasure.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ubicasure.viewModels.ChatViewModel

@Composable
fun ChatScreen(
    navController: NavController,
    usuario: String,
    chatViewModel: ChatViewModel = viewModel()
) {
    val chats by chatViewModel.chats.collectAsState()
    val loading by chatViewModel.loading.collectAsState()
    val error by chatViewModel.error.collectAsState()

    LaunchedEffect(usuario) {
        if (usuario.isNotEmpty()) {
            chatViewModel.loadChats(usuario)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("< Regresar", fontSize = 16.sp, color = Color.Gray)
            }
        }

        Text(
            text = "Chats",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 24.dp)
        )

        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Text(
                    text = error ?: "",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            chats.isEmpty() -> {
                Text(
                    text = "No tienes chats disponibles.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    chats.forEach { chat ->
                        val otherUser = chat.users.firstOrNull { it != usuario } ?: "Desconocido"
                        ChatItem(
                            stationName = otherUser,
                            onClick = {
                                navController.navigate("messages/${chat.id}/$usuario")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(
    stationName: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = stationName,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black
        )
    }
}

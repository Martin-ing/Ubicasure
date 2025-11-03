package com.example.ubicasure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ubicasure.viewModels.MessagesViewModel
import com.example.ubicasure.viewModels.Message
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.rememberLazyListState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    chatId: String,
    usuario: String,
    onBack: () -> Unit,
    viewModel: MessagesViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPreview by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showPreview = true
        }
    }

    val context = LocalContext.current

    fun uriToFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output ->
            inputStream?.copyTo(output)
        }
        return file
    }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = {
                viewModel.deleteChat(
                    chatId,
                    onSuccess = { onBack() },
                    onError = {}
                )
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Eliminar chat",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                errorMsg != null -> {
                    Text(
                        text = "Error: $errorMsg",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                messages.isEmpty() -> {
                    Text(
                        text = "No hay mensajes todavía.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(
                                message = msg,
                                isMine = (msg.sender == usuario)
                            )
                        }
                    }

                    LaunchedEffect(messages) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }

                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Escribe un mensaje...") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(
                                chatId = chatId,
                                sender = usuario,
                                text = messageText,
                                onSuccess = {},
                                onError = {}
                            )
                            messageText = ""
                        }
                    }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { pickImage.launch("image/*") },
                modifier = Modifier.height(56.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text("📷")
            }

            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(
                            chatId = chatId,
                            sender = usuario,
                            text = messageText,
                            onSuccess = {},
                            onError = {}
                        )
                        messageText = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(text = "Enviar", fontSize = 16.sp)
            }
        }
        if (showPreview && selectedImageUri != null) {
            AlertDialog(
                onDismissRequest = { showPreview = false },
                confirmButton = {
                    Button(onClick = {
                        selectedImageUri?.let { uri ->
                            val file = uriToFile(uri)
                            viewModel.sendImage(
                                chatId = chatId,
                                sender = usuario,
                                imageFile = file,
                                onSuccess = { showPreview = false },
                                onError = { showPreview = false }
                            )
                        }
                    }) {
                        Text("Enviar imagen")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPreview = false }) {
                        Text("Cancelar")
                    }
                },
                text = {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Vista previa",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean) {
    val bubbleColor = if (isMine) Color(0xFF424242) else Color(0xFFE0E0E0)
    val textColor = if (isMine) Color.White else Color.Black
    val alignment = if (isMine) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .background(bubbleColor, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            // Mostrar imagen o texto según el tipo
            if (message.type == "Imagen") {
                Image(
                    painter = rememberAsyncImagePainter(message.data),
                    contentDescription = "Imagen enviada",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 250.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = message.data,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.timestamp,
                color = textColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
            )
        }
    }
}

package com.example.ubicasure.screens.Login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = viewModel(), // ViewModel para lógica del formulario
    navController: NavController, // Navegación para volver atrás tras enviar correo
) {
    val context = LocalContext.current // Contexto para mostrar Toasts
    val email by viewModel.email.collectAsState() // Estado observable del email

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), // Padding general alrededor del contenido
        verticalArrangement = Arrangement.Center // Centra verticalmente el contenido
    ) {
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall) // Título principal
        Spacer(Modifier.height(16.dp)) // Espacio entre elementos

        TextField(
            value = email, // Muestra el email actual
            onValueChange = viewModel::onEmailChange, // Actualiza email en ViewModel
            label = { Text("Correo") }, // Etiqueta del TextField
            modifier = Modifier.fillMaxWidth() // Ocupa todo el ancho disponible
        )

        Spacer(modifier = Modifier.height(24.dp)) // Espacio antes del botón

        Button(
            onClick = {
                // Al hacer clic, intenta enviar correo de recuperación
                viewModel.sendPasswordReset(
                    onSuccess = {
                        // Si es exitoso, muestra Toast y vuelve a pantalla anterior
                        Toast.makeText(
                            context,
                            "Correo enviado. Revisa tu bandeja de entrada.",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.popBackStack()
                    },
                    onError = {
                        // Si hay error, muestra mensaje en Toast
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth() // Botón ocupa ancho completo
        ) {
            Text("Enviar correo de recuperación") // Texto del botón
        }
    }
}
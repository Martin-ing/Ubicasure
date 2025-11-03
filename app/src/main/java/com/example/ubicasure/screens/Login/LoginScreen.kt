package com.example.ubicasure.screens.Login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Pantalla de inicio de sesión.
 *
 * @param viewModel              ViewModel encargado de la autenticación.
 * @param onLoginSuccess         Callback que se invoca cuando el login es exitoso.
 * @param onNavigateToRegister   Callback para navegar a la pantalla de registro.
 * @param onNavigateToForgotPassword Callback para navegar a la pantalla de recuperación de contraseña.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    // Contexto para mostrar Toasts
    val context = LocalContext.current

    // Estados locales para los campos de correo y contraseña
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Inicializar contexto en el ViewModel (por ejemplo, para FirebaseAuth)
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Campo de texto para ingresar el correo electrónico
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de texto para ingresar la contraseña
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para iniciar sesión
        Button(
            onClick = {
                // Validar que no haya campos vacíos
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Debes completar ambos campos", Toast.LENGTH_LONG).show()
                } else {
                    // Invocar función del ViewModel para autenticar
                    viewModel.signInAuth(
                        email,
                        password,
                        onSuccess = { onLoginSuccess() },
                        onError = { errorMsg ->
                            // Mostrar mensaje de error en un Toast
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar Sesión")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón para navegar a la pantalla de registro
        Button(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear Cuenta")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Texto clicable para recuperar contraseña
        TextButton(onClick = onNavigateToForgotPassword) {
            Text("Olvidé mi contraseña")
        }
    }
}
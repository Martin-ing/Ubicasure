package com.example.ubicasure.screens.Login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import android.widget.Toast
import androidx.navigation.NavController
import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Pantalla de registro de usuario en dos pasos:
 * 1. Crear cuenta con correo y contraseña.
 * 2. Completar datos de perfil (nombre, especialidades, descripción, currículum y foto).
 */
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = viewModel()
) {
    // Contexto para mostrar Toasts y acceder a lanzadores de ActivityResult
    val context = LocalContext.current

    // Lanzador para seleccionar una imagen (foto de perfil)
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { viewModel.imagenUri = it }

    var expandedEsp by remember { mutableStateOf(false) }
    val tiposSangre = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    val calendar = Calendar.getInstance()
    var dateText by remember { mutableStateOf(viewModel.date ?: "") }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val dayFormatted = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
            val monthFormatted = if ((selectedMonth + 1) < 10) "0${selectedMonth + 1}" else "${selectedMonth + 1}"
            val formattedDate = "$selectedYear/$monthFormatted/$dayFormatted"
            dateText = formattedDate
            viewModel.date = formattedDate
        },
        year,
        month,
        day
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Paso 1: crear cuenta con email y contraseña
        if (viewModel.paso == 1) {
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Correo") },
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("Contraseña") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // Validar campos vacíos antes de llamar al ViewModel
                    if (viewModel.email.isBlank() || viewModel.password.isBlank()) {
                        Toast.makeText(context, "Debes ingresar correo y contraseña", Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.signUpAuth(
                            onSuccess = {
                                // Avanza al paso 2 si el registro fue exitoso
                                viewModel.paso = 2
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Registrarse")
            }

            // Paso 2: completar perfil con nombre, especialidades, descripción, PDF y foto
        } else {
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = viewModel.phone,
                onValueChange = { viewModel.phone = it },
                label = { Text("Télefono") },
                modifier = Modifier.fillMaxWidth(0.9f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
            ) {
                OutlinedTextField(
                    value = viewModel.date,
                    onValueChange = {viewModel.date = it },
                    readOnly = true,
                    label = { Text("Fecha de nacimiento") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            datePickerDialog.show()
                        }
                )
            }


            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
            ) {
                OutlinedTextField(
                    value = viewModel.btype,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de sangre") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expandedEsp = !expandedEsp }
                )

                DropdownMenu(
                    expanded = expandedEsp,
                    onDismissRequest = { expandedEsp = false },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    tiposSangre.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = {
                                viewModel.btype = tipo // guardar la selección
                                expandedEsp = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Botón para lanzar selector de imagen; texto cambia si ya hay una imagen seleccionada
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(
                    if (viewModel.imagenUri == null)
                        "Subir foto de perfil"
                    else
                        "Foto subida"
                )
            }


            // Vista previa de la imagen de perfil si ya fue seleccionada
            viewModel.imagenUri?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Botón final para enviar todos los datos de registro al ViewModel
            Button(
                onClick = {
                    // Llamar a la nueva función en el ViewModel
                    viewModel.registerUserProfile(
                        context,
                        onSuccess = {
                            Toast.makeText(context, "¡Registro completado!", Toast.LENGTH_LONG).show()
                            // Navegar a la pantalla de login o a la principal después del éxito
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Registrarse")
            }
        }
    }
}
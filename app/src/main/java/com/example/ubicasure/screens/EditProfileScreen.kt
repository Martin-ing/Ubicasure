package com.example.ubicasure.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ubicasure.screens.Login.LoginViewModel
import java.util.Calendar

@Composable
fun EditProfileScreen(
    navController: NavController,
    loginViewModel: LoginViewModel
) {
    val context = LocalContext.current
    val profile by loginViewModel.userProfile.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var telegramUsername by remember { mutableStateOf("") }
    var expandedBloodType by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    LaunchedEffect(profile) {
        profile?.let { user ->
            fullName = user.fullName
            phone = user.phone
            birthDate = normalizeBirthDate(user.birthDate)
            bloodType = user.bloodType
            telegramUsername = user.telegramUsername.orEmpty()
        }
    }

    val initialDate = parseDateParts(birthDate)
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val dayFormatted = selectedDay.toString().padStart(2, '0')
            val monthFormatted = (selectedMonth + 1).toString().padStart(2, '0')
            birthDate = "$selectedYear/$monthFormatted/$dayFormatted"
        },
        initialDate?.first ?: calendar.get(Calendar.YEAR),
        (initialDate?.second ?: (calendar.get(Calendar.MONTH) + 1)) - 1,
        initialDate?.third ?: calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Regresar a ajustes"
                )
            }

            Text(
                text = "Modificar perfil",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (profile == null) {
            Text("No se pudieron cargar los datos del usuario.")
            return@Column
        }

        OutlinedTextField(
            value = profile?.email.orEmpty(),
            onValueChange = {},
            enabled = false,
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth(0.9f),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(0.9f),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth(0.9f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = telegramUsername,
            onValueChange = { telegramUsername = it },
            label = { Text("Usuario de Telegram") },
            placeholder = { Text("@usuario") },
            modifier = Modifier.fillMaxWidth(0.9f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            OutlinedTextField(
                value = birthDate,
                onValueChange = {},
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
                    .clickable { datePickerDialog.show() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            OutlinedTextField(
                value = bloodType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de sangre") },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expandedBloodType = !expandedBloodType }
            )

            DropdownMenu(
                expanded = expandedBloodType,
                onDismissRequest = { expandedBloodType = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                bloodTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            bloodType = type
                            expandedBloodType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (
                    fullName.isBlank() ||
                    phone.isBlank() ||
                    birthDate.isBlank() ||
                    bloodType.isBlank() ||
                    telegramUsername.isBlank()
                ) {
                    Toast.makeText(
                        context,
                        "Debes completar todos los campos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                isSaving = true
                loginViewModel.updateUserProfile(
                    fullName = fullName,
                    phone = phone,
                    birthDate = birthDate,
                    bloodType = bloodType,
                    telegramUsername = telegramUsername,
                    onSuccess = { message ->
                        isSaving = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    },
                    onError = { error ->
                        isSaving = false
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Text(if (isSaving) "Guardando..." else "Guardar cambios")
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun normalizeBirthDate(value: String): String {
    if (value.isBlank()) return ""

    val dateOnly = value.substringBefore("T").substringBefore(" ")
    return when {
        dateOnly.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> dateOnly.replace("-", "/")
        else -> dateOnly
    }
}

private fun parseDateParts(value: String): Triple<Int, Int, Int>? {
    val normalized = normalizeBirthDate(value)
    val parts = normalized.split("/")
    if (parts.size != 3) return null

    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null

    if (month !in 1..12 || day !in 1..31) return null
    return Triple(year, month, day)
}

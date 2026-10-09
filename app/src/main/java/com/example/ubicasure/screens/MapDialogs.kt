package com.example.ubicasure.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.ubicasure.models.AlertResponse
import com.example.ubicasure.models.Station
import com.example.ubicasure.viewModels.AlertSendState

@Composable
internal fun StationDetailsDialog(
    station: Station,
    onDismiss: () -> Unit,
    onStartChat: () -> Unit
) {
    val context = LocalContext.current
    val isPolice = station.type.equals("police", ignoreCase = true)
    val phoneNumber = station.phone?.trim()?.takeIf { it.isNotEmpty() }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            DialogIcon(if (isPolice) Icons.Default.LocalPolice else Icons.Default.LocalFireDepartment)
        },
        title = { Text(station.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isPolice) "Estación de policía" else "Estación de bomberos",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
                StationDetailRow(
                    Icons.Default.LocationOn, "Dirección",
                    station.address.ifBlank { "No disponible" }
                )
                phoneNumber?.let { phone ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StationDetailRow(Icons.Default.Phone, "Teléfono", phone)
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.fromParts("tel", phone, null)
                                )
                                try {
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        "No hay una aplicación de teléfono disponible.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (_: SecurityException) {
                                    Toast.makeText(
                                        context,
                                        "No se pudo abrir el marcador.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Llamar")
                        }
                    }
                }
                StationDetailRow(
                    Icons.Default.Schedule,
                    "Horario",
                    station.opening_hours?.filter { it.isNotBlank() }
                        ?.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "No disponible"
                )
            }
        },
        confirmButton = {
            Button(onClick = onStartChat) {
                Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Iniciar chat")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun StationDetailRow(icon: ImageVector, label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
internal fun SendAlertDialog(
    selectedImage: String?,
    state: AlertSendState,
    locationAvailable: Boolean,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    val sending = state is AlertSendState.Sending
    val error = state as? AlertSendState.Error
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !sending,
            dismissOnClickOutside = !sending
        ),
        shape = RoundedCornerShape(24.dp),
        icon = { DialogIcon(Icons.Default.NotificationsActive) },
        title = { Text(if (sending) "Enviando alerta" else "Enviar alerta", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Se enviará tu ubicación a las estaciones cercanas. Puedes adjuntar una imagen de manera opcional.")

                if (selectedImage != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImage),
                        contentDescription = "Imagen adjunta a la alerta",
                        modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    TextButton(
                        onClick = onRemoveImage,
                        enabled = !sending,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Quitar imagen")
                    }
                }

                OutlinedButton(
                    onClick = onPickImage,
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedImage == null) "Adjuntar imagen (opcional)" else "Cambiar imagen")
                }

                if (!locationAvailable && !sending) {
                    Text(
                        "Todavía no se ha obtenido tu ubicación. Cierra este cuadro y pulsa Mi ubicación antes de enviar.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                error?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(it.message, modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                if (sending) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(if (selectedImage == null) "Enviando tu alerta…"
                             else "Enviando imagen y esperando el análisis…")
                    }
                }
            }
        },
        confirmButton = {
            // Ante un resultado incierto solo se permite cerrar; no invitar a
            // repetir inmediatamente un POST que pudo insertar la alerta.
            if (error?.deliveryUncertain != true) {
                Button(onClick = onSend, enabled = !sending && locationAvailable) {
                    Text(if (sending) "Enviando…" else "Enviar alerta")
                }
            } else {
                Button(onClick = onDismiss) { Text("Entendido") }
            }
        },
        dismissButton = {
            if (error?.deliveryUncertain != true) {
                TextButton(onClick = onDismiss, enabled = !sending) { Text("Cancelar") }
            }
        }
    )
}

@Composable
internal fun AlertResultDialog(response: AlertResponse, onDismiss: () -> Unit) {
    val showAnalysis = response.imageWasAttached && response.hasAnalysis
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { DialogIcon(if (showAnalysis) Icons.Default.ImageSearch else Icons.Default.CheckCircle) },
        title = { Text(if (showAnalysis) "Análisis de la imagen" else "Alerta enviada", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tu alerta fue registrada correctamente.")
                when {
                    !response.responseWasReadable -> Text(
                        "No pudimos leer el detalle de la respuesta. No es necesario volver a enviar la alerta."
                    )
                    showAnalysis -> {
                        Text(
                            "Hemos detectado lo siguiente en el análisis de la imagen enviada",
                            fontWeight = FontWeight.SemiBold
                        )
                        // TODO: dibujar aquí los hallazgos de response.analysis
                        // cuando el backend defina su contrato. No asumir etiquetas,
                        // porcentajes ni que una respuesta vacía significa ausencia de riesgo.
                    }
                    response.imageWasAttached -> Text(
                        "No recibimos un resultado de análisis para la imagen. Tu alerta ya está registrada."
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Entendido") } }
    )
}

@Composable
private fun DialogIcon(icon: ImageVector) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Icon(
            icon, contentDescription = null,
            modifier = Modifier.padding(14.dp).size(28.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

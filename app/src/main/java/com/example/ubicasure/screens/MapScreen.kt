package com.example.ubicasure.screens

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ubicasure.R
import com.example.ubicasure.viewModels.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.ubicasure.screens.Login.LoginViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(navController: NavController, usuario: String, signOut: () -> Unit) {
    val context = LocalContext.current
    val guatemala = LatLng(14.6349, -90.5069)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(guatemala, 10f)
    }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val viewModel: MapViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()
    val estaciones by viewModel.stations.collectAsState()
    val cargando by viewModel.isLoading.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showDialogAlert by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<com.example.ubicasure.models.Station?>(null) }
    var lastKnownLocation by remember { mutableStateOf<LatLng?>(null) }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Si el permiso está concedido, obtenemos la ubicación y cargamos estaciones
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            loginViewModel.setContext(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    lastKnownLocation = currentLatLng
                    viewModel.obtenerEstaciones(it.latitude, it.longitude)
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (locationPermissionState.status.isGranted) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                val markerIcon = remember {
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.fire_station)
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * 0.4f).toInt(),
                        (bitmap.height * 0.4f).toInt(),
                        false
                    )
                    BitmapDescriptorFactory.fromBitmap(scaledBitmap)
                }

                estaciones.forEach { estacion ->
                    Marker(
                        state = MarkerState(LatLng(estacion.location.lat, estacion.location.lng)),
                        title = estacion.name,
                        snippet = estacion.address,
                        icon = markerIcon,
                        onClick = {
                            selectedStation = estacion
                            showDialog = true
                            true
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Text(
                        text = "Permiso de ubicación necesario",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                        Text("Conceder permiso")
                    }
                }
            }
        }

        if (cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularIconButton(
                icon = Icons.Default.ExitToApp,
                contentDescription = "Cerrar sesión"
            ) {
                loginViewModel.signOut()
                signOut()
                navController.navigate("login") {
                    popUpTo("map") { inclusive = true }
                }
            }

            CircularIconButton(
                icon = Icons.Default.ChatBubble,
                contentDescription = "Chats"
            ) {
                navController.navigate("chat")
            }

            CircularIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Acercar"
            ) {
                cameraPositionState.move(CameraUpdateFactory.zoomIn())
            }

            CircularIconButton(
                icon = Icons.Default.Remove,
                contentDescription = "Alejar"
            ) {
                cameraPositionState.move(CameraUpdateFactory.zoomOut())
            }

            CircularIconButton(
                icon = Icons.Default.LocationOn,
                contentDescription = "Mi ubicación"
            ) {
                lastKnownLocation?.let {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 16f))
                }
            }
        }

        // Botón enviar alerta
        Button(
            onClick = { showDialogAlert = true },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Enviar alerta",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Diálogos
        if (showDialog && selectedStation != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(selectedStation!!.name, fontWeight = FontWeight.Bold) },
                text = { Text("Dirección: ${selectedStation!!.address}") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.createChat(
                            sender = usuario,
                            receiver = selectedStation!!.name,
                            onSuccess = {
                                showDialog = false
                                println("Chat creado correctamente con ${selectedStation!!.name}")
                            },
                            onError = { error ->
                                showDialog = false
                                println("Error al crear chat: $error")
                            }
                        )
                    }) {
                        Text("Iniciar chat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        if (showDialogAlert) {
            AlertDialog(
                onDismissRequest = { showDialogAlert = false },
                title = { Text("Confirmar alerta") },
                text = { Text("Estás a punto de enviar una alerta a las estaciones cercanas, ¿deseas continuar?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDialogAlert = false
                        lastKnownLocation?.let { location ->
                            viewModel.sendPanicAlert(usuario, location.latitude, location.longitude)
                        }
                    }) {
                        Text("Continuar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialogAlert = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Gray)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

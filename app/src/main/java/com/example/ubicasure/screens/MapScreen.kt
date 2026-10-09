package com.example.ubicasure.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ubicasure.models.Station
import com.example.ubicasure.viewModels.AlertSendState
import com.example.ubicasure.viewModels.MapViewModel
import com.example.ubicasure.viewModels.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navController: NavController,
    usuario: String,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val guatemala = LatLng(14.6349, -90.5069)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(guatemala, 10f)
    }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationToken = remember { CancellationTokenSource() }
    DisposableEffect(locationToken) {
        onDispose { locationToken.cancel() }
    }

    val viewModel: MapViewModel = viewModel()
    val estaciones by viewModel.stations.collectAsState()
    val cargando by viewModel.isLoading.collectAsState()
    val stationsError by viewModel.stationsError.collectAsState()
    val alertState by viewModel.alertState.collectAsState()
    val searchRadius by settingsViewModel.searchRadius.collectAsState()
    var showDialogAlert by rememberSaveable { mutableStateOf(false) }
    var selectedImage by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStation by remember { mutableStateOf<Station?>(null) }
    var lastKnownLocation by remember { mutableStateOf<LatLng?>(null) }
    var locating by remember { mutableStateOf(false) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val pickAlertImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImage = uri.toString()
    }

    fun centerOnLocation(latitude: Double, longitude: Double) {
        val location = LatLng(latitude, longitude)
        lastKnownLocation = location
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(location, 16f))
    }

    fun requestCurrentLocation() {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
            return
        }
        if (locating) return
        locating = true
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, locationToken.token)
            .addOnSuccessListener { location ->
                locating = false
                if (location != null) {
                    centerOnLocation(location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "No se pudo obtener tu ubicación. Revisa que esté activada.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                locating = false
                Toast.makeText(context, "No se pudo obtener tu ubicación. Intenta de nuevo.", Toast.LENGTH_LONG).show()
            }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) locationPermissionState.launchPermissionRequest()
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) centerOnLocation(location.latitude, location.longitude)
                else requestCurrentLocation()
            }.addOnFailureListener { requestCurrentLocation() }
        }
    }

    LaunchedEffect(lastKnownLocation, searchRadius) {
        lastKnownLocation?.let {
            viewModel.obtenerEstaciones(it.latitude, it.longitude, searchRadius)
        }
    }

    LaunchedEffect(alertState) {
        if (alertState is AlertSendState.Success) {
            showDialogAlert = false
            selectedImage = null
        }
    }

    LaunchedEffect(stationsError) {
        stationsError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStationsError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (locationPermissionState.status.isGranted) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
            ) {
                val fireIcon = remember {
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.fire_station)
                    BitmapDescriptorFactory.fromBitmap(
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * 0.4f).toInt(), (bitmap.height * 0.4f).toInt(), false)
                    )
                }
                val policeIcon = remember {
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.police_station)
                    BitmapDescriptorFactory.fromBitmap(
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * 0.4f).toInt(), (bitmap.height * 0.4f).toInt(), false)
                    )
                }
                estaciones.forEach { estacion ->
                    Marker(
                        state = MarkerState(position = LatLng(estacion.location.lat, estacion.location.lng)),
                        title = estacion.name,
                        snippet = estacion.address,
                        icon = if (estacion.type.equals("police", ignoreCase = true)) policeIcon else fireIcon,
                        onClick = {
                            selectedStation = estacion
                            true
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LocationOff, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                    Text("Permiso de ubicación necesario", fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { locationPermissionState.launchPermissionRequest() }) { Text("Conceder permiso") }
                }
            }
        }

        if (cargando) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp, bottom = 112.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularIconButton(Icons.Default.Settings, "Ajustes") { navController.navigate("settings") }
                CircularIconButton(Icons.Default.ChatBubble, "Chats") { navController.navigate("chat") }
                CircularIconButton(Icons.Default.Phone, "Teléfonos") { navController.navigate("emergencyContacts") }
                CircularIconButton(Icons.Default.Add, "Acercar") {
                    cameraPositionState.move(CameraUpdateFactory.zoomIn())
                }
                CircularIconButton(Icons.Default.Remove, "Alejar") {
                    cameraPositionState.move(CameraUpdateFactory.zoomOut())
                }
                CircularIconButton(Icons.Default.MyLocation, "Mi ubicación", enabled = !locating) {
                    requestCurrentLocation()
                }
            }

            Button(
                onClick = {
                    viewModel.clearAlertResult()
                    showDialogAlert = true
                },
                enabled = alertState !is AlertSendState.Sending,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp).heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                contentColor = Color.White
            )
            ) {
                Text("Enviar alerta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        selectedStation?.let { station ->
            StationDetailsDialog(
                station = station,
                onDismiss = { selectedStation = null },
                onStartChat = {
                    viewModel.createChat(
                        sender = usuario,
                        receiver = station.name,
                        navController = navController,
                        onSuccess = { selectedStation = null },
                        onError = { error ->
                            selectedStation = null
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        if (alertState !is AlertSendState.Success &&
            (showDialogAlert || alertState is AlertSendState.Sending || alertState is AlertSendState.Error)) {
            SendAlertDialog(
                selectedImage = selectedImage,
                state = alertState,
                locationAvailable = locationPermissionState.status.isGranted && lastKnownLocation != null,
                onPickImage = {
                    try {
                        pickAlertImage.launch("image/*")
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No hay un selector de imágenes disponible.", Toast.LENGTH_LONG).show()
                    }
                },
                onRemoveImage = { selectedImage = null },
                onDismiss = {
                    showDialogAlert = false
                    selectedImage = null
                    viewModel.clearAlertResult()
                },
                onSend = {
                    lastKnownLocation?.let {
                        viewModel.sendPanicAlert(context, it.latitude, it.longitude, selectedImage?.let(Uri::parse))
                    }
                }
            )
        }

        (alertState as? AlertSendState.Success)?.let { result ->
            AlertResultDialog(result.response, onDismiss = {
                showDialogAlert = false
                selectedImage = null
                viewModel.clearAlertResult()
            })
        }
    }
}

@Composable
fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.5f))
    ) {
        Icon(icon, contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
    }
}

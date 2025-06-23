package com.example.panico.ui.screens

import android.Manifest
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.panico.ui.viewmodel.MapViewModel
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import com.google.maps.android.compose.Circle
import androidx.compose.ui.graphics.lerp
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import android.os.VibratorManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val mapViewModel: MapViewModel = viewModel()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val alerts by mapViewModel.alerts.collectAsState()
    val cameraPosition by mapViewModel.cameraPosition.collectAsState()
    val userLocation by mapViewModel.userLocation.collectAsState()
    val alertSent by mapViewModel.alertSent.collectAsState()
    val cameraPositionState = rememberCameraPositionState()
    val lastAlertSent = remember { mutableStateOf(alertSent) }

    // Vibrar cuando cambia el estado de alerta
    LaunchedEffect(alertSent) {
        if (alertSent != lastAlertSent.value) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
            lastAlertSent.value = alertSent
        }
    }

    // Centrar la cámara cuando se obtenga la ubicación del usuario
    LaunchedEffect(userLocation) {
        userLocation?.let { latLng ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
        }
    }

    // Solicitar el permiso si no está concedido
    LaunchedEffect(Unit) {
        if (locationPermissionState.status is PermissionStatus.Denied) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mensaje de alerta enviada fijo en la parte superior, fuera del Box principal
        if (alertSent) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ALERTA ENVIADA",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                BlinkingDot()
            }
        }
        // El resto de la UI va en un Box que ocupa el resto del espacio
        Box(modifier = Modifier.weight(1f)) {
            when (val status = locationPermissionState.status) {
                is PermissionStatus.Granted -> {
                    LaunchedEffect(true) {
                        mapViewModel.loadAlerts()
                        mapViewModel.centerCameraOnUser()
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true
                        )
                    ) {
                        if (alertSent && userLocation != null) {
                            BlinkingAlertCircle(
                                center = userLocation,
                                radius = 100.0 // metros
                            )
                        }
                    }
                    // Botón grande en la parte inferior, centrado, más grande
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(bottom = 40.dp),
                        contentAlignment = Alignment.Center // Centrar el contenido
                    ) {
                        Button(
                            onClick = {
                                if (!alertSent) mapViewModel.sendAlert()
                                else mapViewModel.endAlert()
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(90.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (!alertSent) Color.Red else Color.Gray
                            )
                        ) {
                            Text(
                                if (!alertSent) "ENVIAR ALERTA" else "TERMINAR ALERTA",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                    }
                }
                is PermissionStatus.Denied -> {
                    if (status.shouldShowRationale) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                                Text("Permitir ubicación")
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text("Permiso de ubicación no disponible")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlinkingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "blinking-dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.0f at 0
                1.0f at 400
                0.0f at 800
            },
            repeatMode = RepeatMode.Restart
        ), label = "dot-alpha"
    )
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .size(16.dp)
            .background(Color.White.copy(alpha = alpha), shape = CircleShape)
    )
}

// Círculo rojo parpadeante en el mapa, oscila entre dos tonos de rojo con transparencia
@Composable
fun BlinkingAlertCircle(center: LatLng?, radius: Double) {
    if (center == null) return
    val infiniteTransition = rememberInfiniteTransition(label = "blinking-circle")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.3f at 0
                0.6f at 450
                0.3f at 900
            },
            repeatMode = RepeatMode.Restart
        ), label = "circle-alpha"
    )
    val color1 = Color(0x88FF0000) // Rojo con transparencia
    val color2 = Color(0x44FF0000) // Rojo más transparente
    val color = lerp(color1, color2, alpha)
    // Animar el radio entre 50 y 100 metros
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.5f at 0
                1.0f at 450
                0.5f at 900
            },
            repeatMode = RepeatMode.Restart
        ), label = "circle-scale"
    )
    val animatedRadius = radius * scale
    Circle(
        center = center,
        radius = animatedRadius,
        fillColor = color,
        strokeColor = color,
        strokeWidth = 0f
    )
} 
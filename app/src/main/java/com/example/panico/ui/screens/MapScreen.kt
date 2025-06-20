package com.example.panico.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val mapViewModel: MapViewModel = viewModel()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val alerts by mapViewModel.alerts.collectAsState()
    val cameraPosition by mapViewModel.cameraPosition.collectAsState()
    val userLocation by mapViewModel.userLocation.collectAsState()
    val cameraPositionState = rememberCameraPositionState()

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

    when (val status = locationPermissionState.status) {
        is PermissionStatus.Granted -> {
            // Cargar alertas y centrar cámara solo una vez
            LaunchedEffect(true) {
                mapViewModel.loadAlerts()
                mapViewModel.centerCameraOnUser()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = true
                    )
                ) {
                    // No mostrar ningún marcador
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
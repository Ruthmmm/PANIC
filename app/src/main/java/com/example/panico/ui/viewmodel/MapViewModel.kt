package com.example.panico.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.panico.data.models.Alert
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val context = application.applicationContext

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _cameraPosition = MutableStateFlow(CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 15f))
    val cameraPosition: StateFlow<CameraPosition> = _cameraPosition.asStateFlow()

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    fun loadAlerts() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("alerts")
                    .whereEqualTo("active", true)
                    .get()
                    .await()
                _alerts.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Alert::class.java)?.copy(id = doc.id)
                }.filter { it.active }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun centerCameraOnUser() {
        viewModelScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    _userLocation.value = latLng
                    _cameraPosition.value = CameraPosition.fromLatLngZoom(latLng, 15f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 
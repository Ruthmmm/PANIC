package com.example.panico.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.panico.data.models.Alert
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val context = application.applicationContext

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _cameraPosition = MutableStateFlow(CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 15f))
    val cameraPosition: StateFlow<CameraPosition> = _cameraPosition.asStateFlow()

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    private val _alertSent = MutableStateFlow(false)
    val alertSent: StateFlow<Boolean> = _alertSent.asStateFlow()

    private val _lastAlertId = MutableStateFlow<String?>(null)
    val lastAlertId: StateFlow<String?> = _lastAlertId.asStateFlow()

    fun loadAlerts(userId: String = "powerButtonUser") {
        viewModelScope.launch {
            try {
                db.collection("alerts")
                    .whereEqualTo("active", true)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val alertsList = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(Alert::class.java)?.copy(id = doc.id)
                            }.filter { it.active }
                            _alerts.value = alertsList
                            // Buscar si hay una alerta activa del usuario actual
                            val myAlert = alertsList.find { it.userId == userId }
                            if (myAlert != null) {
                                _alertSent.value = true
                                _lastAlertId.value = myAlert.id
                            } else {
                                _alertSent.value = false
                                _lastAlertId.value = null
                            }
                        }
                    }
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

    fun sendAlert(userId: String = "powerButtonUser") {
        viewModelScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    val alert = Alert(
                        id = UUID.randomUUID().toString(),
                        active = true,
                        userId = userId,
                        location = GeoPoint(it.latitude, it.longitude),
                        timestamp = Timestamp.now(),
                        message = "Alerta de emergencia",
                        status = "active"
                    )
                    val ref = db.collection("alerts").add(alert).await()
                    _lastAlertId.value = ref.id
                    _alertSent.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun endAlert() {
        viewModelScope.launch {
            try {
                val alertId = _lastAlertId.value
                if (alertId != null) {
                    db.collection("alerts").document(alertId).update("active", false, "status", "resolved")
                    _alertSent.value = false
                    _lastAlertId.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 
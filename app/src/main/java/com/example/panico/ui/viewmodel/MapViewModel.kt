package com.example.panico.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.panico.data.models.Alert
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import android.util.Log
import android.widget.Toast

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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showAlertSentToast = MutableStateFlow(false)
    val showAlertSentToast: StateFlow<Boolean> = _showAlertSentToast.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _userRole = MutableStateFlow("")
    val userRole: StateFlow<String> = _userRole

    fun loadAlerts() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        viewModelScope.launch {
            try {
                db.collection("alerts")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .addSnapshotListener { snapshot, _ ->
                        Log.d("MapViewModel", "SnapshotListener disparado para userId=$userId")
                        Toast.makeText(context, "Actualización de alertas detectada", Toast.LENGTH_SHORT).show()
                        if (snapshot != null) {
                            val alertsList = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(Alert::class.java)?.copy(id = doc.id)
                            }
                            _alerts.value = alertsList
                            // Solo considerar alerta activa si está activa y no está reemplazada ni terminada
                            val myAlert = alertsList.firstOrNull { it.active && it.status == "active" }
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

    fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                _userName.value = doc.getString("name") ?: ""
                _userRole.value = doc.getString("role") ?: ""
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

    fun sendAlert() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        viewModelScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.lastLocation.await()
                if (location == null) {
                    _errorMessage.value = "No se pudo obtener la ubicación."
                    _showAlertSentToast.value = false
                    return@launch
                }

                // Buscar alerta activa previa del usuario
                val activeAlerts = db.collection("alerts")
                    .whereEqualTo("active", true)
                    .whereEqualTo("userId", userId)
                    .get().await()

                // Marcar como reemplazada cualquier alerta activa previa
                for (doc in activeAlerts.documents) {
                    db.collection("alerts").document(doc.id)
                        .update("active", false, "status", "reemplazada")
                }

                // Crear la nueva alerta
                val alert = Alert(
                    id = UUID.randomUUID().toString(),
                    active = true,
                    userId = userId,
                    location = GeoPoint(location.latitude, location.longitude),
                    timestamp = Timestamp.now(),
                    message = "Alerta de emergencia",
                    status = "active"
                )
                val ref = db.collection("alerts").add(alert).await()
                _lastAlertId.value = ref.id
                _alertSent.value = true
                _errorMessage.value = null
                _showAlertSentToast.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error al enviar la alerta: ${e.localizedMessage}"
                _showAlertSentToast.value = false
            }
        }
    }

    fun endAlert() {
        viewModelScope.launch {
            try {
                val alertId = _lastAlertId.value
                if (alertId != null) {
                    db.collection("alerts").document(alertId).update("active", false, "status", "terminado")
                    _alertSent.value = false
                    _lastAlertId.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetShowAlertSentToast() {
        _showAlertSentToast.value = false
    }
} 
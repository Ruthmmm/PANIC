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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private val _alertTypeShown = MutableStateFlow<String>("")
    val alertTypeShown: StateFlow<String> = _alertTypeShown.asStateFlow()

    private val _alertColor = MutableStateFlow(0xFFD32F2F) // Rojo por defecto
    val alertColor: StateFlow<Long> = _alertColor.asStateFlow()

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

    fun sendAlert(fromPowerButton: Boolean = false) {
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
                // Reemplazar cualquier alerta activa previa
                replaceActiveAlerts(userId)
                val alertTypeName = if (fromPowerButton) "ALERTA CRÍTICA" else "ALERTA URGENTE"
                val alertTypeId = withContext(Dispatchers.IO) { getAlertTypeIdByName(alertTypeName) } ?: ""
                val groupId = withContext(Dispatchers.IO) { getCurrentUserGroupId() } ?: ""
                Log.d("MapViewModel", "userId=$userId")
                Log.d("MapViewModel", "groupId from user doc: $groupId")
                Log.d("MapViewModel", "alertTypeId from alertTypes: $alertTypeId")
                if (alertTypeId.isBlank() || groupId.isBlank() || userId.isBlank()) {
                    _errorMessage.value = "Error: Faltan datos para crear la alerta. userId=$userId, groupId=$groupId, alertTypeId=$alertTypeId"
                    _showAlertSentToast.value = false
                    return@launch
                }
                val observation = if (fromPowerButton) "enviado desde el botón de encendido" else "alerta enviada desde botón"
                val alert = Alert(
                    id = UUID.randomUUID().toString(),
                    active = true,
                    userId = userId,
                    groupId = groupId,
                    alertTypeId = alertTypeId,
                    location = GeoPoint(location.latitude, location.longitude),
                    timestamp = Timestamp.now(),
                    message = "",
                    status = "active",
                    observation = observation
                )
                db.collection("alerts").add(alert).await()
                _lastAlertId.value = alert.id
                _alertTypeShown.value = alertTypeName // <-- siempre se actualiza correctamente
                _alertColor.value = 0xFFD32F2F // Rojo
                // Actualización optimista
                _alerts.value = listOf(alert)
                loadAlerts() // <-- Forzar refresco de la UI
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error al enviar la alerta: ${e.localizedMessage}"
                _showAlertSentToast.value = false
            }
        }
    }

    fun sendSpecificAlert(typeName: String, fromPowerButton: Boolean = false) {
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
                // Reemplazar cualquier alerta activa previa
                replaceActiveAlerts(userId)
                val alertTypeId = withContext(Dispatchers.IO) { getAlertTypeIdByName(typeName) } ?: ""
                val groupId = withContext(Dispatchers.IO) { getCurrentUserGroupId() } ?: ""
                Log.d("MapViewModel", "userId=$userId")
                Log.d("MapViewModel", "groupId from user doc: $groupId")
                Log.d("MapViewModel", "alertTypeId from alertTypes: $alertTypeId")
                if (alertTypeId.isBlank() || groupId.isBlank() || userId.isBlank()) {
                    _errorMessage.value = "Error: Faltan datos para crear la alerta. userId=$userId, groupId=$groupId, alertTypeId=$alertTypeId"
                    _showAlertSentToast.value = false
                    return@launch
                }
                val color = when (typeName) {
                    "INCENDIO" -> 0xFFFF9800
                    "ACCIDENTE VEHICULAR" -> 0xFF2196F3
                    "VIOLENCIA INTRAFAMILIAR" -> 0xFF9C27B0
                    "DESMAYO" -> 0xFF009688
                    "ROBO" -> 0xFFFFC107
                    else -> 0xFF888888
                }
                val observation = if (fromPowerButton) "enviado desde el botón de encendido" else "enviado desde botón de alerta"
                val alert = Alert(
                    id = UUID.randomUUID().toString(),
                    active = true,
                    userId = userId,
                    groupId = groupId,
                    alertTypeId = alertTypeId,
                    location = GeoPoint(location.latitude, location.longitude),
                    timestamp = Timestamp.now(),
                    message = "",
                    status = "active",
                    observation = observation
                )
                db.collection("alerts").add(alert).await()
                _lastAlertId.value = alert.id
                _alertTypeShown.value = typeName
                _alertColor.value = color
                // Actualización optimista
                _alerts.value = listOf(alert)
                loadAlerts() // <-- Forzar refresco de la UI
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error al enviar la alerta: ${e.localizedMessage}"
                _showAlertSentToast.value = false
            }
        }
    }

    fun endAlert(message: String = "") {
        viewModelScope.launch {
            try {
                val alertId = _lastAlertId.value
                if (alertId != null) {
                    db.collection("alerts").document(alertId).update(
                        mapOf(
                            "active" to false,
                            "status" to "terminado",
                            "message" to message,
                            "endTimestamp" to Timestamp.now()
                        )
                    )
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

    private suspend fun replaceActiveAlerts(userId: String) {
        val activeAlerts = db.collection("alerts")
            .whereEqualTo("active", true)
            .whereEqualTo("userId", userId)
            .get().await()

        for (doc in activeAlerts.documents) {
            db.collection("alerts").document(doc.id)
                .update("active", false, "status", "reemplazada")
        }
    }

    private suspend fun getAlertTypeIdByName(name: String): String? {
        val alertTypesSnapshot = db.collection("alertTypes").get().await()
        return alertTypesSnapshot.documents.firstOrNull { it.getString("name") == name }?.id
    }

    private suspend fun getCurrentUserGroupId(): String? {
        val userDoc = db.collection("users").document(FirebaseAuth.getInstance().currentUser?.uid ?: "").get().await()
        return userDoc.getString("groupId")
    }

    fun centerMapOnLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val location = com.google.android.gms.maps.model.LatLng(latitude, longitude)
                _userLocation.value = location
                _cameraPosition.value = CameraPosition.fromLatLngZoom(location, 15f)
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error centering map: ${e.message}")
            }
        }
    }
} 
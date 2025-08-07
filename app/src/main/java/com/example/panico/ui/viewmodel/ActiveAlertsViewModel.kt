package com.example.panico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.panico.data.models.Alert
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class ActiveAlertsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    private val _activeAlerts = MutableStateFlow<List<Alert>>(emptyList())
    val activeAlerts: StateFlow<List<Alert>> = _activeAlerts
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun loadActiveAlerts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Cargar TODAS las alertas (sin filtro)
                val allAlertsSnapshot = db.collection("alerts")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                // Filtrar las alertas activas en el código
                val alertsList = mutableListOf<Alert>()
                for (alertDoc in allAlertsSnapshot.documents) {
                    val active = alertDoc.getBoolean("active") ?: false
                    
                    if (active) {
                        val alert = alertDoc.toObject(Alert::class.java)?.copy(id = alertDoc.id)
                        if (alert != null) {
                            alertsList.add(alert)
                        }
                    }
                }
                
                _activeAlerts.value = alertsList
            } catch (e: Exception) {
                Log.e("ActiveAlertsViewModel", "Error loading active alerts: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun terminateAlert(alertId: String, message: String) {
        viewModelScope.launch {
            try {
                db.collection("alerts").document(alertId).update(
                    mapOf(
                        "active" to false,
                        "status" to "terminado",
                        "message" to message,
                        "endTimestamp" to com.google.firebase.Timestamp.now()
                    )
                ).await()
                
                // Recargar alertas activas después de terminar una
                loadActiveAlerts()
            } catch (e: Exception) {
                Log.e("ActiveAlertsViewModel", "Error terminating alert: ${e.message}")
            }
        }
    }
    
    suspend fun getUserName(userId: String): String {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                Log.d("ActiveAlertsViewModel", "Campos del usuario ${userId}: ${userDoc.data}")
                
                // Usar los campos correctos de la base de datos
                val name = userDoc.getString("name") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                
                Log.d("ActiveAlertsViewModel", "name: '$name', lastName: '$lastName'")
                
                if (name.isNotEmpty() && lastName.isNotEmpty()) {
                    "$name $lastName"
                } else if (name.isNotEmpty()) {
                    name
                } else {
                    "Usuario"
                }
            } else {
                Log.e("ActiveAlertsViewModel", "Usuario ${userId} no existe")
                "Usuario"
            }
        } catch (e: Exception) {
            Log.e("ActiveAlertsViewModel", "Error getting user name: ${e.message}")
            "Usuario"
        }
    }
    
    suspend fun getGroupName(groupId: String): String {
        return try {
            val groupDoc = db.collection("groups").document(groupId).get().await()
            if (groupDoc.exists()) {
                groupDoc.getString("name") ?: "Grupo"
            } else {
                "Grupo"
            }
        } catch (e: Exception) {
            Log.e("ActiveAlertsViewModel", "Error getting group name: ${e.message}")
            "Grupo"
        }
    }
    
    suspend fun getAlertTypeName(alertTypeId: String): String {
        return try {
            val alertTypeDoc = db.collection("alertTypes").document(alertTypeId).get().await()
            if (alertTypeDoc.exists()) {
                alertTypeDoc.getString("name") ?: "Tipo de Alerta"
            } else {
                "Tipo de Alerta"
            }
        } catch (e: Exception) {
            Log.e("ActiveAlertsViewModel", "Error getting alert type name: ${e.message}")
            "Tipo de Alerta"
        }
    }
} 
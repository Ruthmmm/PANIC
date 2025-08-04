package com.example.panico.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.panico.data.models.Alert
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class AlertHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Estados para los dropdowns de edición
    private val _users = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val users: StateFlow<List<Pair<String, String>>> = _users.asStateFlow()
    
    private val _groups = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val groups: StateFlow<List<Pair<String, String>>> = _groups.asStateFlow()
    
    private val _alertTypes = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val alertTypes: StateFlow<List<Pair<String, String>>> = _alertTypes.asStateFlow()
    
    fun loadAllAlerts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Obtener todas las alertas
                val alertsSnapshot = db.collection("alerts")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val alertsList = mutableListOf<Alert>()
                
                for (alertDoc in alertsSnapshot.documents) {
                    val alert = alertDoc.toObject(Alert::class.java)?.copy(id = alertDoc.id)
                    if (alert != null) {
                        alertsList.add(alert)
                    }
                }
                
                _alerts.value = alertsList
                
            } catch (e: Exception) {
                Log.e("AlertHistoryViewModel", "Error loading alerts: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            try {
                db.collection("alerts").document(alertId).delete().await()
                // Recargar la lista después de eliminar
                loadAllAlerts()
            } catch (e: Exception) {
                Log.e("AlertHistoryViewModel", "Error deleting alert: ${e.message}")
            }
        }
    }
    
    fun updateAlert(alertId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            try {
                db.collection("alerts").document(alertId).update(updates).await()
                // Recargar la lista después de actualizar
                loadAllAlerts()
            } catch (e: Exception) {
                Log.e("AlertHistoryViewModel", "Error updating alert: ${e.message}")
            }
        }
    }
    
    fun loadUsers(groupId: String? = null) {
        viewModelScope.launch {
            try {
                val usersQuery = if (groupId != null) {
                    db.collection("users").whereEqualTo("groupId", groupId)
                } else {
                    db.collection("users")
                }
                
                val usersSnapshot = usersQuery.get().await()
                val usersList = mutableListOf<Pair<String, String>>()
                
                for (userDoc in usersSnapshot.documents) {
                    val userId = userDoc.id
                    val firstName = userDoc.getString("name") ?: ""
                    val lastName = userDoc.getString("lastName") ?: ""
                    val fullName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                        "$firstName $lastName"
                    } else if (firstName.isNotEmpty()) {
                        firstName
                    } else {
                        "Usuario"
                    }
                    usersList.add(Pair(userId, fullName))
                }
                
                _users.value = usersList
            } catch (e: Exception) {
                Log.e("AlertHistoryViewModel", "Error loading users: ${e.message}")
            }
        }
    }
    
    fun loadGroups() {
        viewModelScope.launch {
            try {
                val groupsSnapshot = db.collection("groups").get().await()
                val groupsList = mutableListOf<Pair<String, String>>()
                
                for (groupDoc in groupsSnapshot.documents) {
                    val groupId = groupDoc.id
                    val groupName = groupDoc.getString("name") ?: "Grupo"
                    groupsList.add(Pair(groupId, groupName))
                }
                
                _groups.value = groupsList
            } catch (e: Exception) {
                Log.e("AlertHistoryViewModel", "Error loading groups: ${e.message}")
            }
        }
    }
    
    fun loadAlertTypes() {
        viewModelScope.launch {
            try {
                val alertTypesSnapshot = db.collection("alertTypes").get().await()
                val alertTypesList = mutableListOf<Pair<String, String>>()
                
                for (alertTypeDoc in alertTypesSnapshot.documents) {
                    val alertTypeId = alertTypeDoc.id
                    val alertTypeName = alertTypeDoc.getString("name") ?: "Tipo de Alerta"
                    alertTypesList.add(Pair(alertTypeId, alertTypeName))
                }
                
                _alertTypes.value = alertTypesList
            } catch (e: Exception) {
                Log.e("AlertHistoryViewModel", "Error loading alert types: ${e.message}")
            }
        }
    }
    
    suspend fun getUserName(userId: String): String {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val firstName = userDoc.getString("name") ?: ""
            val lastName = userDoc.getString("lastName") ?: ""
            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                "$firstName $lastName"
            } else if (firstName.isNotEmpty()) {
                firstName
            } else {
                "Usuario"
            }
        } catch (e: Exception) {
            Log.e("AlertHistoryViewModel", "Error getting user name: ${e.message}")
            "Usuario"
        }
    }
    
    suspend fun getGroupName(groupId: String): String {
        return try {
            Log.d("AlertHistoryViewModel", "Getting group name for ID: $groupId")
            val groupDoc = db.collection("groups").document(groupId).get().await()
            Log.d("AlertHistoryViewModel", "Group doc exists: ${groupDoc.exists()}")
            if (groupDoc.exists()) {
                val groupName = groupDoc.getString("name")
                Log.d("AlertHistoryViewModel", "Group name from doc: $groupName")
                groupName ?: "Grupo"
            } else {
                Log.d("AlertHistoryViewModel", "Group document does not exist")
                "Grupo"
            }
        } catch (e: Exception) {
            Log.e("AlertHistoryViewModel", "Error getting group name: ${e.message}")
            "Grupo"
        }
    }
    
    suspend fun getAlertTypeName(alertTypeId: String): String {
        return try {
            val alertTypeDoc = db.collection("alertTypes").document(alertTypeId).get().await()
            alertTypeDoc.getString("name") ?: "Tipo de Alerta"
        } catch (e: Exception) {
            Log.e("AlertHistoryViewModel", "Error getting alert type name: ${e.message}")
            "Tipo de Alerta"
        }
    }
    
    // Función para obtener el grupo de un usuario específico
    suspend fun getUserGroup(userId: String): String {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val groupId = userDoc.getString("groupId") ?: ""
                if (groupId.isNotEmpty()) {
                    val groupDoc = db.collection("groups").document(groupId).get().await()
                    groupDoc.getString("name") ?: "Grupo"
                } else {
                    "Sin grupo"
                }
            } else {
                "Usuario no encontrado"
            }
        } catch (e: Exception) {
            Log.e("AlertHistoryViewModel", "Error getting user group: ${e.message}")
            "Error"
        }
    }
    
    // Función para obtener el ID del grupo de un usuario
    suspend fun getUserGroupId(userId: String): String {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                userDoc.getString("groupId") ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("AlertHistoryViewModel", "Error getting user group ID: ${e.message}")
            ""
        }
    }
    
    // Función optimizada para obtener múltiples nombres de usuarios de una vez
    suspend fun getUserNames(userIds: List<String>): Map<String, String> {
        return try {
            val userNames = mutableMapOf<String, String>()
            
            // Usar consultas individuales que son más compatibles
            for (userId in userIds) {
                val userDoc = db.collection("users").document(userId).get().await()
                
                if (userDoc.exists()) {
                    val firstName = userDoc.getString("name") ?: ""
                    val lastName = userDoc.getString("lastName") ?: ""
                    val fullName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                        "$firstName $lastName"
                    } else if (firstName.isNotEmpty()) {
                        firstName
                    } else {
                        "Usuario"
                    }
                    userNames[userId] = fullName
                } else {
                    userNames[userId] = "Usuario"
                }
            }
            
            userNames
        } catch (e: Exception) {
            Log.e("AlertHistoryViewModel", "Error getting user names: ${e.message}")
            userIds.associateWith { "Usuario" }
        }
    }
    
    // Función optimizada para obtener múltiples nombres de grupos de una vez
    suspend fun getGroupNames(groupIds: List<String>): Map<String, String> {
        return try {
            val groupNames = mutableMapOf<String, String>()
            
            // Usar consultas individuales que son más compatibles
            for (groupId in groupIds) {
                val groupDoc = db.collection("groups").document(groupId).get().await()
                
                if (groupDoc.exists()) {
                    groupNames[groupId] = groupDoc.getString("name") ?: "Grupo"
                } else {
                    groupNames[groupId] = "Grupo"
                }
            }
            
            groupNames
        } catch (e: Exception) {
            Log.e("AlertHistoryViewModel", "Error getting group names: ${e.message}")
            groupIds.associateWith { "Grupo" }
        }
    }
    
    // Función optimizada para obtener múltiples nombres de tipos de alerta de una vez
    suspend fun getAlertTypeNames(alertTypeIds: List<String>): Map<String, String> {
        return try {
            val alertTypeNames = mutableMapOf<String, String>()
            
            // Usar consultas individuales que son más compatibles
            for (alertTypeId in alertTypeIds) {
                val alertTypeDoc = db.collection("alertTypes").document(alertTypeId).get().await()
                
                if (alertTypeDoc.exists()) {
                    alertTypeNames[alertTypeId] = alertTypeDoc.getString("name") ?: "Tipo de Alerta"
                } else {
                    alertTypeNames[alertTypeId] = "Tipo de Alerta"
                }
            }
            
            alertTypeNames
        } catch (e: Exception) {
            Log.e("AlertHistoryViewModel", "Error getting alert type names: ${e.message}")
            alertTypeIds.associateWith { "Tipo de Alerta" }
        }
    }
} 
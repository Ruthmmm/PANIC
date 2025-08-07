package com.example.panico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class AlertLocationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    suspend fun getUserName(userId: String): String {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val name = userDoc.getString("name") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                if (name.isNotEmpty() && lastName.isNotEmpty()) {
                    "$name $lastName"
                } else if (name.isNotEmpty()) {
                    name
                } else {
                    "Usuario"
                }
            } else {
                "Usuario"
            }
        } catch (e: Exception) {
            Log.e("AlertLocationViewModel", "Error getting user name: ${e.message}")
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
            Log.e("AlertLocationViewModel", "Error getting group name: ${e.message}")
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
            Log.e("AlertLocationViewModel", "Error getting alert type name: ${e.message}")
            "Tipo de Alerta"
        }
    }
} 
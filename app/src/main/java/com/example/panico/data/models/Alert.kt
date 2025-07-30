package com.example.panico.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Alert(
    val id: String = "",
    val active: Boolean = true,
    val groupId: String = "",
    val userId: String = "",
    val alertTypeId: String = "",
    val location: GeoPoint? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val message: String = "",
    val status: String = "active",
    val observation: String = ""
) 
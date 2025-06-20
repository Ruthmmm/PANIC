package com.example.panico.data.models

import com.google.firebase.Timestamp

data class AlertType(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val color: String = "",
    val icon: String = ""
) 
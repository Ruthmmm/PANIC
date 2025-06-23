package com.example.panico

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.panico.service.PowerButtonForegroundService
import com.example.panico.ui.screens.MapScreen
import com.example.panico.ui.theme.PANICOTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                startPowerButtonService()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                startPowerButtonService()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Se requieren permisos de ubicación para el funcionamiento completo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        
        // Verificar permisos
        if (hasRequiredPermissions()) {
            startPowerButtonService()
        } else {
            locationPermissionRequest.launch(requiredPermissions)
        }

        setContent {
            PANICOTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen()
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startPowerButtonService() {
        try {
            val intent = Intent(this, PowerButtonForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Error al iniciar el servicio de protección",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
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
import com.example.panico.ui.screens.LoginScreen
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.util.Log

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

    private lateinit var auth: FirebaseAuth
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        Log.d("AUTH_DEBUG", "onCreate: currentUser = ${auth.currentUser}")
        
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
                    AuthContent(auth)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!::auth.isInitialized) auth = FirebaseAuth.getInstance()
        if (authStateListener == null) {
            authStateListener = FirebaseAuth.AuthStateListener {
                Log.d("AUTH_DEBUG", "AuthStateListener: currentUser = ${auth.currentUser}")
                // Forzar recomposición
                setContent {
                    PANICOTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            AuthContent(auth)
                }
            }
        }
    }
}
        auth.addAuthStateListener(authStateListener!!)
    }

    override fun onStop() {
        super.onStop()
        authStateListener?.let { auth.removeAuthStateListener(it) }
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

@Composable
fun AuthContent(auth: FirebaseAuth) {
    // No usar remember ni mutableStateOf aquí
    if (auth.currentUser == null) {
        LoginScreen(onLoginSuccess = { /* No-op, AuthStateListener se encarga */ })
    } else {
        MapScreen()
    }
}
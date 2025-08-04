package com.example.panico.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.example.panico.data.models.Alert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth

class PowerButtonForegroundService : Service() {
    private val channelId = "power_button_channel"
    private val notificationId = 2
    private val db = FirebaseFirestore.getInstance()
    private val userId = "powerButtonUser"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    // Configuración para la detección de pulsaciones
    private val maxTimeBetweenPresses = 1000L // 1 segundo máximo entre pulsaciones
    private val requiredPresses = 5 // Número de pulsaciones requeridas
    private var timestamps = LongArray(requiredPresses) { 0L }
    private var pressCount = 0
    private var lastPressTime = 0L
    private var isProcessing = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("PowerButtonService", "Screen event received: ${intent.action}")
            
            if (intent.action == Intent.ACTION_SCREEN_OFF || intent.action == Intent.ACTION_SCREEN_ON) {
                val currentTime = SystemClock.elapsedRealtime()
                
                // Si ha pasado mucho tiempo desde la última pulsación, reiniciar contador
                if (currentTime - lastPressTime > maxTimeBetweenPresses && pressCount > 0) {
                    Log.d("PowerButtonService", "Resetting counter due to timeout")
                    pressCount = 0
                    timestamps = LongArray(requiredPresses) { 0L }
                }
                
                // Registrar la pulsación actual
                if (pressCount < requiredPresses) {
                    timestamps[pressCount] = currentTime
                    pressCount++
                    lastPressTime = currentTime
                    Log.d("PowerButtonService", "Press registered: $pressCount/$requiredPresses")
                }
                
                // Verificar si tenemos suficientes pulsaciones y están dentro del tiempo límite
                if (pressCount == requiredPresses && !isProcessing) {
                    Log.d("PowerButtonService", "Checking if press sequence is valid")
                    
                    // Verificar que todas las pulsaciones estén dentro del límite de tiempo
                    val isValid = (1 until requiredPresses).all { i ->
                        timestamps[i] - timestamps[i-1] <= maxTimeBetweenPresses
                    }
                    
                    if (isValid) {
                        Log.d("PowerButtonService", "Valid press sequence detected, sending alert")
                        isProcessing = true
                        serviceScope.launch {
                            try {
                                sendAlertAndVibrate()
                            } catch (e: Exception) {
                                Log.e("PowerButtonService", "Error sending alert: ${e.message}", e)
                            } finally {
                                // Reiniciar el contador después de procesar
                                pressCount = 0
                                timestamps = LongArray(requiredPresses) { 0L }
                                isProcessing = false
                                Log.d("PowerButtonService", "Alert processing completed, counter reset")
                            }
                        }
                    } else {
                        Log.d("PowerButtonService", "Invalid press sequence, resetting counter")
                        // Si las pulsaciones no son válidas, reiniciar contador
                        pressCount = 0
                        timestamps = LongArray(requiredPresses) { 0L }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("PowerButtonService", "Service onCreate called")
        
        try {
            createNotificationChannel()
            val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, channelId)
                    .setContentTitle("Protección de emergencia activa")
                    .setContentText("La app está escuchando pulsaciones del botón de encendido")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setOngoing(true) // Hacer la notificación persistente
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build()
            } else {
                Notification.Builder(this)
                    .setContentTitle("Protección de emergencia activa")
                    .setContentText("La app está escuchando pulsaciones del botón de encendido")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setOngoing(true) // Hacer la notificación persistente
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build()
            }
            startForeground(notificationId, notification)
            Log.d("PowerButtonService", "Foreground service started successfully")
            
            // Registrar receiver dinámico
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenReceiver, filter)
            Log.d("PowerButtonService", "Screen receiver registered")
            
        } catch (e: Exception) {
            Log.e("PowerButtonService", "Error in onCreate: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PowerButtonService", "Service onStartCommand called")
        // Retornar START_STICKY para que el sistema reinicie el servicio si es eliminado
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("PowerButtonService", "Service onDestroy called")
        try {
            unregisterReceiver(screenReceiver)
            Log.d("PowerButtonService", "Screen receiver unregistered")
        } catch (e: Exception) {
            Log.e("PowerButtonService", "Error unregistering receiver: ${e.message}", e)
        }
        serviceJob.cancel()
    }

    private suspend fun getCurrentUserGroupId(userId: String): String? {
        val userDoc = db.collection("users").document(userId).get().await()
        return userDoc.getString("groupId")
    }

    private suspend fun getAlertTypeIdByName(name: String): String? {
        val alertTypesSnapshot = db.collection("alertTypes").get().await()
        return alertTypesSnapshot.documents.firstOrNull { it.getString("name") == name }?.id
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

    private suspend fun sendAlertAndVibrate() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return
            val userId = currentUser.uid
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                vibrateStandard()
                return
            }
            val location = fusedLocationClient.lastLocation.await()
            val geoPoint = if (location != null) GeoPoint(location.latitude, location.longitude) else null
            // Desactivar alertas activas previas
            replaceActiveAlerts(userId)
            // Obtener groupId y alertTypeId
            val groupId = getCurrentUserGroupId(userId) ?: ""
            val alertTypeId = getAlertTypeIdByName("ALERTA CRÍTICA") ?: ""
            if (groupId.isBlank() || alertTypeId.isBlank()) {
                vibrateStandard()
                return
            }
            val alert = Alert(
                id = UUID.randomUUID().toString(),
                active = true,
                userId = userId,
                groupId = groupId,
                alertTypeId = alertTypeId,
                location = geoPoint,
                timestamp = Timestamp.now(),
                message = "",
                status = "active",
                observation = "alerta enviada desde botón de encendido"
            )
            db.collection("alerts").add(alert).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vibrateStandard()
    }

    private fun vibrateStandard() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1200)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Protección de emergencia", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
} 
package com.example.panico.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.example.panico.data.models.Alert
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import android.location.Location

class AlertForegroundService : Service() {
    private val channelId = "alert_channel"
    private val notificationId = 1
    private val db = FirebaseFirestore.getInstance()
    private val userId = "powerButtonUser" // Puedes personalizar esto
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Alerta enviada")
            .setContentText("Se ha enviado una alerta de emergencia")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        startForeground(notificationId, notification)
        serviceScope.launch { sendAlertAndVibrate() }
    }

    private suspend fun sendAlertAndVibrate() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // No hay permisos, vibra y termina
                vibrateStandard()
                stopSelf()
                return
            }
            val location = fusedLocationClient.lastLocation.await()
            val geoPoint = if (location != null) com.google.firebase.firestore.GeoPoint(location.latitude, location.longitude) else null
            val alert = Alert(
                id = UUID.randomUUID().toString(),
                active = true,
                userId = userId,
                location = geoPoint,
                timestamp = Timestamp.now(),
                message = "Alerta enviada por botón de encendido",
                status = "active"
            )
            db.collection("alerts").add(alert).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vibrateStandard()
        stopSelf()
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
            val channel = NotificationChannel(channelId, "Alertas de emergencia", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
} 
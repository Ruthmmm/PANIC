package com.example.panico.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.panico.service.PowerButtonForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Received intent: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, starting PowerButtonForegroundService")
            
            try {
                val serviceIntent = Intent(context, PowerButtonForegroundService::class.java)
                
                // Agregar flags para asegurar que el servicio se inicie correctamente
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d("BootReceiver", "Starting foreground service (API >= 26)")
                    context.startForegroundService(serviceIntent)
                } else {
                    Log.d("BootReceiver", "Starting regular service (API < 26)")
                    context.startService(serviceIntent)
                }
                
                Log.d("BootReceiver", "Service start intent sent successfully")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting service: ${e.message}", e)
            }
        }
    }
} 
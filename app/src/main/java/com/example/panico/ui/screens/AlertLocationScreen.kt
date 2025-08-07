package com.example.panico.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.panico.data.models.Alert
import com.example.panico.ui.viewmodel.AlertLocationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Circle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertLocationScreen(
    alert: Alert,
    onBackPressed: () -> Unit
) {
    // Manejar botón de atrás del teléfono
    BackHandler {
        onBackPressed()
    }
    
    val alertLocationViewModel: AlertLocationViewModel = viewModel()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    // Estados para nombres
    var userName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var alertTypeName by remember { mutableStateOf("") }
    
    // Estado para tiempo actualizado
    var currentTime by remember { mutableStateOf(Date()) }
    
    // Actualizar tiempo cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000) // Actualizar cada segundo
        }
    }
    
    // Cargar nombres inmediatamente
    LaunchedEffect(alert.id) {
        Log.d("AlertLocationScreen", "Cargando nombres para alerta: ${alert.id}")
        Log.d("AlertLocationScreen", "userId: ${alert.userId}")
        
        userName = alertLocationViewModel.getUserName(alert.userId)
        Log.d("AlertLocationScreen", "Nombre cargado: '$userName'")
        
        groupName = alertLocationViewModel.getGroupName(alert.groupId)
        alertTypeName = alertLocationViewModel.getAlertTypeName(alert.alertTypeId)
    }
    
    // Log para ver cuando cambia el nombre
    LaunchedEffect(userName) {
        Log.d("AlertLocationScreen", "userName actualizado: '$userName'")
    }
    
    // Calcular tiempo activo con horas, minutos y segundos
    val timeActive = remember(currentTime, alert.timestamp, alert.active) {
        val alertTime = alert.timestamp.toDate()
        
        // Si la alerta está activa, usar el tiempo actual
        // Si la alerta está terminada, usar el tiempo cuando se terminó (no se actualiza)
        val endTime = if (alert.active) currentTime else alertTime
        val diffInSeconds = (endTime.time - alertTime.time) / 1000
        val hours = diffInSeconds / 3600
        val minutes = (diffInSeconds % 3600) / 60
        val seconds = diffInSeconds % 60
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
    
    // Calcular tiempo activo para mostrar en la tarjeta
    val timeActiveText = remember(alert.timestamp) {
        val now = Date()
        val alertTime = alert.timestamp.toDate()
        val diffInMinutes = (now.time - alertTime.time) / (1000 * 60)
        when {
            diffInMinutes < 1 -> "Menos de 1 minuto"
            diffInMinutes < 60 -> "Hace $diffInMinutes minutos"
            else -> {
                val hours = diffInMinutes / 60
                val minutes = diffInMinutes % 60
                "Hace $hours horas $minutes minutos"
            }
        }
    }
    
    // Configurar mapa
    val alertLocation = alert.location?.let { 
        LatLng(it.latitude, it.longitude) 
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(alertLocation ?: LatLng(0.0, 0.0), 15f)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (userName.isNotEmpty() && userName != "Usuario" && userName != "Cargando...") userName else "Ubicación de Alerta",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Mensaje flotante con el tipo de alerta
            if (alertTypeName.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        alertTypeName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            
            // Mapa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                if (alertLocation != null) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true
                        )
                    ) {
                        // Marcador de la ubicación de la alerta
                        Marker(
                            state = com.google.maps.android.compose.rememberMarkerState(position = alertLocation),
                            title = "Ubicación de la alerta",
                            snippet = "Enviada por $userName"
                        )
                        
                        // Círculo parpadeante alrededor de la ubicación
                        AlertLocationBlinkingCircle(
                            center = alertLocation,
                            radius = 100.0
                        )
                    }
                    
                    // Tiempo activo sobre el mapa
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeActive,
                                fontSize = 16.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ubicación no disponible",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertLocationDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Text(
            text = value,
            fontSize = 12.sp
        )
    }
}

// Círculo rojo parpadeante en el mapa
@Composable
fun AlertLocationBlinkingCircle(center: LatLng?, radius: Double) {
    if (center == null) return
    val infiniteTransition = rememberInfiniteTransition(label = "blinking-circle")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.3f at 0
                0.6f at 450
                0.3f at 900
            },
            repeatMode = RepeatMode.Restart
        ), label = "circle-alpha"
    )
    val color1 = Color(0x88FF0000) // Rojo con transparencia
    val color2 = Color(0x44FF0000) // Rojo más transparente
    val color = lerp(color1, color2, alpha)
    // Animar el radio entre 50 y 100 metros
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.5f at 0
                1.0f at 450
                0.5f at 900
            },
            repeatMode = RepeatMode.Restart
        ), label = "circle-scale"
    )
    val animatedRadius = radius * scale
    Circle(
        center = center,
        radius = animatedRadius,
        fillColor = color,
        strokeColor = color,
        strokeWidth = 0f
    )
} 
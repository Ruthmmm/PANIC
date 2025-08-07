package com.example.panico.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.panico.data.models.Alert
import com.example.panico.ui.viewmodel.ActiveAlertsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlinx.coroutines.delay
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveAlertsScreen(
    onBackPressed: () -> Unit,
    onViewLocation: (Alert) -> Unit,
    onAlertTerminated: () -> Unit = {}
) {
    // Manejar botón de atrás del teléfono
    BackHandler {
        onBackPressed()
    }
    
    val activeAlertsViewModel: ActiveAlertsViewModel = viewModel()
    val activeAlerts by activeAlertsViewModel.activeAlerts.collectAsState()
    val isLoading by activeAlertsViewModel.isLoading.collectAsState()
    
    // Estado para tiempo actualizado
    var currentTime by remember { mutableStateOf(Date()) }
    
    // Actualizar tiempo cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000) // Actualizar cada segundo
        }
    }
    
    // Estados para confirmaciones
    var showTerminateConfirmation by remember { mutableStateOf(false) }
    var alertToTerminate by remember { mutableStateOf<Alert?>(null) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    
    // Cargar alertas activas al iniciar
    LaunchedEffect(Unit) {
        activeAlertsViewModel.loadActiveAlerts()
    }
    
    // Función para calcular tiempo activo
    fun calculateTimeActive(timestamp: com.google.firebase.Timestamp): String {
        val alertTime = timestamp.toDate()
        val diffInSeconds = (currentTime.time - alertTime.time) / 1000
        val hours = diffInSeconds / 3600
        val minutes = (diffInSeconds % 3600) / 60
        val seconds = diffInSeconds % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alertas Activas") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (activeAlerts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay alertas activas",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Todas las alertas han sido terminadas",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeAlerts) { alert ->
                        ActiveAlertItem(
                            alert = alert,
                            onViewLocation = { onViewLocation(alert) },
                            onTerminate = { 
                                alertToTerminate = alert
                                showTerminateConfirmation = true
                            },
                            calculateTimeActive = ::calculateTimeActive
                        )
                    }
                }
            }
        }
    }
    
    // Diálogo de confirmación para terminar alerta
    if (showTerminateConfirmation) {
        AlertDialog(
            onDismissRequest = { /* No hacer nada para evitar cierre */ },
            title = { Text("¿Terminar alerta?") },
            text = { Text("¿Estás seguro de terminar la alerta?") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { showTerminateConfirmation = false }) { 
                        Text("No") 
                    }
                    Button(onClick = {
                        showTerminateConfirmation = false
                        showCommentDialog = true
                    }) { 
                        Text("Sí") 
                    }
                }
            },
            dismissButton = { /* Vacío porque los botones están en confirmButton */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
    
    // Diálogo para comentario opcional
    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { /* No hacer nada para evitar cierre */ },
            title = { Text("Comentario opcional") },
            text = {
                Column {
                    Text("¿Deseas dejar un comentario sobre la alerta? (opcional)")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Escribe tu comentario...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        showCommentDialog = false
                        alertToTerminate?.let { alert ->
                            activeAlertsViewModel.terminateAlert(alert.id, "")
                        }
                        commentText = ""
                        alertToTerminate = null
                        onAlertTerminated()
                    }) { 
                        Text("Cancelar") 
                    }
                    Button(onClick = {
                        showCommentDialog = false
                        alertToTerminate?.let { alert ->
                            activeAlertsViewModel.terminateAlert(alert.id, commentText)
                        }
                        commentText = ""
                        alertToTerminate = null
                        onAlertTerminated()
                    }) { 
                        Text("Aceptar") 
                    }
                }
            },
            dismissButton = { /* Vacío porque los botones están en confirmButton */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}

@Composable
fun ActiveAlertItem(
    alert: Alert,
    onViewLocation: () -> Unit,
    onTerminate: () -> Unit,
    calculateTimeActive: (com.google.firebase.Timestamp) -> String
) {
    val activeAlertsViewModel: ActiveAlertsViewModel = viewModel()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    // Estados para nombres
    var userName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var alertTypeName by remember { mutableStateOf("") }
    
    // Cargar nombres inmediatamente
    LaunchedEffect(alert.id) {
        Log.d("ActiveAlertsScreen", "Cargando nombres para alerta: ${alert.id}")
        Log.d("ActiveAlertsScreen", "userId: ${alert.userId}")
        
        userName = activeAlertsViewModel.getUserName(alert.userId)
        Log.d("ActiveAlertsScreen", "Nombre cargado: '$userName'")
        
        groupName = activeAlertsViewModel.getGroupName(alert.groupId)
        alertTypeName = activeAlertsViewModel.getAlertTypeName(alert.alertTypeId)
    }
    
    // Log para ver cuando cambia el nombre
    LaunchedEffect(userName) {
        Log.d("ActiveAlertsScreen", "userName actualizado: '$userName'")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con información básica
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (userName.isNotEmpty()) userName else "Cargando...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = dateFormat.format(alert.timestamp.toDate()),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                // Indicador de tiempo activo en formato HH:MM:SS
                Row(
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
                        text = calculateTimeActive(alert.timestamp),
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Detalles de la alerta
            ActiveAlertDetailRow(
                icon = Icons.Filled.Group,
                label = "Grupo",
                value = if (groupName.isNotEmpty()) groupName else "Cargando..."
            )
            
            ActiveAlertDetailRow(
                icon = Icons.Filled.Notifications,
                label = "Tipo de Alerta",
                value = if (alertTypeName.isNotEmpty()) alertTypeName else "Cargando..."
            )
            
            if (alert.message.isNotEmpty()) {
                ActiveAlertDetailRow(
                    icon = Icons.Filled.Person,
                    label = "Mensaje",
                    value = alert.message
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onViewLocation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver ubicación")
                }
                
                Button(
                    onClick = onTerminate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Terminar")
                }
            }
        }
    }
}

@Composable
fun ActiveAlertDetailRow(
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
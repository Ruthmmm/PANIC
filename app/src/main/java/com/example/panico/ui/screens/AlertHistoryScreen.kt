package com.example.panico.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.panico.data.models.Alert
import com.example.panico.ui.viewmodel.AlertHistoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.util.Date
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertHistoryScreen(
    onBackPressed: () -> Unit
) {
    BackHandler {
        onBackPressed()
    }

    val alertHistoryViewModel: AlertHistoryViewModel = viewModel()
    val alerts by alertHistoryViewModel.alerts.collectAsState()
    val isLoading by alertHistoryViewModel.isLoading.collectAsState()

    // Estado para tiempo actualizado
    var currentTime by remember { mutableStateOf(Date()) }

    // Actualizar tiempo cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000) // Actualizar cada segundo
        }
    }

    // Estados para diálogos
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedAlertForDelete by remember { mutableStateOf<Alert?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedAlertForEdit by remember { mutableStateOf<Alert?>(null) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showSaveConfirmationDialog by remember { mutableStateOf(false) }

    // Estados para edición
    var editMessage by remember { mutableStateOf("") }
    var editObservation by remember { mutableStateOf("") }
    var editUserId by remember { mutableStateOf("") }
    var editGroupId by remember { mutableStateOf("") }
    var editAlertTypeId by remember { mutableStateOf("") }

    // Estados para dropdowns
    var showUserDropdown by remember { mutableStateOf(false) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var showAlertTypeDropdown by remember { mutableStateOf(false) }
    var userSearchText by remember { mutableStateOf("") }
    var groupSearchText by remember { mutableStateOf("") }
    var alertTypeSearchText by remember { mutableStateOf("") }

    // Estados para nombres actuales
    var currentUserName by remember { mutableStateOf("") }
    var currentGroupName by remember { mutableStateOf("") }
    var currentAlertTypeName by remember { mutableStateOf("") }

    // Estados para pull-to-refresh
    var isRefreshing by remember { mutableStateOf(false) }

    // Focus requesters
    val userFocusRequester = remember { FocusRequester() }
    val groupFocusRequester = remember { FocusRequester() }
    val alertTypeFocusRequester = remember { FocusRequester() }

    // Estados para datos de dropdowns
    val users by alertHistoryViewModel.users.collectAsState()
    val groups by alertHistoryViewModel.groups.collectAsState()
    val alertTypes by alertHistoryViewModel.alertTypes.collectAsState()

    // Usuarios filtrados por grupo seleccionado
    val filteredUsers = users

    var expandedAlertId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        alertHistoryViewModel.loadAllAlerts()
        alertHistoryViewModel.loadUsers()
        alertHistoryViewModel.loadGroups()
        alertHistoryViewModel.loadAlertTypes()
    }

    // Recargar usuarios cuando cambia el grupo seleccionado
    LaunchedEffect(editGroupId) {
        if (editGroupId.isNotEmpty()) {
            alertHistoryViewModel.loadUsers(editGroupId)
        }
    }

    // Función para refrescar datos
    fun refreshData() {
        isRefreshing = true
        CoroutineScope(Dispatchers.Main).launch {
            alertHistoryViewModel.loadAllAlerts()
            alertHistoryViewModel.loadUsers()
            alertHistoryViewModel.loadGroups()
            alertHistoryViewModel.loadAlertTypes()
            delay(1000) // Simular tiempo de carga
            isRefreshing = false
        }
    }

    // Función para calcular tiempo activo
    fun calculateTimeActive(
        timestamp: com.google.firebase.Timestamp,
        isActive: Boolean,
        endTimestamp: com.google.firebase.Timestamp? = null
    ): String {
        val alertTime = timestamp.toDate()

        val endTime = when {
            isActive -> currentTime
            endTimestamp != null -> endTimestamp.toDate()
            else -> currentTime // Fallback para alertas terminadas sin endTimestamp
        }

        val diffInSeconds = (endTime.time - alertTime.time) / 1000
        val hours = diffInSeconds / 3600
        val minutes = (diffInSeconds % 3600) / 60
        val seconds = diffInSeconds % 60
        return "${hours.toString().padStart(2, '0')}:${
            minutes.toString().padStart(2, '0')
        }:${seconds.toString().padStart(2, '0')}"
    }

    // Función para cerrar otros dropdowns
    fun closeOtherDropdowns(current: String) {
        when (current) {
            "user" -> {
                showGroupDropdown = false
                showAlertTypeDropdown = false
            }

            "group" -> {
                showUserDropdown = false
                showAlertTypeDropdown = false
            }

            "alertType" -> {
                showUserDropdown = false
                showGroupDropdown = false
            }
        }
    }

    // Función para abrir diálogo de edición
    fun openEditDialog(alert: Alert) {
        selectedAlertForEdit = alert
        editMessage = alert.message
        editObservation = alert.observation
        editUserId = alert.userId
        editGroupId = alert.groupId
        editAlertTypeId = alert.alertTypeId

        // Cargar nombres actuales
        CoroutineScope(Dispatchers.Main).launch {
            try {
                currentUserName = alertHistoryViewModel.getUserName(alert.userId)
                currentGroupName = alertHistoryViewModel.getGroupName(alert.groupId)
                currentAlertTypeName = alertHistoryViewModel.getAlertTypeName(alert.alertTypeId)
            } catch (e: Exception) {
                // Manejar errores de carga de nombres
                currentUserName = "Usuario"
                currentGroupName = "Grupo"
                currentAlertTypeName = "Tipo de Alerta"
            }
        }

        // Cargar usuarios del grupo seleccionado
        alertHistoryViewModel.loadUsers(alert.groupId)

        showEditDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Alertas") },
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
            } else {
                if (alerts.isEmpty()) {
                    // Mensaje centrado cuando no hay alertas
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay alertas en el historial",
                                fontSize = 18.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Las alertas aparecerán aquí una vez que se envíen",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Lista de alertas cuando hay datos
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alerts) { alert ->
                            AlertHistoryItem(
                                alert = alert,
                                isExpanded = expandedAlertId == alert.id,
                                onToggleExpansion = { alertId ->
                                    expandedAlertId =
                                        if (expandedAlertId == alertId) null else alertId
                                },
                                onEdit = { alertId ->
                                    openEditDialog(alerts.find { it.id == alertId }!!)
                                },
                                onDelete = { alertId ->
                                    selectedAlertForDelete = alerts.find { it.id == alertId }
                                    showDeleteDialog = true
                                },
                                calculateTimeActive = { timestamp, isActive, endTimestamp ->
                                    calculateTimeActive(timestamp, isActive, endTimestamp)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta alerta? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedAlertForDelete?.let { alert ->
                            alertHistoryViewModel.deleteAlert(alert.id)
                        }
                        showDeleteDialog = false
                        selectedAlertForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

         // Diálogo de edición
     if (showEditDialog) {
         Dialog(
             onDismissRequest = { 
                 showEditDialog = false
                 // Limpiar estados de edición al cerrar
                 selectedAlertForEdit = null
                 editMessage = ""
                 editObservation = ""
                 editUserId = ""
                 editGroupId = ""
                 editAlertTypeId = ""
                 currentUserName = ""
                 currentGroupName = ""
                 currentAlertTypeName = ""
             },
             properties = DialogProperties(
                 dismissOnBackPress = false,
                 dismissOnClickOutside = false
             )
         ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Card principal simple - más ancha y menos alta
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.98f) // Más ancha (98% del ancho)
                        .padding(8.dp) // Menos padding
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp) // Menos padding interno
                    ) {
                        // Header simple
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp), // Menos espacio
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Editar Alerta",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                                                         IconButton(
                                 onClick = { 
                                     showEditDialog = false
                                     // Limpiar estados de edición al cerrar
                                     selectedAlertForEdit = null
                                     editMessage = ""
                                     editObservation = ""
                                     editUserId = ""
                                     editGroupId = ""
                                     editAlertTypeId = ""
                                     currentUserName = ""
                                     currentGroupName = ""
                                     currentAlertTypeName = ""
                                 },
                                 modifier = Modifier.size(40.dp)
                             ) {
                                 Icon(
                                     imageVector = Icons.Filled.Cancel,
                                     contentDescription = "Cerrar",
                                     tint = MaterialTheme.colorScheme.onSurface
                                 )
                             }
                        }

                        // Grupo
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Grupo",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp) // Menos espacio
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (showGroupDropdown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        try {
                                            closeOtherDropdowns("group")
                                            showGroupDropdown = !showGroupDropdown
                                            groupFocusRequester.requestFocus()
                                        } catch (e: Exception) {
                                            // Manejar error sin cerrar la app
                                        }
                                    }
                                    .padding(14.dp) // Menos padding
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (currentGroupName.isNotEmpty()) currentGroupName else "Seleccionar grupo",
                                        color = if (currentGroupName.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = if (showGroupDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp)) // Menos espacio

                        // Usuario
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Usuario",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp) // Menos espacio
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (showUserDropdown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        try {
                                            closeOtherDropdowns("user")
                                            showUserDropdown = !showUserDropdown
                                            userFocusRequester.requestFocus()
                                        } catch (e: Exception) {
                                            // Manejar error sin cerrar la app
                                        }
                                    }
                                    .padding(14.dp) // Menos padding
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (currentUserName.isNotEmpty()) currentUserName else "Seleccionar usuario",
                                        color = if (currentUserName.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = if (showUserDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp)) // Menos espacio

                        // Tipo de Alerta
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tipo de Alerta",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp) // Menos espacio
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (showAlertTypeDropdown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        try {
                                            closeOtherDropdowns("alertType")
                                            showAlertTypeDropdown = !showAlertTypeDropdown
                                            alertTypeFocusRequester.requestFocus()
                                        } catch (e: Exception) {
                                            // Manejar error sin cerrar la app
                                        }
                                    }
                                    .padding(14.dp) // Menos padding
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (currentAlertTypeName.isNotEmpty()) currentAlertTypeName else "Seleccionar tipo de alerta",
                                        color = if (currentAlertTypeName.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = if (showAlertTypeDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp)) // Menos espacio

                        // Mensaje
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Mensaje",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp) // Menos espacio
                            )
                            OutlinedTextField(
                                value = editMessage,
                                onValueChange = { editMessage = it },
                                placeholder = { Text("Mensaje de la alerta") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp)) // Menos espacio

                        // Observación
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Observación",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp) // Menos espacio
                            )
                            OutlinedTextField(
                                value = editObservation,
                                onValueChange = { editObservation = it },
                                placeholder = { Text("Observación de la alerta") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp)) // Menos espacio

                        // Botones simples
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                                         Button(
                                 onClick = { 
                                     showEditDialog = false
                                     // Limpiar estados de edición al cancelar
                                     selectedAlertForEdit = null
                                     editMessage = ""
                                     editObservation = ""
                                     editUserId = ""
                                     editGroupId = ""
                                     editAlertTypeId = ""
                                     currentUserName = ""
                                     currentGroupName = ""
                                     currentAlertTypeName = ""
                                 },
                                 modifier = Modifier.weight(1f),
                                 colors = ButtonDefaults.buttonColors(
                                     containerColor = MaterialTheme.colorScheme.surfaceVariant
                                 ),
                                 shape = RoundedCornerShape(8.dp)
                             ) {
                                 Text(
                                     text = "Cancelar",
                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                     fontSize = 14.sp,
                                     modifier = Modifier.padding(vertical = 8.dp)
                                 )
                             }

                            Button(
                                onClick = {
                                    showSaveConfirmationDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Guardar",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Dropdowns en ventanas flotantes separadas
                // Dropdown para Grupo
                if (showGroupDropdown) {
                    Dialog(
                        onDismissRequest = { showGroupDropdown = false },
                        properties = DialogProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Header simple
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Seleccionar Grupo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    IconButton(
                                        onClick = { showGroupDropdown = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Cancel,
                                            contentDescription = "Cerrar",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Campo de búsqueda
                                OutlinedTextField(
                                    value = groupSearchText,
                                    onValueChange = { groupSearchText = it },
                                    placeholder = { Text("Buscar grupo...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(groupFocusRequester),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Lista de grupos
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    val filteredGroups = groups.filter { group ->
                                        group.second.contains(groupSearchText, ignoreCase = true)
                                    }

                                    items(filteredGroups) { group ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    try {
                                                        editGroupId = group.first
                                                        currentGroupName = group.second
                                                        showGroupDropdown = false
                                                        groupSearchText = ""
                                                        // Limpiar usuario seleccionado cuando cambia el grupo
                                                        editUserId = ""
                                                        currentUserName = ""
                                                        // Recargar usuarios del nuevo grupo
                                                        alertHistoryViewModel.loadUsers(group.first)
                                                    } catch (e: Exception) {
                                                        // Manejar error sin cerrar la app
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 16.dp)
                                                .background(
                                                    if (editGroupId == group.first)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = group.second,
                                                modifier = Modifier.weight(1f),
                                                fontSize = 14.sp,
                                                color = if (editGroupId == group.first)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (editGroupId == group.first)
                                                    FontWeight.Medium
                                                else
                                                    FontWeight.Normal
                                            )
                                            if (editGroupId == group.first) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dropdown para Usuario
                if (showUserDropdown) {
                    Dialog(
                        onDismissRequest = { showUserDropdown = false },
                        properties = DialogProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Header simple
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Seleccionar Usuario",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    IconButton(
                                        onClick = { showUserDropdown = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Cancel,
                                            contentDescription = "Cerrar",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Campo de búsqueda
                                OutlinedTextField(
                                    value = userSearchText,
                                    onValueChange = { userSearchText = it },
                                    placeholder = { Text("Buscar usuario...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(userFocusRequester),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Lista de usuarios
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    val filteredUsersBySearch = filteredUsers.filter { user ->
                                        user.second.contains(userSearchText, ignoreCase = true)
                                    }

                                    items(filteredUsersBySearch) { user ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    try {
                                                        editUserId = user.first
                                                        currentUserName = user.second
                                                        showUserDropdown = false
                                                        userSearchText = ""
                                                    } catch (e: Exception) {
                                                        // Manejar error sin cerrar la app
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 16.dp)
                                                .background(
                                                    if (editUserId == user.first)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = user.second,
                                                modifier = Modifier.weight(1f),
                                                fontSize = 14.sp,
                                                color = if (editUserId == user.first)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (editUserId == user.first)
                                                    FontWeight.Medium
                                                else
                                                    FontWeight.Normal
                                            )
                                            if (editUserId == user.first) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dropdown para Tipo de Alerta
                if (showAlertTypeDropdown) {
                    Dialog(
                        onDismissRequest = { showAlertTypeDropdown = false },
                        properties = DialogProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Header simple
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Seleccionar Tipo de Alerta",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    IconButton(
                                        onClick = { showAlertTypeDropdown = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Cancel,
                                            contentDescription = "Cerrar",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Campo de búsqueda
                                OutlinedTextField(
                                    value = alertTypeSearchText,
                                    onValueChange = { alertTypeSearchText = it },
                                    placeholder = { Text("Buscar tipo de alerta...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(alertTypeFocusRequester),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Lista de tipos de alerta
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    val filteredAlertTypes = alertTypes.filter { alertType ->
                                        alertType.second.contains(
                                            alertTypeSearchText,
                                            ignoreCase = true
                                        )
                                    }

                                    items(filteredAlertTypes) { alertType ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    try {
                                                        editAlertTypeId = alertType.first
                                                        currentAlertTypeName = alertType.second
                                                        showAlertTypeDropdown = false
                                                        alertTypeSearchText = ""
                                                    } catch (e: Exception) {
                                                        // Manejar error sin cerrar la app
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 16.dp)
                                                .background(
                                                    if (editAlertTypeId == alertType.first)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = alertType.second,
                                                modifier = Modifier.weight(1f),
                                                fontSize = 14.sp,
                                                color = if (editAlertTypeId == alertType.first)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (editAlertTypeId == alertType.first)
                                                    FontWeight.Medium
                                                else
                                                    FontWeight.Normal
                                            )
                                            if (editAlertTypeId == alertType.first) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de confirmación para guardar
        if (showSaveConfirmationDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSaveConfirmationDialog = false
                },
                title = { Text("Confirmar Guardado") },
                text = { Text("¿Estás seguro de que quieres guardar los cambios realizados?") },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                selectedAlertForEdit?.let { alert ->
                                    val updates = mapOf(
                                        "userId" to editUserId,
                                        "groupId" to editGroupId,
                                        "alertTypeId" to editAlertTypeId,
                                        "message" to editMessage,
                                        "observation" to editObservation
                                    )
                                    alertHistoryViewModel.updateAlert(alert.id, updates)
                                }
                                showSaveConfirmationDialog = false
                                showEditDialog = false
                                // Limpiar estados de edición
                                selectedAlertForEdit = null
                                editMessage = ""
                                editObservation = ""
                                editUserId = ""
                                editGroupId = ""
                                editAlertTypeId = ""
                                currentUserName = ""
                                currentGroupName = ""
                                currentAlertTypeName = ""
                            } catch (e: Exception) {
                                // Manejar el error sin cerrar la aplicación
                                showSaveConfirmationDialog = false
                                showEditDialog = false
                                // Limpiar estados de edición
                                selectedAlertForEdit = null
                                editMessage = ""
                                editObservation = ""
                                editUserId = ""
                                editGroupId = ""
                                editAlertTypeId = ""
                                currentUserName = ""
                                currentGroupName = ""
                                currentAlertTypeName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                                         ) {
                         Text(
                             text = "Confirmar",
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     }
                },
                                 dismissButton = {
                     Button(
                         onClick = {
                             showSaveConfirmationDialog = false
                         },
                         colors = ButtonDefaults.buttonColors(
                             containerColor = MaterialTheme.colorScheme.surfaceVariant
                         )
                     ) {
                         Text(
                             text = "Cancelar",
                             color = Color.Black
                         )
                     }
                 }
            )
        }


    }

}

@Composable
fun AlertHistoryItem(
    alert: Alert,
    isExpanded: Boolean,
    onToggleExpansion: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    calculateTimeActive: (com.google.firebase.Timestamp, Boolean, com.google.firebase.Timestamp?) -> String
) {
    val alertHistoryViewModel: AlertHistoryViewModel = viewModel()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Estados para nombres
    var userName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var alertTypeName by remember { mutableStateOf("") }

    // Cargar nombres inmediatamente
    LaunchedEffect(alert.id) {
        try {
            userName = alertHistoryViewModel.getUserName(alert.userId)
            groupName = alertHistoryViewModel.getGroupName(alert.groupId)
            alertTypeName = alertHistoryViewModel.getAlertTypeName(alert.alertTypeId)
        } catch (e: Exception) {
            // Manejar errores de carga de nombres
            userName = "Usuario"
            groupName = "Grupo"
            alertTypeName = "Tipo de Alerta"
        }
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
                    // Mostrar tiempo activo debajo de la fecha para alertas activas
                    if (alert.active) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tiempo activo: ${
                                    calculateTimeActive(
                                        alert.timestamp,
                                        alert.active,
                                        alert.endTimestamp
                                    )
                                }",
                                fontSize = 11.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Botón de expandir/contraer
                IconButton(
                    onClick = { onToggleExpansion(alert.id) }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Contraer" else "Expandir"
                    )
                }
            }

            // Contenido expandido
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Grupo de la alerta
                AlertDetailRow(
                    icon = Icons.Filled.Group,
                    label = "Grupo",
                    value = if (groupName.isNotEmpty()) groupName else "Cargando..."
                )

                // Tipo de alerta
                AlertDetailRow(
                    icon = Icons.Filled.Notifications,
                    label = "Tipo de Alerta",
                    value = if (alertTypeName.isNotEmpty()) alertTypeName else "Cargando..."
                )

                // Estado de la alerta
                AlertDetailRow(
                    icon = getStatusIcon(alert.status),
                    label = "Estado",
                    value = getStatusText(alert.status)
                )

                // Información de tiempo de alerta
                AlertDetailRow(
                    icon = Icons.Filled.Timer,
                    label = if (alert.active) "Tiempo Activo" else "Duración",
                    value = calculateTimeActive(
                        alert.timestamp,
                        alert.active,
                        alert.endTimestamp
                    )
                )

                // Mensaje (si existe)
                if (alert.message.isNotEmpty()) {
                    AlertDetailRow(
                        icon = Icons.Filled.Message,
                        label = "Mensaje",
                        value = alert.message
                    )
                }

                // Observación (si existe)
                if (alert.observation.isNotEmpty()) {
                    AlertDetailRow(
                        icon = Icons.Filled.Info,
                        label = "Observación",
                        value = alert.observation
                    )
                }

                // Ubicación de la alerta
                if (alert.location != null) {
                    AlertDetailRow(
                        icon = Icons.Filled.LocationOn,
                        label = "Ubicación",
                        value = "Lat: ${alert.location.latitude}, Lng: ${alert.location.longitude}"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onEdit(alert.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }

                    Button(
                        onClick = { onDelete(alert.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
fun AlertDetailRow(
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

@Composable
fun getStatusIcon(status: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        "active" -> Icons.Filled.CheckCircle
        "resolved" -> Icons.Filled.Cancel
        "canceled" -> Icons.Filled.Cancel
        else -> Icons.Filled.Pending
    }
}

@Composable
fun getStatusText(status: String): String {
    return when (status) {
        "active" -> "Activa"
        "resolved" -> "Resuelta"
        "canceled" -> "Cancelada"
        else -> "Pendiente"
    }
}

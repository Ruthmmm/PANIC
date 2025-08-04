package com.example.panico.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertHistoryScreen(
    onBackPressed: () -> Unit
) {
    // Manejar botón de atrás del teléfono
    BackHandler {
        onBackPressed()
    }

    val alertHistoryViewModel: AlertHistoryViewModel = viewModel()
    val alerts by alertHistoryViewModel.alerts.collectAsState()
    val isLoading by alertHistoryViewModel.isLoading.collectAsState()
    val users by alertHistoryViewModel.users.collectAsState()
    val groups by alertHistoryViewModel.groups.collectAsState()
    val alertTypes by alertHistoryViewModel.alertTypes.collectAsState()

    // Estado para controlar qué alerta está expandida
    var expandedAlertId by remember { mutableStateOf<String?>(null) }

    // Estados para edición y confirmaciones
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedAlertForEdit by remember { mutableStateOf<Alert?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var alertToDelete by remember { mutableStateOf<String?>(null) }

    // Estados para campos editables
    var editMessage by remember { mutableStateOf("") }
    var editObservation by remember { mutableStateOf("") }
    var editUserId by remember { mutableStateOf("") }
    var editGroupId by remember { mutableStateOf("") }
    var editAlertTypeId by remember { mutableStateOf("") }
    var showSaveConfirmation by remember { mutableStateOf(false) }

    // Estados para dropdowns expandidos
    var showUserDropdown by remember { mutableStateOf(false) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var showAlertTypeDropdown by remember { mutableStateOf(false) }

    // Estados para el nombre del grupo actual
    var currentGroupName by remember { mutableStateOf("") }

    // Estados para búsqueda de texto
    var groupSearchText by remember { mutableStateOf("") }
    var userSearchText by remember { mutableStateOf("") }
    var alertTypeSearchText by remember { mutableStateOf("") }

    // Focus requesters para posicionar el cursor
    val groupFocusRequester = remember { FocusRequester() }
    val userFocusRequester = remember { FocusRequester() }
    val alertTypeFocusRequester = remember { FocusRequester() }

    // Función para cerrar todos los dropdowns excepto el especificado
    fun closeOtherDropdowns(keepOpen: String) {
        when (keepOpen) {
            "group" -> {
                showUserDropdown = false
                showAlertTypeDropdown = false
            }

            "user" -> {
                showGroupDropdown = false
                showAlertTypeDropdown = false
            }

            "alertType" -> {
                showGroupDropdown = false
                showUserDropdown = false
            }
        }
    }

    // Función para abrir edición
    fun openEditDialog(alert: Alert) {
        selectedAlertForEdit = alert
        editMessage = alert.message
        editObservation = alert.observation
        editUserId = alert.userId
        editGroupId = alert.groupId
        editAlertTypeId = alert.alertTypeId
        showUserDropdown = false
        showGroupDropdown = false
        showAlertTypeDropdown = false
        // Inicializar textos de búsqueda
        groupSearchText = ""
        userSearchText = ""
        alertTypeSearchText = ""
        // Cargar usuarios del grupo actual de la alerta
        alertHistoryViewModel.loadUsers(alert.groupId)
        showEditDialog = true
    }

    // Cargar alertas al iniciar
    LaunchedEffect(Unit) {
        alertHistoryViewModel.loadAllAlerts()
        alertHistoryViewModel.loadGroups()
        alertHistoryViewModel.loadAlertTypes()
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
                                expandedAlertId = if (expandedAlertId == alertId) null else alertId
                            },
                            onEdit = { alertId ->
                                openEditDialog(alerts.find { it.id == alertId }!!)
                            },
                            onDelete = { alertId ->
                                alertToDelete = alertId
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta alerta? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        alertToDelete?.let { alertId ->
                            alertHistoryViewModel.deleteAlert(alertId)
                        }
                        showDeleteConfirmation = false
                        alertToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        alertToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de edición
    if (showEditDialog && selectedAlertForEdit != null) {
        // Cargar el nombre del grupo cuando se abre el diálogo
        LaunchedEffect(showEditDialog) {
            if (showEditDialog && selectedAlertForEdit != null) {
                android.util.Log.d(
                    "AlertHistoryScreen",
                    "LaunchedEffect triggered, groupId: ${selectedAlertForEdit!!.groupId}"
                )
                try {
                    currentGroupName =
                        alertHistoryViewModel.getGroupName(selectedAlertForEdit!!.groupId)
                    android.util.Log.d("AlertHistoryScreen", "Loaded group name: $currentGroupName")
                } catch (e: Exception) {
                    android.util.Log.e(
                        "AlertHistoryScreen",
                        "Error loading group name: ${e.message}"
                    )
                    currentGroupName = "Error: ${e.message}"
                }
            }
        }

        // Cargar el nombre del grupo cuando cambia editGroupId
        LaunchedEffect(editGroupId) {
            if (editGroupId.isNotEmpty()) {
                currentGroupName = alertHistoryViewModel.getGroupName(editGroupId)
            }
        }

        Dialog(
            onDismissRequest = {
                showEditDialog = false
                selectedAlertForEdit = null
                // Resetear campos
                editMessage = ""
                editObservation = ""
                editUserId = ""
                editGroupId = ""
                editAlertTypeId = ""
                showUserDropdown = false
                showGroupDropdown = false
                showAlertTypeDropdown = false
            }
        ) {
            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Título
                        Text(
                            text = "Editar Alerta",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Combobox de grupo
                        Text(
                            text = "Grupo:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box {
                            OutlinedTextField(
                                value = if (showGroupDropdown) groupSearchText else {
                                    if (editGroupId.isEmpty()) {
                                        currentGroupName.ifEmpty { "Seleccionar grupo" }
                                    } else {
                                        groups.find { it.first == editGroupId }?.second
                                            ?: currentGroupName.ifEmpty { "Seleccionar grupo" }
                                    }
                                },
                                onValueChange = { 
                                    groupSearchText = it
                                    if (it.isNotEmpty()) {
                                        closeOtherDropdowns("group")
                                        showGroupDropdown = true
                                    } else {
                                        showGroupDropdown = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(groupFocusRequester),
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        showGroupDropdown = !showGroupDropdown
                                        closeOtherDropdowns("group")
                                        if (!showGroupDropdown) {
                                            groupSearchText = ""
                                        }
                                    }) {
                                        Icon(
                                            if (showGroupDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = "Expandir"
                                        )
                                    }
                                },
                                placeholder = { Text("Buscar grupo...") },
                                readOnly = !showGroupDropdown
                            )

                            // Dropdown de grupo
                            if (showGroupDropdown) {
                                AlertDialog(
                                    onDismissRequest = { showGroupDropdown = false },
                                    title = { Text("Seleccionar Grupo") },
                                    text = {
                                        Column {
                                            OutlinedTextField(
                                                value = groupSearchText,
                                                onValueChange = { groupSearchText = it },
                                                placeholder = { Text("Buscar grupo...") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 200.dp)
                                            ) {
                                                val filteredGroups = groups.filter {
                                                    groupSearchText.isEmpty() ||
                                                            it.second.contains(
                                                                groupSearchText,
                                                                ignoreCase = true
                                                            )
                                                }
                                                items(filteredGroups) { group ->
                                                    Text(
                                                        text = group.second,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                editGroupId = group.first
                                                                currentGroupName = group.second
                                                                showGroupDropdown = false
                                                                groupSearchText = ""
                                                                // Cargar usuarios cuando se selecciona un grupo
                                                                alertHistoryViewModel.loadUsers(group.first)
                                                                // Resetear usuario seleccionado
                                                                editUserId = ""
                                                            }
                                                            .padding(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {},
                                    dismissButton = {}
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Combobox de usuario (solo si hay grupo seleccionado o la alerta tiene grupo)
                        if (editGroupId.isNotEmpty() || selectedAlertForEdit!!.groupId.isNotEmpty()) {
                            Text(
                                text = "Usuario:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Box {
                                OutlinedTextField(
                                    value = if (showUserDropdown) userSearchText else {
                                        if (editUserId.isEmpty()) {
                                            users.find { it.first == selectedAlertForEdit!!.userId }?.second
                                                ?: "Seleccionar usuario"
                                        } else {
                                            users.find { it.first == editUserId }?.second
                                                ?: "Seleccionar usuario"
                                        }
                                    },
                                    onValueChange = { 
                                        userSearchText = it
                                        if (it.isNotEmpty()) {
                                            closeOtherDropdowns("user")
                                            showUserDropdown = true
                                        } else {
                                            showUserDropdown = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(userFocusRequester),
                                    trailingIcon = {
                                        IconButton(onClick = { 
                                            showUserDropdown = !showUserDropdown
                                            closeOtherDropdowns("user")
                                            if (!showUserDropdown) {
                                                userSearchText = ""
                                            }
                                        }) {
                                            Icon(
                                                if (showUserDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = "Expandir"
                                            )
                                        }
                                    },
                                    placeholder = { Text("Buscar usuario...") },
                                    readOnly = !showUserDropdown
                                )

                                // Dropdown de usuario
                                if (showUserDropdown) {
                                    AlertDialog(
                                        onDismissRequest = { showUserDropdown = false },
                                        title = { Text("Seleccionar Usuario") },
                                        text = {
                                            Column {
                                                OutlinedTextField(
                                                    value = userSearchText,
                                                    onValueChange = { userSearchText = it },
                                                    placeholder = { Text("Buscar usuario...") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 200.dp)
                                                ) {
                                                    val filteredUsers = users.filter {
                                                        userSearchText.isEmpty() ||
                                                                it.second.contains(
                                                                    userSearchText,
                                                                    ignoreCase = true
                                                                )
                                                    }
                                                    items(filteredUsers) { user ->
                                                        Text(
                                                            text = user.second,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    editUserId = user.first
                                                                    showUserDropdown = false
                                                                    userSearchText = ""
                                                                    // Obtener automáticamente el grupo del usuario seleccionado
                                                                    CoroutineScope(Dispatchers.IO).launch {
                                                                        val userGroupId =
                                                                            alertHistoryViewModel.getUserGroupId(
                                                                                user.first
                                                                            )
                                                                        if (userGroupId.isNotEmpty()) {
                                                                            editGroupId = userGroupId
                                                                            // Recargar usuarios del nuevo grupo
                                                                            alertHistoryViewModel.loadUsers(
                                                                                userGroupId
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                .padding(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {},
                                        dismissButton = {}
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Combobox de tipo de alerta
                        Text(
                            text = "Tipo de Alerta:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box {
                            OutlinedTextField(
                                value = if (showAlertTypeDropdown) alertTypeSearchText else {
                                    if (editAlertTypeId.isEmpty()) {
                                        alertTypes.find { it.first == selectedAlertForEdit!!.alertTypeId }?.second
                                            ?: "Seleccionar tipo"
                                    } else {
                                        alertTypes.find { it.first == editAlertTypeId }?.second
                                            ?: "Seleccionar tipo"
                                    }
                                },
                                onValueChange = { 
                                    alertTypeSearchText = it
                                    if (it.isNotEmpty()) {
                                        closeOtherDropdowns("alertType")
                                        showAlertTypeDropdown = true
                                    } else {
                                        showAlertTypeDropdown = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(alertTypeFocusRequester),
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        showAlertTypeDropdown = !showAlertTypeDropdown
                                        closeOtherDropdowns("alertType")
                                        if (!showAlertTypeDropdown) {
                                            alertTypeSearchText = ""
                                        }
                                    }) {
                                        Icon(
                                            if (showAlertTypeDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = "Expandir"
                                        )
                                    }
                                },
                                placeholder = { Text("Buscar tipo de alerta...") },
                                readOnly = !showAlertTypeDropdown
                            )

                            // Dropdown de tipo de alerta
                            if (showAlertTypeDropdown) {
                                AlertDialog(
                                    onDismissRequest = { showAlertTypeDropdown = false },
                                    title = { Text("Seleccionar Tipo de Alerta") },
                                    text = {
                                        Column {
                                            OutlinedTextField(
                                                value = alertTypeSearchText,
                                                onValueChange = { alertTypeSearchText = it },
                                                placeholder = { Text("Buscar tipo de alerta...") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 200.dp)
                                            ) {
                                                val filteredAlertTypes = alertTypes.filter {
                                                    alertTypeSearchText.isEmpty() ||
                                                            it.second.contains(
                                                                alertTypeSearchText,
                                                                ignoreCase = true
                                                            )
                                                }
                                                items(filteredAlertTypes) { alertType ->
                                                    Text(
                                                        text = alertType.second,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                editAlertTypeId = alertType.first
                                                                showAlertTypeDropdown = false
                                                                alertTypeSearchText = ""
                                                            }
                                                            .padding(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {},
                                    dismissButton = {}
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo de mensaje
                        Text(
                            text = "Mensaje:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = editMessage,
                            onValueChange = { editMessage = it },
                            placeholder = { Text("Escribe un mensaje...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Campo de observación
                        Text(
                            text = "Observación:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = editObservation,
                            onValueChange = { editObservation = it },
                            placeholder = { Text("Escribe una observación...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botones
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    showEditDialog = false
                                    selectedAlertForEdit = null
                                    // Resetear campos
                                    editMessage = ""
                                    editObservation = ""
                                    editUserId = ""
                                    editGroupId = ""
                                    editAlertTypeId = ""
                                    showUserDropdown = false
                                    showGroupDropdown = false
                                    showAlertTypeDropdown = false
                                }
                            ) {
                                Text("Cancelar")
                            }

                            Button(
                                onClick = { showSaveConfirmation = true }
                            ) {
                                Text("Guardar")
                            }
                        }
                    }
                }
            }
        }
    }

    // Eliminar los dropdowns separados que estaban fuera del Dialog

    // Diálogo de confirmación de guardado
    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text("Confirmar Cambios") },
            text = { Text("¿Estás seguro de que quieres guardar los cambios realizados?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedAlertForEdit?.let { alert ->
                            val finalMessage =
                                if (editMessage.isEmpty()) alert.message else editMessage
                            val finalObservation =
                                if (editObservation.isEmpty()) alert.observation else editObservation
                            val finalUserId = if (editUserId.isEmpty()) alert.userId else editUserId
                            val finalGroupId =
                                if (editGroupId.isEmpty()) alert.groupId else editGroupId
                            val finalAlertTypeId =
                                if (editAlertTypeId.isEmpty()) alert.alertTypeId else editAlertTypeId

                            alertHistoryViewModel.updateAlert(
                                alert.id,
                                mapOf(
                                    "message" to finalMessage,
                                    "observation" to finalObservation,
                                    "userId" to finalUserId,
                                    "groupId" to finalGroupId,
                                    "alertTypeId" to finalAlertTypeId
                                )
                            )
                        }
                        showSaveConfirmation = false
                        showEditDialog = false
                        selectedAlertForEdit = null
                        // Resetear campos
                        editMessage = ""
                        editObservation = ""
                        editUserId = ""
                        editGroupId = ""
                        editAlertTypeId = ""
                        showUserDropdown = false
                        showGroupDropdown = false
                        showAlertTypeDropdown = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSaveConfirmation = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AlertHistoryItem(
    alert: Alert,
    isExpanded: Boolean,
    onToggleExpansion: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val alertHistoryViewModel: AlertHistoryViewModel = viewModel()
    
    // Estados para nombres
    var userName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var alertTypeName by remember { mutableStateOf("") }
    
    // Cargar nombres inmediatamente cuando se renderiza el componente
    LaunchedEffect(alert.id) {
        // Cargar nombres secuencialmente para evitar problemas de contexto
        userName = alertHistoryViewModel.getUserName(alert.userId)
        groupName = alertHistoryViewModel.getGroupName(alert.groupId)
        alertTypeName = alertHistoryViewModel.getAlertTypeName(alert.alertTypeId)
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
                IconButton(
                    onClick = { onToggleExpansion(alert.id) }
                ) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Contraer" else "Expandir"
                    )
                }
            }
            
            // Contenido expandido
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                // Detalles de la alerta
                AlertDetailRow(
                    icon = Icons.Filled.Group,
                    label = "Grupo",
                    value = if (groupName.isNotEmpty()) groupName else "Cargando..."
                )
                
                AlertDetailRow(
                    icon = Icons.Filled.Notifications,
                    label = "Tipo de Alerta",
                    value = if (alertTypeName.isNotEmpty()) alertTypeName else "Cargando..."
                )
                
                if (alert.message.isNotEmpty()) {
                    AlertDetailRow(
                        icon = Icons.Filled.Message,
                        label = "Mensaje",
                        value = alert.message
                    )
                }
                
                if (alert.observation.isNotEmpty()) {
                    AlertDetailRow(
                        icon = Icons.Filled.Info,
                        label = "Observación",
                        value = alert.observation
                    )
                }
                
                AlertDetailRow(
                    icon = getStatusIcon(alert.status),
                    label = "Estado",
                    value = getStatusText(alert.status)
                )
                
                if (alert.location != null) {
                    AlertDetailRow(
                        icon = Icons.Filled.LocationOn,
                        label = "Ubicación",
                        value = "Lat: ${alert.location.latitude}, Lon: ${alert.location.longitude}"
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
    return when (status.lowercase()) {
        "active" -> Icons.Filled.Pending
        "terminado" -> Icons.Filled.CheckCircle
        "reemplazada" -> Icons.Filled.Cancel
        else -> Icons.Filled.Info
    }
}

fun getStatusText(status: String): String {
    return when (status.lowercase()) {
        "active" -> "Activa"
        "terminado" -> "Terminada"
        "reemplazada" -> "Reemplazada"
        else -> status
    }
}
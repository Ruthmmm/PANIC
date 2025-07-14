package com.example.panico.ui.screens

import android.Manifest
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.panico.ui.viewmodel.MapViewModel
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import com.google.maps.android.compose.Circle
import androidx.compose.ui.graphics.lerp
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import android.os.VibratorManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Info
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.text.font.FontStyle


@OptIn(ExperimentalPermissionsApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val mapViewModel: MapViewModel = viewModel()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val alerts by mapViewModel.alerts.collectAsState()
    val cameraPosition by mapViewModel.cameraPosition.collectAsState()
    val userLocation by mapViewModel.userLocation.collectAsState()
    val alertSent by mapViewModel.alertSent.collectAsState()
    val cameraPositionState = rememberCameraPositionState()
    val errorMessage by mapViewModel.errorMessage.collectAsState()
    val showAlertSentToast by mapViewModel.showAlertSentToast.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    // Flag para saber si el usuario presionó el botón
    val userAction = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStart by rememberUpdatedState(newValue = {
        mapViewModel.loadAlerts()
    })
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val userName by mapViewModel.userName.collectAsState()
    val userRole by mapViewModel.userRole.collectAsState()

    // Observer de lifecycle con cleanup
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                currentOnStart()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Cargar perfil de usuario solo con LaunchedEffect
    LaunchedEffect(Unit) {
        mapViewModel.loadUserProfile()
    }

    val drawerItems = listOf(
        Triple("Alertas específicas", Icons.Filled.Notifications, ""),
        Triple("Alertas activas", Icons.Filled.List, ""),
        Triple("Historial de alertas", Icons.Filled.History, ""),
        Triple("Usuarios", Icons.Filled.Person, ""),
        Triple("Grupos", Icons.Filled.Group, ""),
        Triple("Perfil", Icons.Filled.AssignmentInd, "")
    )
    var selectedItem by remember { mutableStateOf(0) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerWidth = screenWidth * 0.7f
    val animatedDrawerOffset by animateDpAsState(
        targetValue = if (drawerOpen) 0.dp else -drawerWidth,
        animationSpec = tween(durationMillis = 300), label = "drawer-offset"
    )

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
    }

    // Vibrar solo si el cambio de estado fue por acción del usuario
    LaunchedEffect(alertSent) {
        if (userAction.value) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
            userAction.value = false
        }
    }

    // Centrar la cámara cuando se obtenga la ubicación del usuario
    LaunchedEffect(userLocation) {
        userLocation?.let { latLng ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
        }
    }

    // Solicitar el permiso si no está concedido
    LaunchedEffect(Unit) {
        if (locationPermissionState.status is PermissionStatus.Denied) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Mostrar mensaje flotante (Toast) cuando la alerta se envía correctamente
    LaunchedEffect(showAlertSentToast) {
        if (showAlertSentToast) {
            Toast.makeText(context, "Alerta enviada", Toast.LENGTH_SHORT).show()
            mapViewModel.resetShowAlertSentToast()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Scrim
        if (drawerOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { drawerOpen = false }
                    .zIndex(1f)
            )
        }
        // Drawer
        Box(
            Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .offset(x = animatedDrawerOffset)
                .background(MaterialTheme.colorScheme.surface)
                .zIndex(2f)
        ) {
            // Header profesional con fondo y avatar
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = userName,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = userRole,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
                Divider()
                drawerItems.forEachIndexed { index, triple ->
                    NavigationDrawerItem(
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(triple.second, contentDescription = null)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(triple.first, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "Términos y condiciones",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 24.dp),
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        // Contenido principal
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PANICO") },
                    navigationIcon = {
                        IconButton(onClick = { drawerOpen = true }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    }
                )
            },
            modifier = Modifier.zIndex(0f)
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    // Mensaje de error si ocurre un problema al enviar la alerta
                    if (errorMessage != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFD32F2F))
                                .padding(8.dp)
                        ) {
                            Text(errorMessage ?: "", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Mensaje de alerta enviada fijo en la parte superior, fuera del Box principal
                    if (alertSent) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ALERTA ENVIADA",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            BlinkingDot()
                        }
                    }
                    // El resto de la UI va en un Box que ocupa el resto del espacio
                    Box(modifier = Modifier.weight(1f)) {
                        when (val status = locationPermissionState.status) {
                            is PermissionStatus.Granted -> {
                                LaunchedEffect(true) {
                                    mapViewModel.loadAlerts()
                                    mapViewModel.centerCameraOnUser()
                                }
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState,
                                    properties = MapProperties(
                                        isMyLocationEnabled = true
                                    )
                                ) {
                                    if (alertSent && userLocation != null) {
                                        BlinkingAlertCircle(
                                            center = userLocation,
                                            radius = 100.0 // metros
                                        )
                                    }
                                }
                                // Botón grande en la parte inferior, centrado, más grande
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Transparent)
                                        .padding(bottom = 40.dp),
                                    contentAlignment = Alignment.Center // Centrar el contenido
                                ) {
                                    Button(
                                        onClick = {
                                            userAction.value = true
                                            if (!alertSent) mapViewModel.sendAlert()
                                            else mapViewModel.endAlert()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .height(90.dp),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = if (!alertSent) Color.Red else Color.Gray
                                        )
                                    ) {
                                        Text(
                                            if (!alertSent) "ENVIAR ALERTA" else "TERMINAR ALERTA",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 28.sp
                                        )
                                    }
                                }
                            }
                            is PermissionStatus.Denied -> {
                                val packageName = context.packageName
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "Permiso de ubicación no disponible",
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        if (status.shouldShowRationale) {
                                            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                                                Text("Dar permiso de ubicación")
                                            }
                                        } else {
                                            Button(onClick = {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = android.net.Uri.fromParts("package", packageName, null)
                                                }
                                                context.startActivity(intent)
                                            }) {
                                                Text("Abrir configuración")
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Debes conceder el permiso manualmente desde la configuración del sistema.",
                                                color = Color.Gray,
                                                fontSize = 14.sp
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


@Composable
fun BlinkingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "blinking-dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0.0f at 0
                1.0f at 400
                0.0f at 800
            },
            repeatMode = RepeatMode.Restart
        ), label = "dot-alpha"
    )
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .size(16.dp)
            .background(Color.White.copy(alpha = alpha), shape = CircleShape)
    )
}

// Círculo rojo parpadeante en el mapa, oscila entre dos tonos de rojo con transparencia
@Composable
fun BlinkingAlertCircle(center: LatLng?, radius: Double) {
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
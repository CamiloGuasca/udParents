package com.example.udparents.vista.pantallas

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.udparents.viewmodel.VistaModeloApps
import kotlinx.coroutines.delay
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBarDefaults.topAppBarColors
@Composable
fun PantallaControlApps(
    uidHijo: String,
    vistaModeloApps: VistaModeloApps = viewModel()
) {
    // Estados para los datos de la UI
    val usosApps by vistaModeloApps.listaUsos.collectAsState()
    val estadosBloqueo by vistaModeloApps.estadoBloqueoApp.collectAsState()
    val limitesApps by vistaModeloApps.limitesApp.collectAsState()

    // Contexto local para acceder al PackageManager y obtener iconos
    val context = LocalContext.current

    // Estado para el tiempo total de pantalla del día
    val tiempoTotalPantalla by remember(usosApps) {
        derivedStateOf {
            usosApps.sumOf { it.tiempoUso }
        }
    }

    // Cargar datos iniciales al entrar en la pantalla
    LaunchedEffect(uidHijo) {
        val ahora = System.currentTimeMillis()
        val inicioHoy = ahora - (ahora % (24 * 60 * 60 * 1000))
        vistaModeloApps.cargarUsos(uidHijo, inicioHoy, ahora)
        vistaModeloApps.cargarLimites(uidHijo)
    }

    // Refrescar el uso de las aplicaciones cada 60 segundos
    LaunchedEffect(uidHijo) {
        while (true) {
            val ahora = System.currentTimeMillis()
            val inicioHoy = ahora - (ahora % (24 * 60 * 60 * 1000))
            vistaModeloApps.cargarUsos(uidHijo, inicioHoy, ahora)
            delay(60_000L) // Espera 1 minuto antes de la próxima actualización
        }
    }

    // Verificar el estado de bloqueo para cada app (se mantiene)
    LaunchedEffect(usosApps) {
        usosApps.forEach {
            vistaModeloApps.verificarSiEstaBloqueada(uidHijo, it.nombrePaquete)
        }
    }

    // --- PALETA DE COLORES COHERENTE CON PANTALLA REPORTE ---
    val primaryDark = Color(0xFF1A237E) // Azul oscuro muy profundo (fondo principal)
    val primaryLight = Color(0xFF3F51B5) // Azul primario para TopAppBar, botón principal
    val accentColor = Color(0xFFCDDC39) // Verde lima vibrante para botones de acción y texto destacado
    val onPrimaryColor = Color.White // Texto sobre azules
    val surfaceColor = Color(0xFF2C387F) // Azul oscuro para tarjetas de apps
    val onSurfaceColor = Color(0xFFE8EAF6) // Texto claro sobre tarjetas de apps
    val borderColor = Color(0xFF9FA8DA) // Borde para OutlinedButtons

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de Aplicaciones", color = onPrimaryColor) },
                // Si tienes un botón de navegación para volver, lo agregarías aquí
                // navigationIcon = { /* ... */ },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryLight, // Color de la barra superior
                    titleContentColor = onPrimaryColor // Color del título
                )
            )
        },
        containerColor = primaryDark // Color de fondo del Scaffold
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica el padding del Scaffold
                .padding(horizontal = 16.dp, vertical = 8.dp) // Padding adicional para el contenido
        ) {
            // Resumen del tiempo total de pantalla del día
            val totalHoras = tiempoTotalPantalla / (60 * 60 * 1000)
            val totalMinutos = (tiempoTotalPantalla % (60 * 60 * 1000)) / 60000
            Text(
                text = "Tiempo total de pantalla hoy: ${totalHoras}h ${totalMinutos}m",
                style = MaterialTheme.typography.titleMedium,
                color = onSurfaceColor // Color de texto acorde al tema
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje si no hay aplicaciones usadas aún
            if (usosApps.isEmpty()) {
                Text(
                    "No se encontraron aplicaciones usadas aún.",
                    color = onSurfaceColor // Color de texto acorde al tema
                )
            }

            LazyColumn {
                items(usosApps) { app ->
                    val bloqueada = estadosBloqueo[app.nombrePaquete] == true
                    val tiempoLimite = limitesApps[app.nombrePaquete] ?: 0L
                    val tiempoUso = app.tiempoUso
                    val tiempoRestante = max(0L, tiempoLimite - tiempoUso)

                    val progreso = if (tiempoLimite > 0) tiempoUso.toFloat() / tiempoLimite.toFloat() else 0f
                    val progresoClamped = progreso.coerceIn(0f, 1f)

                    // Colores para la barra de progreso (usando la paleta definida)
                    val colorProgreso = when {
                        progreso > 0.9f -> MaterialTheme.colorScheme.error // Rojo por defecto para error
                        progreso > 0.7f -> accentColor // Verde lima para advertencia (cerca del límite)
                        else -> primaryLight // Azul primario para progreso normal
                    }

                    var nuevoLimiteHoras by remember { mutableStateOf("") }
                    var nuevoLimiteMinutos by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(surfaceColor, shape = MaterialTheme.shapes.medium) // Fondo de tarjeta con azul oscuro
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val appIcon: Drawable? = remember(app.nombrePaquete) {
                                try {
                                    context.packageManager.getApplicationIcon(app.nombrePaquete)
                                } catch (e: PackageManager.NameNotFoundException) {
                                    null
                                }
                            }

                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon.toBitmap().asImageBitmap(),
                                    contentDescription = "Icono de ${app.nombreApp}",
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (app.nombreApp == app.nombrePaquete)
                                        app.nombrePaquete
                                    else
                                        "${app.nombreApp}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = onPrimaryColor // Texto blanco
                                )
                                Text(
                                    text = "(${app.nombrePaquete})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onSurfaceColor // Texto claro
                                )

                                val usoEnMinutos = tiempoUso / 60000
                                val horasUso = usoEnMinutos / 60
                                val minutosUso = usoEnMinutos % 60
                                Text(
                                    text = "Uso hoy: ${horasUso}h ${minutosUso}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onSurfaceColor // Texto claro
                                )

                                if (tiempoLimite > 0L) {
                                    val restanteEnMinutos = tiempoRestante / 60000
                                    val horasRestantes = restanteEnMinutos / 60
                                    val minutosRestantes = restanteEnMinutos % 60
                                    Text(
                                        text = "Tiempo restante: ${horasRestantes}h ${minutosRestantes}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (tiempoRestante <= 0) MaterialTheme.colorScheme.error else accentColor // Verde lima para tiempo restante
                                    )
                                }
                            }

                            Switch(
                                checked = bloqueada,
                                onCheckedChange = { nuevoEstado ->
                                    vistaModeloApps.cambiarEstadoBloqueo(
                                        uidHijo,
                                        app.nombrePaquete,
                                        nuevoEstado
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentColor, // Pulgar en verde lima cuando está activo
                                    checkedTrackColor = accentColor.copy(alpha = 0.5f), // Pista en verde lima claro
                                    uncheckedThumbColor = onSurfaceColor, // Pulgar claro cuando inactivo
                                    uncheckedTrackColor = onSurfaceColor.copy(alpha = 0.5f) // Pista clara
                                )
                            )
                        }

                        if (tiempoLimite > 0L) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = progresoClamped,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(MaterialTheme.shapes.small),
                                color = colorProgreso,
                                trackColor = onSurfaceColor.copy(alpha = 0.1f) // Pista de la barra en un tono claro
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = nuevoLimiteHoras,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                        nuevoLimiteHoras = it
                                    }
                                },
                                label = { Text("Horas", color = onSurfaceColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors( // Colores para el TextField
                                    focusedContainerColor = Color.Transparent, // Fondo transparente
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = accentColor, // Indicador de foco en verde lima
                                    unfocusedIndicatorColor = borderColor, // Indicador normal en azul claro
                                    cursorColor = accentColor,
                                    focusedTextColor = onPrimaryColor,
                                    unfocusedTextColor = onPrimaryColor,
                                    unfocusedLabelColor = onSurfaceColor,
                                    focusedLabelColor = accentColor
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = nuevoLimiteMinutos,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                        val minutes = it.toIntOrNull()
                                        if (minutes == null || minutes <= 59) {
                                            nuevoLimiteMinutos = it
                                        }
                                    }
                                },
                                label = { Text("Minutos", color = onSurfaceColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors( // Colores para el TextField
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = accentColor,
                                    unfocusedIndicatorColor = borderColor,
                                    cursorColor = accentColor,
                                    focusedTextColor = onPrimaryColor,
                                    unfocusedTextColor = onPrimaryColor,
                                    unfocusedLabelColor = onSurfaceColor,
                                    focusedLabelColor = accentColor
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                val horas = nuevoLimiteHoras.toLongOrNull() ?: 0L
                                val minutos = nuevoLimiteMinutos.toLongOrNull() ?: 0L
                                val milis = (horas * 60 * 60 * 1000) + (minutos * 60 * 1000)
                                if (milis > 0) {
                                    vistaModeloApps.establecerLimite(uidHijo, app.nombrePaquete, milis)
                                    nuevoLimiteHoras = ""
                                    nuevoLimiteMinutos = ""
                                }
                            },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor, // Fondo en verde lima
                                    contentColor = primaryDark // Texto en azul oscuro
                                )
                            ) {
                                Text("Limitar")
                            }
                        }

                        Divider(modifier = Modifier.padding(top = 12.dp), color = borderColor.copy(alpha = 0.3f)) // Divisor más sutil
                    }
                }
            }
        }
    }
}

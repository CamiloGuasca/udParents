package com.example.udparents.vista.pantallas

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.udparents.modelo.RestriccionHorario
import com.example.udparents.viewmodel.VistaModeloApps
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// Función de ayuda para formatear los milisegundos del día a una cadena de tiempo (HH:mm)
private fun formatMillisOfDay(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = (totalSeconds / 3600) % 24
    val minutes = (totalSeconds % 3600) / 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaProgramarRestricciones(
    uidHijo: String,
    vistaModelo: VistaModeloApps = viewModel(),
    onVolver: () -> Unit
) {
    val restricciones by vistaModelo.restriccionesHorario.collectAsState()
    val usosApps by vistaModelo.listaUsos.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uidHijo) {
        vistaModelo.cargarRestriccionesHorario(uidHijo)
        val ahora = System.currentTimeMillis()
        val inicioHoy = ahora - (ahora % (24 * 60 * 60 * 1000))
        vistaModelo.cargarUsos(uidHijo, inicioHoy, ahora)
    }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingRestriction by remember { mutableStateOf<RestriccionHorario?>(null) }

    val primaryDark = Color(0xFF1A237E)
    val primaryLight = Color(0xFF3F51B5)
    val accentColor = Color(0xFFCDDC39)
    val onPrimaryColor = Color.White
    val surfaceColor = Color(0xFF2C387F)
    val onSurfaceColor = Color(0xFFE8EAF6)
    val borderColor = Color(0xFF9FA8DA)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restricciones por Horario", color = onPrimaryColor) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = onPrimaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryLight,
                    titleContentColor = onPrimaryColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRestriction = null
                    showAddEditDialog = true
                },
                containerColor = accentColor,
                contentColor = primaryDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Restricción")
            }
        },
        containerColor = primaryDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (restricciones.isEmpty()) {
                Text(
                    "No hay restricciones de horario configuradas. Pulsa '+' para añadir una.",
                    color = onSurfaceColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(restricciones) { restriccion ->
                        RestriccionHorarioCard(
                            restriccion = restriccion,
                            onEdit = {
                                editingRestriction = it
                                showAddEditDialog = true
                            },
                            onDelete = { vistaModelo.eliminarRestriccionHorario(uidHijo, it.id) },
                            onToggleEnabled = {
                                vistaModelo.guardarRestriccionHorario(uidHijo, it.copy(isEnabled = !it.isEnabled))
                            },
                            primaryDark = primaryDark,
                            primaryLight = primaryLight,
                            accentColor = accentColor,
                            onPrimaryColor = onPrimaryColor,
                            surfaceColor = surfaceColor,
                            onSurfaceColor = onSurfaceColor,
                            borderColor = borderColor
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditRestriccionDialog(
            restriccion = editingRestriction,
            onDismiss = { showAddEditDialog = false },
            onSave = { newRestriction ->
                vistaModelo.guardarRestriccionHorario(uidHijo, newRestriction)
                showAddEditDialog = false
            },
            appsList = usosApps.map { it.nombreApp to it.nombrePaquete }.distinctBy { it.second }.sortedBy { it.first },
            primaryDark = primaryDark,
            primaryLight = primaryLight,
            accentColor = accentColor,
            onPrimaryColor = onPrimaryColor,
            surfaceColor = surfaceColor,
            onSurfaceColor = onSurfaceColor,
            borderColor = borderColor
        )
    }
}

@Composable
fun RestriccionHorarioCard(
    restriccion: RestriccionHorario,
    onEdit: (RestriccionHorario) -> Unit,
    onDelete: (RestriccionHorario) -> Unit,
    onToggleEnabled: (RestriccionHorario) -> Unit,
    primaryDark: Color,
    primaryLight: Color,
    accentColor: Color,
    onPrimaryColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    borderColor: Color
) {
    val daysMap = mapOf(
        Calendar.MONDAY to "Lun", Calendar.TUESDAY to "Mar", Calendar.WEDNESDAY to "Mié",
        Calendar.THURSDAY to "Jue", Calendar.FRIDAY to "Vie", Calendar.SATURDAY to "Sáb", Calendar.SUNDAY to "Dom"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = restriccion.ruleName.ifEmpty { "Regla sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    color = onPrimaryColor
                )
                Switch(
                    checked = restriccion.isEnabled,
                    onCheckedChange = { onToggleEnabled(restriccion) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = onSurfaceColor,
                        uncheckedTrackColor = onSurfaceColor.copy(alpha = 0.5f)
                    )
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "App: ${restriccion.appName.ifEmpty { "Todas las Apps" }}",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceColor
            )
            // Lógica corregida para mostrar la hora
            val startTimeStr = formatMillisOfDay(restriccion.startTimeMillis)
            val endTimeStr = formatMillisOfDay(restriccion.endTimeMillis)
            Text(
                text = "Horario: $startTimeStr - $endTimeStr",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceColor
            )
            Text(
                text = "Días: ${restriccion.daysOfWeek.map { daysMap[it] ?: "" }.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceColor
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onEdit(restriccion) }) {
                    Text("Editar", color = accentColor)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onDelete(restriccion) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRestriccionDialog(
    restriccion: RestriccionHorario?,
    onDismiss: () -> Unit,
    onSave: (RestriccionHorario) -> Unit,
    appsList: List<Pair<String, String>>,
    primaryDark: Color,
    primaryLight: Color,
    accentColor: Color,
    onPrimaryColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    borderColor: Color
) {
    var ruleName by remember { mutableStateOf(restriccion?.ruleName ?: "") }
    var selectedAppPackage by remember { mutableStateOf(restriccion?.packageName ?: "ALL_APPS") }
    var selectedAppName by remember { mutableStateOf(restriccion?.appName ?: "Todas las Apps") }
    var startTime by remember { mutableStateOf(restriccion?.startTimeMillis ?: 0L) }
    var endTime by remember { mutableStateOf(restriccion?.endTimeMillis ?: 0L) }
    val selectedDays = remember { mutableStateListOf(*restriccion?.daysOfWeek?.toTypedArray() ?: emptyArray()) }
    var expandedAppDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val daysOfWeekOptions = listOf(
        Calendar.MONDAY to "Lunes", Calendar.TUESDAY to "Martes", Calendar.WEDNESDAY to "Miércoles",
        Calendar.THURSDAY to "Jueves", Calendar.FRIDAY to "Viernes", Calendar.SATURDAY to "Sábado", Calendar.SUNDAY to "Domingo"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (restriccion == null) "Añadir Restricción" else "Editar Restricción", color = onPrimaryColor) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryDark)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Nombre de la Regla (Opcional)", color = onSurfaceColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = borderColor,
                        cursorColor = accentColor,
                        focusedTextColor = onPrimaryColor,
                        unfocusedTextColor = onPrimaryColor,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = onSurfaceColor
                    )
                )
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedAppDropdown,
                    onExpandedChange = { expandedAppDropdown = !expandedAppDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedAppName,
                        onValueChange = {},
                        label = { Text("Aplicación", color = onSurfaceColor) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAppDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = borderColor,
                            cursorColor = accentColor,
                            focusedTextColor = onPrimaryColor,
                            unfocusedTextColor = onPrimaryColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = onSurfaceColor
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expandedAppDropdown,
                        onDismissRequest = { expandedAppDropdown = false },
                        modifier = Modifier.background(surfaceColor)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas las Apps", color = onPrimaryColor) },
                            onClick = {
                                selectedAppPackage = "ALL_APPS"
                                selectedAppName = "Todas las Apps"
                                expandedAppDropdown = false
                            }
                        )
                        appsList.forEach { (appName, packageName) ->
                            DropdownMenuItem(
                                text = { Text(appName, color = onPrimaryColor) },
                                onClick = {
                                    selectedAppPackage = packageName
                                    selectedAppName = appName
                                    expandedAppDropdown = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Selectores de Hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, (startTime / (60 * 60 * 1000)).toInt())
                                set(Calendar.MINUTE, ((startTime % (60 * 60 * 1000)) / (60 * 1000)).toInt())
                            }
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    startTime = (hour * 60 * 60 * 1000 + minute * 60 * 1000).toLong()
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accentColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(borderColor))
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "Hora inicio")
                        Spacer(Modifier.width(4.dp))
                        Text("Inicio: ${formatMillisOfDay(startTime)}")
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, (endTime / (60 * 60 * 1000)).toInt())
                                set(Calendar.MINUTE, ((endTime % (60 * 60 * 1000)) / (60 * 1000)).toInt())
                            }
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    endTime = (hour * 60 * 60 * 1000 + minute * 60 * 1000).toLong()
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accentColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(borderColor))
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "Hora fin")
                        Spacer(Modifier.width(4.dp))
                        Text("Fin: ${formatMillisOfDay(endTime)}")
                    }
                }
                Spacer(Modifier.height(8.dp))

                Text("Días de la Semana:", style = MaterialTheme.typography.bodyLarge, color = onPrimaryColor)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeekOptions.forEach { (dayInt, dayName) ->
                        FilterChip(
                            selected = selectedDays.contains(dayInt),
                            onClick = {
                                if (selectedDays.contains(dayInt)) {
                                    selectedDays.remove(dayInt)
                                } else {
                                    selectedDays.add(dayInt)
                                }
                            },
                            enabled = true,
                            label = { Text(dayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = primaryDark,
                                containerColor = surfaceColor,
                                labelColor = onSurfaceColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedDays.contains(dayInt),
                                borderColor = if (selectedDays.contains(dayInt)) accentColor else borderColor
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newId = restriccion?.id ?: UUID.randomUUID().toString()
                    val newRestriction = RestriccionHorario(
                        id = newId,
                        ruleName = ruleName,
                        packageName = selectedAppPackage,
                        appName = selectedAppName,
                        startTimeMillis = startTime,
                        endTimeMillis = endTime,
                        daysOfWeek = selectedDays.toList(),
                        isEnabled = restriccion?.isEnabled ?: true
                    )
                    onSave(newRestriction)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = primaryDark)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = onPrimaryColor),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(borderColor))
            ) {
                Text("Cancelar")
            }
        },
        containerColor = primaryDark
    )
}

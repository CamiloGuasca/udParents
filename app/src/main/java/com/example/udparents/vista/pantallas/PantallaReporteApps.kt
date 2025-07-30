package com.example.udparents.vista.pantallas

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.udparents.viewmodel.VistaModeloApps
import java.text.SimpleDateFormat
import java.util.*

enum class TipoFecha { DESDE, HASTA }

fun formatoTiempo(ms: Long): String {
    val totalMin = ms / 1000 / 60
    val horas = totalMin / 60
    val minutos = totalMin % 60

    return when {
        horas > 0 -> "${horas}h ${minutos}m"
        minutos > 0 -> "${minutos}m"
        else -> "Menos de 1m"
    }
}

// ... (imports previos)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaReporteApps(
    listaHijos: List<Pair<String, String>>,
    vistaModelo: VistaModeloApps = viewModel(),
    onVolver: () -> Unit
) {
    val usos by vistaModelo.listaUsos.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val calendarDesde = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val calendarHasta = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
    }

    val fechaDesde = remember { mutableStateOf(calendarDesde.timeInMillis) }
    val fechaHasta = remember { mutableStateOf(calendarHasta.timeInMillis) }

    val fechaDesdeTexto = remember { mutableStateOf(dateFormat.format(Date(fechaDesde.value))) }
    val fechaHastaTexto = remember { mutableStateOf(dateFormat.format(Date(fechaHasta.value))) }

    var expanded by remember { mutableStateOf(false) }
    var hijoSeleccionado by remember { mutableStateOf<Pair<String, String>?>(null) }

    var mostrarFicha by remember { mutableStateOf(false) }

    fun mostrarSelectorFecha(
        context: Context,
        tipo: TipoFecha,
        calendario: Calendar,
        onFechaSeleccionada: (Long, String) -> Unit
    ) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendario.set(year, month, day)
                if (tipo == TipoFecha.DESDE) {
                    calendario.set(Calendar.HOUR_OF_DAY, 0)
                    calendario.set(Calendar.MINUTE, 0)
                    calendario.set(Calendar.SECOND, 0)
                    calendario.set(Calendar.MILLISECOND, 0)
                } else {
                    calendario.set(Calendar.HOUR_OF_DAY, 23)
                    calendario.set(Calendar.MINUTE, 59)
                    calendario.set(Calendar.SECOND, 59)
                    calendario.set(Calendar.MILLISECOND, 999)
                }
                val fechaMillis = calendario.timeInMillis
                onFechaSeleccionada(fechaMillis, dateFormat.format(Date(fechaMillis)))
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Reporte de uso de aplicaciones", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                readOnly = true,
                value = hijoSeleccionado?.second ?: "Selecciona un hijo",
                onValueChange = {},
                label = { Text("Hijo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listaHijos.forEach { hijo ->
                    DropdownMenuItem(
                        text = { Text(hijo.second) },
                        onClick = {
                            hijoSeleccionado = hijo
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                mostrarSelectorFecha(context, TipoFecha.DESDE, calendarDesde) { millis, texto ->
                    fechaDesde.value = millis
                    fechaDesdeTexto.value = texto
                }
            }) {
                Text("Desde: ${fechaDesdeTexto.value}")
            }

            Button(onClick = {
                mostrarSelectorFecha(context, TipoFecha.HASTA, calendarHasta) { millis, texto ->
                    fechaHasta.value = millis
                    fechaHastaTexto.value = texto
                }
            }) {
                Text("Hasta: ${fechaHastaTexto.value}")
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                hijoSeleccionado?.let { (id, _) ->
                    vistaModelo.cargarUsos(id, fechaDesde.value, fechaHasta.value)
                }
            },
            enabled = hijoSeleccionado != null
        ) {
            Text("Consultar")
        }

        Spacer(Modifier.height(16.dp))

        if (usos.isEmpty()) {
            Text("No se encontraron datos en el rango seleccionado.")
        } else {
            val totalMs = usos.sumOf { it.tiempoUso }
            Text("⏱ Tiempo total de uso: ${formatoTiempo(totalMs)}")
            Spacer(Modifier.height(16.dp))

            Button(onClick = { mostrarFicha = !mostrarFicha }) {
                Text(if (mostrarFicha) "Ocultar ficha técnica" else "Ver ficha técnica")
            }

            Spacer(Modifier.height(16.dp))

            if (mostrarFicha) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.6f)
                ) {
                    items(usos) { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                if (app.nombreApp == app.nombrePaquete) {
                                    Text(app.nombrePaquete, style = MaterialTheme.typography.titleMedium)
                                } else {
                                    Text(app.nombreApp, style = MaterialTheme.typography.titleMedium)
                                    Text("(${app.nombrePaquete})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("🕒 Fecha: ${dateFormat.format(Date(app.fechaUso))}")
                                Text("⏱ Tiempo de uso: ${formatoTiempo(app.tiempoUso)}")
                            }
                        }
                    }
                }
            } else {
                usos.forEach { app ->
                    val nombreMostrado = if (app.nombreApp == app.nombrePaquete)
                        app.nombrePaquete
                    else
                        "${app.nombreApp} (${app.nombrePaquete})"

                    Text("📱 $nombreMostrado - ${dateFormat.format(Date(app.fechaUso))} - ${formatoTiempo(app.tiempoUso)}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onVolver) {
            Text("Volver")
        }
    }
}

package com.example.udparents.vista.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.udparents.viewmodel.VistaModeloApps
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun PantallaControlApps(
    uidHijo: String,
    vistaModeloApps: VistaModeloApps = viewModel()
) {
    // Estados para los datos de la UI
    val usosApps by vistaModeloApps.listaUsos.collectAsState()
    val estadosBloqueo by vistaModeloApps.estadoBloqueoApp.collectAsState()
    val limitesApps by vistaModeloApps.limitesApp.collectAsState()

    // Cargar datos iniciales al entrar en la pantalla
    LaunchedEffect(uidHijo) {
        val ahora = System.currentTimeMillis()
        // Calcula el inicio del día actual (medianoche) para consultar los usos
        val inicioHoy = ahora - (ahora % (24 * 60 * 60 * 1000))
        vistaModeloApps.cargarUsos(uidHijo, inicioHoy, ahora)
        vistaModeloApps.cargarLimites(uidHijo)
    }

    // Refrescar el uso de las aplicaciones cada 60 segundos
    // para mantener la información de "tiempo restante" y "tiempo de uso" actualizada.
    LaunchedEffect(uidHijo) {
        while (true) {
            val ahora = System.currentTimeMillis()
            val inicioHoy = ahora - (ahora % (24 * 60 * 60 * 1000))
            vistaModeloApps.cargarUsos(uidHijo, inicioHoy, ahora)
            delay(60_000L) // Espera 1 minuto antes de la próxima actualización
        }
    }

    // Verificar el estado de bloqueo para cada aplicación mostrada
    LaunchedEffect(usosApps) {
        usosApps.forEach {
            vistaModeloApps.verificarSiEstaBloqueada(uidHijo, it.nombrePaquete)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Control de Aplicaciones", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        // Mensaje si no hay aplicaciones usadas aún
        if (usosApps.isEmpty()) {
            Text(
                "No se encontraron aplicaciones usadas aún.",
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Lista de aplicaciones con su información y controles
        LazyColumn {
            items(usosApps) { app ->
                val bloqueada = estadosBloqueo[app.nombrePaquete] == true
                val tiempoLimite = limitesApps[app.nombrePaquete] ?: 0L
                val tiempoUso = app.tiempoUso // Tiempo de uso acumulado en milisegundos
                val tiempoRestante = max(0L, tiempoLimite - tiempoUso)

                // Estados para los nuevos campos de entrada de límite (horas y minutos)
                var nuevoLimiteHoras by remember { mutableStateOf("") }
                var nuevoLimiteMinutos by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Nombre de la aplicación y su paquete
                            Text(
                                text = if (app.nombreApp == app.nombrePaquete)
                                    app.nombrePaquete
                                else
                                    "${app.nombreApp} (${app.nombrePaquete})",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            // Mostrar el tiempo de uso actual de la aplicación
                            val usoEnMinutos = tiempoUso / 60000
                            val horasUso = usoEnMinutos / 60
                            val minutosUso = usoEnMinutos % 60
                            Text(
                                text = "Uso hoy: ${horasUso}h ${minutosUso}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Mostrar tiempo restante si hay un límite establecido
                            if (tiempoLimite > 0L) {
                                val restanteEnMinutos = tiempoRestante / 60000
                                val horasRestantes = restanteEnMinutos / 60
                                val minutosRestantes = restanteEnMinutos % 60
                                Text(
                                    text = "Tiempo restante: ${horasRestantes}h ${minutosRestantes}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (tiempoRestante <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Switch para bloquear/desbloquear la aplicación
                        Switch(
                            checked = bloqueada,
                            onCheckedChange = { nuevoEstado ->
                                vistaModeloApps.cambiarEstadoBloqueo(
                                    uidHijo,
                                    app.nombrePaquete,
                                    nuevoEstado
                                )
                            }
                        )
                    }

                    // Campos de entrada para establecer el límite de tiempo (horas y minutos)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = nuevoLimiteHoras,
                            onValueChange = {
                                // Acepta solo números y limita a 2 dígitos para horas
                                if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                    nuevoLimiteHoras = it
                                }
                            },
                            label = { Text("Horas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = nuevoLimiteMinutos,
                            onValueChange = {
                                // Acepta solo números y limita a 2 dígitos para minutos (0-59)
                                if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                    val minutes = it.toIntOrNull()
                                    if (minutes == null || minutes <= 59) {
                                        nuevoLimiteMinutos = it
                                    }
                                }
                            },
                            label = { Text("Minutos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val horas = nuevoLimiteHoras.toLongOrNull() ?: 0L
                            val minutos = nuevoLimiteMinutos.toLongOrNull() ?: 0L
                            // Calcula el tiempo total en milisegundos
                            val milis = (horas * 60 * 60 * 1000) + (minutos * 60 * 1000)
                            if (milis > 0) { // Solo establece el límite si es mayor que 0
                                vistaModeloApps.establecerLimite(uidHijo, app.nombrePaquete, milis)
                                nuevoLimiteHoras = ""
                                nuevoLimiteMinutos = ""
                            }
                        }) {
                            Text("Establecer Límite")
                        }
                    }

                    Divider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

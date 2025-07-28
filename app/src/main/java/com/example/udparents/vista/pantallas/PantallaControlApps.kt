package com.example.udparents.vista.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.udparents.viewmodel.VistaModeloApps

@Composable
fun PantallaControlApps(
    uidHijo: String,
    vistaModeloApps: VistaModeloApps = viewModel()
) {
    val usosApps by vistaModeloApps.listaUsos.collectAsState()
    val estadosBloqueo by vistaModeloApps.estadoBloqueoApp.collectAsState()

    LaunchedEffect(uidHijo) {
        val ahora = System.currentTimeMillis()
        val hace7Dias = ahora - (7 * 24 * 60 * 60 * 1000)
        vistaModeloApps.cargarUsos(uidHijo, hace7Dias, ahora)
    }

    LaunchedEffect(usosApps) {
        usosApps.forEach {
            vistaModeloApps.verificarSiEstaBloqueada(uidHijo, it.nombrePaquete)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Control de Aplicaciones", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        if (usosApps.isEmpty()) {
            Text("No se encontraron aplicaciones usadas aún.", color = MaterialTheme.colorScheme.onBackground)
        }

        LazyColumn {
            items(usosApps) { app ->
                val bloqueada = estadosBloqueo[app.nombrePaquete] == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.nombreApp, style = MaterialTheme.typography.bodyLarge)
                        // Ya no mostramos el nombre del paquete para mantener la vista limpia
                        // Text("Paquete: ${app.nombrePaquete}", style = MaterialTheme.typography.bodySmall)
                    }

                    Switch(
                        checked = bloqueada,
                        onCheckedChange = { nuevoEstado ->
                            vistaModeloApps.cambiarEstadoBloqueo(uidHijo, app.nombrePaquete, nuevoEstado)
                        }
                    )
                }
                Divider()
            }
        }
    }
}

package com.example.udparents.vista.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PantallaSeleccionHijoParaControl(
    listaHijos: List<Pair<String, String>>,
    onHijoSeleccionado: (String) -> Unit,
    onVolver: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Selecciona un hijo", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        listaHijos.forEach { (uid, nombre) ->
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHijoSeleccionado(uid) }
                    .padding(12.dp)
            )
            Divider()
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { onVolver() }) {
            Text("Volver")
        }
    }
}

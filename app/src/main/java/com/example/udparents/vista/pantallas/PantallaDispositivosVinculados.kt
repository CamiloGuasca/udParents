package com.example.udparents.vista.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.udparents.viewmodel.VistaModeloVinculacion
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PantallaDispositivosVinculados(
    onVolverAlMenuPadre: () -> Unit // callback para regresar
) {
    val viewModel: VistaModeloVinculacion = viewModel()
    val dispositivos by viewModel.dispositivosVinculados.collectAsState()

    val usuario = FirebaseAuth.getInstance().currentUser
    val idPadre = usuario?.uid ?: ""

    LaunchedEffect(idPadre) {
        if (idPadre.isNotEmpty()) {
            viewModel.cargarDispositivosVinculados(idPadre)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Dispositivos vinculados",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { onVolverAlMenuPadre() }) {
            Text("Volver al menú principal")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dispositivos.isEmpty()) {
            Text(
                "No hay dispositivos vinculados.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn {
                items(dispositivos) { dispositivo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E6E6))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Código: ${dispositivo.codigo}", fontSize = 16.sp)
                            Text("Dispositivo hijo (UID): ${dispositivo.dispositivoHijo}", fontSize = 14.sp, color = Color.DarkGray)
                            Text("Nombre: ${dispositivo.nombreHijo}", fontSize = 14.sp)
                            Text("Edad: ${dispositivo.edadHijo}", fontSize = 14.sp)
                            Text("Sexo: ${dispositivo.sexoHijo}", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

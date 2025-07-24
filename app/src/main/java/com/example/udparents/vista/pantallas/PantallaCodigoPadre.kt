package com.example.udparents.vista.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
fun PantallaCodigoPadre(
    onVolverAlMenuPrincipal: () -> Unit // 👈 nuevo parámetro
) {
    val viewModel: VistaModeloVinculacion = viewModel()
    val codigoGenerado by viewModel.codigoGenerado.collectAsState()

    val usuario = FirebaseAuth.getInstance().currentUser
    val idPadre = usuario?.uid ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Generar Código de Vinculación", fontSize = 22.sp, color = Color(0xFF003366))

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (idPadre.isNotEmpty()) {
                    viewModel.generarCodigo(idPadre)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
        ) {
            Text("Generar Código", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (codigoGenerado != null) {
            Text(
                text = "Tu código: $codigoGenerado",
                fontSize = 20.sp,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 🔙 Botón para volver al menú principal
        TextButton(onClick = { onVolverAlMenuPrincipal() }) {
            Text("Volver al menú principal", color = Color(0xFF003366))
        }
    }
}

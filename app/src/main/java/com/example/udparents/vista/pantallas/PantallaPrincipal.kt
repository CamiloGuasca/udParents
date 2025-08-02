package com.example.udparents.vista.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.udparents.viewmodel.VistaModeloApps
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PantallaPrincipal(
    onCerrarSesion: () -> Unit,
    onIrAVinculacionPadre: () -> Unit,
    onIrAReporteApps: (List<Pair<String, String>>) -> Unit,
    onIrAControlApps: (List<Pair<String, String>>) -> Unit,
    onIrADispositivosVinculados: () -> Unit,
    onIrAProgramarRestricciones: (List<Pair<String, String>>) -> Unit,
    onIrAResumenTiempoPantalla: (List<Pair<String, String>>) -> Unit
) {
    val vistaModelo: VistaModeloApps = viewModel()
    val hijosVinculados by vistaModelo.hijosVinculados.collectAsState()
    val uidPadre = FirebaseAuth.getInstance().currentUser?.uid

    // Cargar hijos desde el ViewModel usando LaunchedEffect
    LaunchedEffect(uidPadre) {
        uidPadre?.let { vistaModelo.cargarHijos(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF003366))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFFE6E6E6), RoundedCornerShape(16.dp))
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bienvenido 👋", fontSize = 24.sp, color = Color(0xFF003366), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onIrAVinculacionPadre() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006699)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generar código de vinculación", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onIrADispositivosVinculados() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF336699)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Ver Dispositivos Vinculados", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onIrAReporteApps(hijosVinculados) },
                enabled = hijosVinculados.isNotEmpty(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6699CC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver historial de uso", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onIrAProgramarRestricciones(hijosVinculados) },
                enabled = hijosVinculados.isNotEmpty(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF99CCFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Programar restricciones", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onIrAControlApps(hijosVinculados) },
                enabled = hijosVinculados.isNotEmpty(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF99CCFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Control de aplicaciones", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onIrAResumenTiempoPantalla(hijosVinculados) },
                enabled = hijosVinculados.isNotEmpty(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF336699)),
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Resumen de Tiempo de Pantalla", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onCerrarSesion()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión", color = Color.White)
            }
        }
    }
}

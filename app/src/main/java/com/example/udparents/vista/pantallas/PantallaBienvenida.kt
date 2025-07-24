package com.example.udparents.vista.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaBienvenida(
    onPadreSeleccionado: () -> Unit,
    onHijoSeleccionado: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF003366)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido a UD Parents",
                fontSize = 24.sp,
                color = Color(0xFF003366)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onPadreSeleccionado,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Soy padre", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onHijoSeleccionado,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006666)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Soy hijo", color = Color.White)
            }
        }
    }
}

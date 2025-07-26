package com.example.udparents.vista.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaBloqueoApp(nombreApp: String) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111)),
        color = Color(0xFF111111)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚫 Acceso Restringido",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "La aplicación \"$nombreApp\" ha sido bloqueada por tus padres.",
                fontSize = 18.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Si necesitas acceso, consulta con ellos.",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

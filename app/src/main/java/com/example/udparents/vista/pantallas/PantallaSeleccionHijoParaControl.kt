package com.example.udparents.vista.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBarDefaults.topAppBarColors
@Composable
fun PantallaSeleccionHijoParaControl(
    listaHijos: List<Pair<String, String>>,
    onHijoSeleccionado: (String) -> Unit,
    onVolver: () -> Unit
) {
    // --- PALETA DE COLORES PARA ESTA PANTALLA (Tema Teal) ---
    val primaryDarkTeal = Color(0xFF004D40) // Verde azulado oscuro muy profundo (fondo principal)
    val primaryLightTeal = Color(0xFF00796B) // Verde azulado primario para TopAppBar
    val accentCyan = Color(0xFF00BCD4) // Cian brillante para botones de acción y texto destacado
    val onPrimaryColor = Color.White // Texto sobre colores primarios
    val surfaceDarkTeal = Color(0xFF263238) // Gris oscuro/casi negro azulado para elementos de lista
    val onSurfaceColor = Color(0xFFB2DFDB) // Texto claro sobre surfaceDarkTeal
    val dividerColor = Color(0xFF4DB6AC) // Tono de verde azulado para divisores

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecciona un Hijo", color = onPrimaryColor) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = onPrimaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryLightTeal, // Color de la barra superior
                    titleContentColor = onPrimaryColor // Color del título
                )
            )
        },
        containerColor = primaryDarkTeal // Color de fondo del Scaffold
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica el padding del Scaffold
                .padding(horizontal = 16.dp, vertical = 8.dp) // Padding adicional para el contenido
        ) {
            Text(
                "Selecciona el perfil del hijo que deseas controlar:",
                style = MaterialTheme.typography.titleLarge,
                color = onPrimaryColor, // Color de texto acorde al tema
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Lista de hijos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceDarkTeal, shape = MaterialTheme.shapes.medium) // Fondo para la lista con esquinas redondeadas
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                listaHijos.forEach { (uid, nombre) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHijoSeleccionado(uid) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = onPrimaryColor // Texto blanco para el nombre del hijo
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ID: $uid", // Mostrar el UID para referencia
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceColor // Texto más claro para el UID
                        )
                    }
                    // Divisor entre elementos, con color que combine
                    if (listaHijos.last() != Pair(uid, nombre)) { // No mostrar divisor después del último elemento
                        Divider(color = dividerColor.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // Espacio antes del botón

            // Botón Volver
            Button(
                onClick = { onVolver() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentCyan, // Fondo en cian brillante
                    contentColor = primaryDarkTeal // Texto en verde azulado oscuro para contraste
                )
            ) {
                Text("Volver al Inicio")
            }
        }
    }
}

package com.example.udparents.vista.pantallas

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.udparents.viewmodel.VistaModeloVinculacion
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun PantallaVinculacionHijo(
    vistaModelo: VistaModeloVinculacion,
    onVolverAlPadre: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var mostrarDialogoExito by remember { mutableStateOf(false) }

    // ✅ Autenticación anónima del hijo si aún no está autenticado
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnFailureListener {
                    mensajeError = "Error al autenticar: ${it.message}"
                }
        }
    }

    if (mostrarDialogoExito) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Vinculación exitosa") },
            text = { Text("El dispositivo ha sido vinculado correctamente.") },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text("Cerrar")
                }
            }
        )

        LaunchedEffect(Unit) {
            delay(2500)
            activity?.finish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Vinculación del dispositivo", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código de vinculación") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del hijo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = edad,
            onValueChange = { edad = it },
            label = { Text("Edad del hijo") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = sexo,
            onValueChange = { sexo = it },
            label = { Text("Sexo del hijo (M/F)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (codigo.isBlank() || nombre.isBlank() || edad.isBlank() || sexo.isBlank()) {
                    mensajeError = "Por favor complete todos los campos."
                    return@Button
                }

                // ✅ Verificamos que el hijo esté autenticado y tenga UID
                val idHijo = FirebaseAuth.getInstance().currentUser?.uid
                if (idHijo.isNullOrEmpty()) {
                    mensajeError = "No se pudo obtener el ID del hijo. Asegúrate de que esté autenticado."
                    return@Button
                }

                vistaModelo.vincularHijoConDatos(
                    codigo,
                    nombre,
                    edad.toIntOrNull() ?: 0,
                    sexo,
                    onExito = {
                        mensajeError = ""
                        mostrarDialogoExito = true
                    },
                    onError = { mensajeError = it }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Vincular")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { onVolverAlPadre() }) {
            Text("Volver al menú principal")
        }

        if (mensajeError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(mensajeError, color = Color.Red)
        }
    }
}

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

    val auth = FirebaseAuth.getInstance()
    var uidHijo by remember { mutableStateOf<String?>(auth.currentUser?.uid) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val nuevoUid = auth.currentUser?.uid
                    uidHijo = nuevoUid
                    vistaModelo.actualizarDispositivoHijo(nuevoUid)
                }
            }
        } else {
            val nuevoUid = auth.currentUser?.uid
            uidHijo = nuevoUid
            vistaModelo.actualizarDispositivoHijo(nuevoUid)
        }
    }



    val codigoVinculacion by vistaModelo.codigoVinculacion.collectAsState()
    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var mostrarDialogoExito by remember { mutableStateOf(false) }

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

        // 🔁 Auto cerrar después de 2.5 segundos
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
            value = codigoVinculacion?.codigo ?: "",
            onValueChange = { vistaModelo.actualizarCodigo(it) },
            label = { Text("Código de vinculación") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = codigoVinculacion?.nombreHijo ?: "",
            onValueChange = { vistaModelo.actualizarNombreHijo(it) },
            label = { Text("Nombre del hijo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = codigoVinculacion?.edadHijo.toString(),
            onValueChange = { vistaModelo.actualizarEdadHijo(it.toIntOrNull() ?: 0) },
            label = { Text("Edad del hijo") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = codigoVinculacion?.sexoHijo ?: "",
            onValueChange = { vistaModelo.actualizarSexoHijo(it)},
            label = { Text("Sexo del hijo (M/F)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (codigoVinculacion?.codigo.isNullOrBlank() || codigoVinculacion?.nombreHijo.isNullOrBlank() || codigoVinculacion?.edadHijo!! <= 0 || codigoVinculacion?.sexoHijo.isNullOrBlank()) {
                    mensajeError = "Por favor complete todos los campos."
                    return@Button
                }

                vistaModelo.vincularHijoConDatos(
                    context = context,
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

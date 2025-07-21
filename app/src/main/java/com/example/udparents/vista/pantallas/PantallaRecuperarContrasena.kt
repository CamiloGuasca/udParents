package com.example.udparents.vista.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.udparents.utilidades.esCorreoValido
import com.example.udparents.viewmodel.VistaModeloUsuario
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun PantallaRecuperarContrasena(
    viewModel: VistaModeloUsuario,
    onRecuperacionEnviada: () -> Unit,
    onVolver: () -> Unit
) {
    val usuario by viewModel.usuario.collectAsState()
    var errorCorreo by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
            Text("Recuperar contraseña", fontSize = 22.sp, color = Color(0xFF003366))

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = usuario.correo,
                onValueChange = {viewModel.actualizarCorreo(it)},
                label = { Text("Correo electrónico") },
                isError = errorCorreo,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorCorreo) {
                Text(
                    "Correo no válido",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.recuperarContrasena { exito, error ->
                        if (exito) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Correo enviado. Revisa tu bandeja.")
                            }
                            onRecuperacionEnviada()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Error: $error")
                            }
                        }
                    }
                },
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
            ) {
                Text(if (cargando) "Enviando..." else "Enviar correo", color = Color.White)
            }


            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Volver al inicio de sesión",
                modifier = Modifier.clickable { onVolver() },
                color = Color(0xFF003366)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

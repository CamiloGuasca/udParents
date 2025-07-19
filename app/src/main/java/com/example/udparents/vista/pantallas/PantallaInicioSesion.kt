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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.udparents.R
import com.example.udparents.viewmodel.VistaModeloUsuario
import kotlinx.coroutines.launch

@Composable
fun PantallaInicioSesion(
    viewModel: VistaModeloUsuario = androidx.lifecycle.viewmodel.compose.viewModel(),
    onIniciarSesionExitoso: () -> Unit,
    onIrARegistro: () -> Unit,
    onOlvidoContrasena: () -> Unit
) {
    val usuario by viewModel.usuario.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Mostrar mensaje si hay error
    val mensaje by viewModel.mensaje.collectAsState()
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
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
            Icon(
                painter = painterResource(id = R.drawable.person_24dp),
                contentDescription = "Icono usuario",
                tint = Color(0xFF003366),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 8.dp)
            )

            Text("Iniciar Sesión", fontSize = 22.sp, color = Color(0xFF003366))

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = usuario.correo,
                onValueChange = { viewModel.actualizarCorreo(it) },
                label = { Text("Correo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = usuario.contrasena,
                onValueChange = { viewModel.actualizarContrasena(it) },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.iniciarSesion(
                        onExito = onIniciarSesionExitoso,
                        onError = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(it)
                            }
                        }
                    )
                },
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
            ) {
                Text(
                    if (cargando) "Cargando..." else "Iniciar sesión",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "¿No tienes cuenta? Regístrate",
                modifier = Modifier.clickable { onIrARegistro() },
                color = Color(0xFF003366)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "¿Olvidaste tu contraseña?",
                modifier = Modifier.clickable { onOlvidoContrasena() },
                color = Color(0xFF003366)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

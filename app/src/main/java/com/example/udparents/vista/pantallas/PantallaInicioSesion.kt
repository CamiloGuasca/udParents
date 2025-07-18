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
import com.example.udparents.utilidades.esCorreoValido
import com.example.udparents.utilidades.esContrasenaValida
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun PantallaInicioSesion(
    onIniciarSesionExitoso: () -> Unit,
    onIrARegistro: () -> Unit,
    onOlvidoContrasena: () -> Unit // ✅ ¡Este ya lo usarás desde la navegación!
) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    var errorCorreo by remember { mutableStateOf(false) }
    var errorContrasena by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var cargando by remember { mutableStateOf(false) }

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
                value = correo,
                onValueChange = {
                    correo = it.trim()
                    errorCorreo = false
                },
                label = { Text("Correo") },
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

            OutlinedTextField(
                value = contrasena,
                onValueChange = {
                    contrasena = it.trim()
                    errorContrasena = false
                },
                label = { Text("Contraseña") },
                isError = errorContrasena,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorContrasena) {
                Text(
                    "La contraseña debe tener al menos 6 caracteres",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    errorCorreo = !esCorreoValido(correo)
                    errorContrasena = !esContrasenaValida(contrasena)

                    if (!errorCorreo && !errorContrasena) {
                        cargando = true
                        FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(correo, contrasena)
                            .addOnCompleteListener { tarea ->
                                cargando = false
                                if (tarea.isSuccessful) {
                                    val usuario = FirebaseAuth.getInstance().currentUser
                                    if (usuario != null && usuario.isEmailVerified) {
                                        onIniciarSesionExitoso()
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Debes verificar tu correo antes de ingresar."
                                            )
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Error: ${tarea.exception?.localizedMessage}"
                                        )
                                    }
                                }
                            }
                    }
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
                modifier = Modifier.clickable { onOlvidoContrasena() }, // ✅ Este es el llamado
                color = Color(0xFF003366)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

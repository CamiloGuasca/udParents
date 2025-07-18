package com.example.udparents.vista.pantallas

import androidx.compose.foundation.Image
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
import com.example.udparents.utilidades.campoNoVacio
import com.example.udparents.utilidades.esCorreoValido
import com.example.udparents.utilidades.esContrasenaValida
import com.example.udparents.repositorio.RepositorioAutenticacion

@Composable
fun PantallaRegistro(
    onRegistroExitoso: () -> Unit,
    onIrAInicioSesion: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    var errorNombre by remember { mutableStateOf(false) }
    var errorCorreo by remember { mutableStateOf(false) }
    var errorContrasena by remember { mutableStateOf(false) }

    var mensajeError by remember { mutableStateOf<String?>(null) }

    val repo = remember { RepositorioAutenticacion() }

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
            Image(
                painter = painterResource(id = R.drawable.person_24dp),
                contentDescription = "Icono usuario",
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 8.dp)
            )

            Text("Registro", fontSize = 22.sp, color = Color(0xFF003366))

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    errorNombre = false
                },
                label = { Text("Nombre") },
                isError = errorNombre,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    errorCorreo = false
                },
                label = { Text("Correo") },
                isError = errorCorreo,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contrasena,
                onValueChange = {
                    contrasena = it
                    errorContrasena = false
                },
                label = { Text("Contraseña (mínimo 6 caracteres)") },
                isError = errorContrasena,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    errorNombre = !campoNoVacio(nombre)
                    errorCorreo = !esCorreoValido(correo)
                    errorContrasena = !esContrasenaValida(contrasena)

                    if (!errorNombre && !errorCorreo && !errorContrasena) {
                        repo.registrarUsuario(
                            nombre.trim(),
                            correo.trim(),
                            contrasena.trim()
                        ) { exito, error ->
                            if (exito) {
                                val user = repo.obtenerUsuarioActual()
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { verifyTask ->
                                        if (verifyTask.isSuccessful) {
                                            mensajeError = "✅ Registro exitoso. Revisa tu correo y verifica tu cuenta para continuar."
                                        } else {
                                            mensajeError = "⚠️ Registro exitoso, pero no se pudo enviar el correo de verificación."
                                        }
                                    }
                            } else {
                                mensajeError = error ?: "Ocurrió un error desconocido"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003366))
            ) {
                Text("Registrarse", color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "¿Ya tienes cuenta? Inicia sesión",
                modifier = Modifier.clickable { onIrAInicioSesion() },
                color = Color(0xFF003366)
            )

            mensajeError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    color = if (it.contains("✅")) Color(0xFF007F00) else Color.Red,
                    fontSize = 14.sp
                )
            }
        }
    }
}

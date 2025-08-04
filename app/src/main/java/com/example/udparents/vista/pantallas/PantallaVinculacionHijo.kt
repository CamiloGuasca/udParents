package com.example.udparents.vista.pantallas

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.udparents.servicio.RegistroUsoService
import com.example.udparents.utilidades.SharedPreferencesUtil
import com.example.udparents.viewmodel.VistaModeloVinculacion
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PantallaVinculacionHijo(
    vistaModelo: VistaModeloVinculacion,
    onVolverAlPadre: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var uidHijo by remember { mutableStateOf(auth.currentUser?.uid) }

    val permisoUsoApps = remember { mutableStateOf(verificarPermisoUsoApps(context)) }
    val permisoAccesibilidad = remember { mutableStateOf(verificarPermisoAccesibilidad(context)) }

    val codigoVinculacion by vistaModelo.codigoVinculacion.collectAsState()
    var mensajeError by remember { mutableStateOf("") }
    var mostrarDialogoPermisoUso by remember { mutableStateOf(false) }
    var mostrarDialogoAccesibilidad by remember { mutableStateOf(false) }
    var mostrarDialogoExito by remember { mutableStateOf(false) }
    var vinculacionIniciada by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && vinculacionIniciada) {
                permisoUsoApps.value = verificarPermisoUsoApps(context)
                permisoAccesibilidad.value = verificarPermisoAccesibilidad(context)

                if (permisoUsoApps.value && permisoAccesibilidad.value) {
                    mostrarDialogoPermisoUso = false
                    mostrarDialogoAccesibilidad = false
                    mostrarDialogoExito = true
                    iniciarServicioRegistroUso(context)
                    coroutineScope.launch {
                        delay(3000)
                        activity?.finish()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener {
                if (it.isSuccessful) {
                    val nuevoUid = auth.currentUser?.uid
                    uidHijo = nuevoUid
                    vistaModelo.actualizarDispositivoHijo(nuevoUid)
                }
            }
        } else {
            uidHijo = auth.currentUser?.uid
            vistaModelo.actualizarDispositivoHijo(uidHijo)
        }
    }

    if (mostrarDialogoPermisoUso && !permisoUsoApps.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Permiso de uso de apps requerido") },
            text = { Text("Activa el permiso para acceder al uso de aplicaciones.") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }) {
                    Text("Abrir ajustes")
                }
            }
        )
    }

    if (mostrarDialogoAccesibilidad && !permisoAccesibilidad.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Activar servicio de bloqueo") },
            text = { Text("Activa el servicio UDParents en accesibilidad para bloquear apps.") },
            confirmButton = {
                TextButton(onClick = {
                    pedirPermisoAccesibilidad(context)
                }) {
                    Text("Ir a accesibilidad")
                }
            }
        )
    }

    if (mostrarDialogoExito) {
        AlertDialog(
            onDismissRequest = { activity?.finish() },
            title = { Text("Vinculación exitosa") },
            text = { Text("El dispositivo fue vinculado correctamente. Cerrando aplicación...") },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text("Cerrar")
                }
            }
        )
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
            value = codigoVinculacion?.edadHijo?.toString() ?: "",
            onValueChange = { vistaModelo.actualizarEdadHijo(it.toIntOrNull() ?: 0) },
            label = { Text("Edad del hijo") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = codigoVinculacion?.sexoHijo ?: "",
            onValueChange = { vistaModelo.actualizarSexoHijo(it) },
            label = { Text("Sexo del hijo (M/F)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (codigoVinculacion?.codigo.isNullOrBlank() ||
                    codigoVinculacion?.nombreHijo.isNullOrBlank() ||
                    codigoVinculacion?.edadHijo!! <= 0 ||
                    codigoVinculacion?.sexoHijo.isNullOrBlank()
                ) {
                    mensajeError = "Por favor completa todos los campos."
                    return@Button
                }

                vistaModelo.vincularHijoConDatos(
                    context = context,
                    onExito = { uidPadre ->
                        mensajeError = ""
                        vinculacionIniciada = true

                        // 💡 NUEVO PASO: Guardar el UID del padre en SharedPreferences
                        SharedPreferencesUtil.guardarUidPadre(context, uidPadre)
                        Log.d("PantallaVinculacionHijo", "UID del padre guardado: $uidPadre")

                        permisoUsoApps.value = verificarPermisoUsoApps(context)
                        permisoAccesibilidad.value = verificarPermisoAccesibilidad(context)

                        if (!permisoUsoApps.value) {
                            mostrarDialogoPermisoUso = true
                        }
                        if (!permisoAccesibilidad.value) {
                            mostrarDialogoAccesibilidad = true
                        }
                        if (permisoUsoApps.value && permisoAccesibilidad.value) {
                            mostrarDialogoExito = true
                            iniciarServicioRegistroUso(context)
                        }
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

fun verificarPermisoUsoApps(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun verificarPermisoAccesibilidad(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(context.packageName)
}

fun pedirPermisoAccesibilidad(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun iniciarServicioRegistroUso(context: Context) {
    val intent = Intent(context, RegistroUsoService::class.java)
    Log.d("PantallaVinculacionHijo", "Iniciando servicio de registro de uso")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

package com.example.udparents.vista.pantallas

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
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
import com.example.udparents.main.MainActivity
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
    val permisoOtorgado = remember { mutableStateOf(verificarPermisoUsoApps(context)) }

    val codigoVinculacion by vistaModelo.codigoVinculacion.collectAsState()
    var mensajeError by remember { mutableStateOf("") }
    var mostrarDialogoPermiso by remember { mutableStateOf(false) }
    var mostrarDialogoExito by remember { mutableStateOf(false) }

    // Bandera para saber si la vinculación se inició y estamos esperando el permiso
    var vinculacionIniciadaYPermisoPendiente by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Cuando la app vuelve de segundo plano (ej. después de dar permisos),
                // verificamos de nuevo el permiso.
                permisoOtorgado.value = verificarPermisoUsoApps(context)

                // Si el permiso se acaba de otorgar Y estábamos esperando por él
                if (permisoOtorgado.value && vinculacionIniciadaYPermisoPendiente) {
                    vinculacionIniciadaYPermisoPendiente = false // Resetea la bandera
                    mostrarDialogoPermiso = false // Oculta el diálogo de permiso si estaba visible
                    mostrarDialogoExito = true // Ahora sí, muestra el diálogo de éxito
                    coroutineScope.launch {
                        delay(3000) // Aumentado a 3 segundo para asegurar que el diálogo se muestre antes de cerrar
                        activity?.finish() // Cierra la actividad principal
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Sesión anónima
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
            uidHijo = auth.currentUser?.uid
            vistaModelo.actualizarDispositivoHijo(uidHijo)
        }
    }

    // Diálogo para pedir permiso de uso de apps
    if (mostrarDialogoPermiso && !permisoOtorgado.value) {
        AlertDialog(
            onDismissRequest = { /* No se puede descartar sin otorgar permiso */ },
            title = { Text("Permiso requerido") },
            text = { Text("Debes conceder acceso al uso de aplicaciones para poder registrar la actividad del hijo.") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    // Después de abrir los ajustes, la app volverá a ON_RESUME,
                    // donde se verificará si el permiso fue otorgado.
                }) {
                    Text("Abrir ajustes")
                }
            }
        )
    }

    // Diálogo de éxito (modificado para cerrar la actividad)
    if (mostrarDialogoExito && permisoOtorgado.value) { // Asegúrate de que el permiso esté otorgado para mostrar este diálogo
        AlertDialog(
            onDismissRequest = {
                // Al descartar el diálogo, cerramos la actividad
                mostrarDialogoExito = false
                activity?.finish()
            },
            title = { Text("Vinculación exitosa") },
            text = { Text("El dispositivo ha sido vinculado correctamente. La aplicación se cerrará.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoExito = false
                    activity?.finish() // Cierra la actividad principal
                }) {
                    Text("Cerrar")
                }
            }
        )
        // Este LaunchedEffect se mantiene para el cierre automático después de un tiempo
        // si el usuario no interactúa con el diálogo.
        LaunchedEffect(Unit) {
            delay(5000) // Aumentado a 5 segundos (5000 milisegundos)
            activity?.finish() // Cierra la actividad principal
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
            value = codigoVinculacion?.edadHijo?.toString() ?: "",
            onValueChange = {
                vistaModelo.actualizarEdadHijo(it.toIntOrNull() ?: 0)
            },
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

                // Vincula primero y luego inicia el servicio si se tiene permiso
                vistaModelo.vincularHijoConDatos(
                    context = context,
                    onExito = {
                        mensajeError = ""
                        if (!permisoOtorgado.value) {
                            mostrarDialogoPermiso = true // Muestra el diálogo para pedir permiso
                            vinculacionIniciadaYPermisoPendiente = true // Establece la nueva bandera
                        } else {
                            // Si el permiso ya está otorgado, muestra el diálogo de éxito
                            mostrarDialogoExito = true
                            // Inicia el servicio de registro de uso
                            (activity as? MainActivity)?.iniciarServicioRegistroUso(context)
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

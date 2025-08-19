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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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

@OptIn(ExperimentalMaterial3Api::class)
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
// Estados derivados para validación en vivo
    val nombreHijo = codigoVinculacion?.nombreHijo.orEmpty()
    val nombreNormalizado = remember(nombreHijo) {
        nombreHijo.trim().replace("\\s+".toRegex(), " ")
    }
    val partes = nombreNormalizado.split(" ")
    val tieneNombreApellido = partes.size >= 2 && partes[0].length >= 2 && partes[1].length >= 2
    val largoOk = nombreNormalizado.replace(" ", "").length >= 10
    val nombreValido = nombreHijo.isNotBlank() && tieneNombreApellido && largoOk


    val edadValida = (codigoVinculacion?.edadHijo ?: 0) in 1..17

    val sexoTexto = codigoVinculacion?.sexoHijo.orEmpty()
    val sexoValido = sexoTexto.trim().equals("m", true)
            || sexoTexto.trim().equals("f", true)
            || sexoTexto.trim().equals("masculino", true)
            || sexoTexto.trim().equals("femenino", true)

    val codigoValido = (codigoVinculacion?.codigo?.length == 6)


    val formularioValido = codigoValido && nombreValido && edadValida && sexoValido

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
            onValueChange = { raw ->
                mensajeError = ""                   // <-- limpiar error global
                val soloDigitos = raw.filter { it.isDigit() }.take(6)
                vistaModelo.actualizarCodigo(soloDigitos)
            },
            label = { Text("Código de vinculación (6 dígitos)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = (codigoVinculacion?.codigo?.length ?: 0) in 1..5, // si hay algo pero menos de 6, error
            supportingText = {
                val len = codigoVinculacion?.codigo?.length ?: 0
                if (len in 1..5) Text("Debe tener 6 dígitos.")
            }
        )


        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = codigoVinculacion?.nombreHijo ?: "",
            onValueChange = {
                mensajeError = ""                   // <-- limpiar error global
                vistaModelo.actualizarNombreHijo(it)
            },
            label = { Text("Nombre y apellido del hijo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            isError = (codigoVinculacion?.nombreHijo?.isNotBlank() == true) && !nombreValido,
            supportingText = {
                if ((codigoVinculacion?.nombreHijo?.isNotBlank() == true) && !nombreValido) {
                    Text("Escribe nombre y apellido (mín. 10 letras en total).")
                }
            }
        )


        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = codigoVinculacion?.edadHijo?.takeIf { it > 0 }?.toString() ?: "",
            onValueChange = { txt ->
                mensajeError = ""
                val valor = txt.toIntOrNull()
                if (valor == null) {
                    // Si no es número, manda 0 para que marque error
                    vistaModelo.actualizarEdadHijo(0)
                } else {
                    vistaModelo.actualizarEdadHijo(valor)
                }
            },
            label = { Text("Edad del hijo (1–17)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = (codigoVinculacion?.edadHijo ?: 0) !in 1..17,
            supportingText = {
                if ((codigoVinculacion?.edadHijo ?: 0) !in 1..17) {
                    Text("Ingresa una edad válida entre 1 y 17.")
                }
            }
        )


        Spacer(modifier = Modifier.height(8.dp))
        var abierto by remember { mutableStateOf(false) }
        val opcionesSexo = listOf("M", "F")

        ExposedDropdownMenuBox(
            expanded = abierto,
            onExpandedChange = { abierto = !abierto },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = codigoVinculacion?.sexoHijo ?: "",
                onValueChange = { /* no escribir libre; usamos selección */ },
                label = { Text("Sexo del hijo (M/F)") },
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                isError = sexoTexto.isNotBlank() && !sexoValido,
                supportingText = {
                    if (sexoTexto.isBlank()) {
                        Text("Selecciona M o F.")
                    } else if (!sexoValido) {
                        Text("Valor inválido. Selecciona M o F.")
                    }
                }
            )
            ExposedDropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false }
            ) {
                opcionesSexo.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion) },
                        onClick = {
                            mensajeError = ""
                            vistaModelo.actualizarSexoHijo(opcion)
                            abierto = false
                        }
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                vistaModelo.vincularHijoConDatos(
                    context = context,
                    onExito = { uidPadre ->
                        mensajeError = ""
                        vinculacionIniciada = true

                        SharedPreferencesUtil.guardarUidPadre(context, uidPadre)
                        Log.d("PantallaVinculacionHijo", "UID del padre guardado: $uidPadre")

                        permisoUsoApps.value = verificarPermisoUsoApps(context)
                        permisoAccesibilidad.value = verificarPermisoAccesibilidad(context)

                        if (!permisoUsoApps.value) mostrarDialogoPermisoUso = true
                        if (!permisoAccesibilidad.value) mostrarDialogoAccesibilidad = true
                        if (permisoUsoApps.value && permisoAccesibilidad.value) {
                            mostrarDialogoExito = true
                            iniciarServicioRegistroUso(context)
                        }
                    },
                    onError = { mensajeError = it }
                )
            },

            enabled = formularioValido,         // <<--- usa tu validación global
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

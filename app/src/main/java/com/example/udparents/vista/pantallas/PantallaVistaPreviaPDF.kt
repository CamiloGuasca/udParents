package com.example.udparents.vista.pantallas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVistaPreviaPDF(
    activity: Activity,
    nombreArchivo: String,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val downloadsDir = context.getExternalFilesDir(null)
    val file = File(downloadsDir, "$nombreArchivo.pdf")

    LaunchedEffect(Unit) {
        // Al entrar a la pantalla, intenta abrir el PDF
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(file), "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    BackHandler { onCerrar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vista previa del PDF") },
                actions = {
                    TextButton(onClick = onCerrar) {
                        Text("Cerrar", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Se generó el PDF exitosamente.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "application/pdf")
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }) {
                Text("Ver PDF")
            }
        }
    }
}

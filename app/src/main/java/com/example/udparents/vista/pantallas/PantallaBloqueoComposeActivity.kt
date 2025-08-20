package com.example.udparents.vista.pantallas

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.udparents.tema.UdParentsTheme
import kotlinx.coroutines.delay

class PantallaBloqueoComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mostrar encima del lockscreen y encender la pantalla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val nombreApp = intent.getStringExtra("nombreApp") ?: "Esta aplicación"
        val motivoBloqueo = intent.getStringExtra("motivoBloqueo")
            ?: "Bloqueada por configuración de UDParents"
        val bloqueoPorPermiso = intent.getBooleanExtra("bloqueoPermiso", false)

        // Bloquear botón atrás
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op */ }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        setContent {
            UdParentsTheme {
                if (bloqueoPorPermiso) {
                    // Bloqueo especial por PERMISOS: se queda hasta que se restauren
                    PantallaBloqueoPermiso(
                        motivo = motivoBloqueo,
                        onAbrirAccesibilidad = {
                            startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        onAbrirUso = {
                            startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )

                    // Auto-cierre cuando ambos permisos estén OK (poll cada 1s)
                    val ctx = this
                    LaunchedEffect(Unit) {
                        while (true) {
                            val accOk = isAccessibilityServiceEnabled(ctx)
                            val usoOk = isUsageAccessGranted(ctx)
                            if (accOk && usoOk) {
                                finish()
                                break
                            }
                            delay(1000)
                        }
                    }
                } else {
                    // Bloqueo normal (límite/horario/app): usa TU PantallaBloqueoApp existente
                    PantallaBloqueoApp(
                        nombreApp = nombreApp,
                        motivoBloqueo = motivoBloqueo
                    )

                    LaunchedEffect(Unit) {
                        delay(5000)
                        val startMain = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(startMain)
                        finish()
                    }
                }
            }
        }
    }

    /** Cadena exacta que aparece en ENABLED_ACCESSIBILITY_SERVICES para este servicio */
    private fun componenteServicioAccesibilidad(context: Context): String {
        val serviceClass = "com.example.udparents.servicio.BloqueoAccessibilityService"
        return "${context.packageName}/$serviceClass"
    }

    /** Verifica si el servicio de accesibilidad de la app está habilitado */
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = componenteServicioAccesibilidad(context)
        return enabled.split(':').any { it.equals(target, ignoreCase = true) }
    }

    /** Verifica si el permiso de "Acceso a uso" está concedido */
    private fun isUsageAccessGranted(context: Context): Boolean {
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
}

/** UI para bloqueo por permisos faltantes (Accesibilidad / Acceso a uso) */
@Composable
fun PantallaBloqueoPermiso(
    motivo: String,
    onAbrirAccesibilidad: () -> Unit,
    onAbrirUso: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFB00020)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Permiso requerido",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Para continuar, activa el permiso indicado:\n$motivo",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAbrirAccesibilidad,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) { Text("Accesibilidad", color = Color(0xFFB00020)) }

                Button(
                    onClick = onAbrirUso,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) { Text("Acceso a uso", color = Color(0xFFB00020)) }
            }
        }
    }
}

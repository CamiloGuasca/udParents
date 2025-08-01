package com.example.udparents.vista.pantallas

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import com.example.udparents.tema.UdParentsTheme

class PantallaBloqueoComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener el nombre de la app y el motivo del bloqueo del Intent
        val nombreApp = intent.getStringExtra("nombreApp") ?: "Esta aplicación"
        // Se añade la línea para obtener el motivo del bloqueo.
        // Si no se encuentra, se usará un mensaje por defecto.
        val motivoBloqueo = intent.getStringExtra("motivoBloqueo") ?: "bloqueada por la configuración de UD Parents"

        // Bloquear botón atrás
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Nada, se bloquea el botón atrás
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        // Opcional: cerrar app bloqueada (requiere permiso especial)
        val packageName = intent.getStringExtra("paqueteBloqueado")
        if (packageName != null) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
        }

        // Cierra la pantalla de bloqueo luego de 5 segundos y lleva al launcher
        Handler(Looper.getMainLooper()).postDelayed({
            val startMain = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(startMain)
            finish()
        }, 5000)

        // Mostrar pantalla visual
        setContent {
            UdParentsTheme {
                // Se actualiza la llamada al Composable para pasar el motivo del bloqueo
                PantallaBloqueoApp(
                    nombreApp = nombreApp,
                    motivoBloqueo = motivoBloqueo // <<-- Se añade este parámetro
                )
            }
        }
    }
}

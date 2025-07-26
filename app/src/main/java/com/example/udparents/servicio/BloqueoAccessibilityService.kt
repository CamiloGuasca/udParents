package com.example.udparents.servicio

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.udparents.repositorio.RepositorioApps
import com.example.udparents.vista.pantallas.PantallaBloqueoComposeActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BloqueoAccessibilityService : AccessibilityService() {

    private var appActual: String? = null
    private val scope = CoroutineScope(Dispatchers.IO) // ⬅️ Corrutina para operaciones suspendidas

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val paqueteActual = event.packageName?.toString() ?: return

            if (paqueteActual != appActual) {
                appActual = paqueteActual
                Log.d("BloqueoAccessibility", "📱 App detectada: $paqueteActual")

                val uidHijo = FirebaseAuth.getInstance().currentUser?.uid ?: return
                val repositorio = RepositorioApps()

                scope.launch {
                    val bloqueada = repositorio.estaAppBloqueada(uidHijo, paqueteActual)
                    if (bloqueada) {
                        val nombreApp = obtenerNombreApp(packageManager, paqueteActual)

                        val intent = Intent(this@BloqueoAccessibilityService, PantallaBloqueoComposeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("nombreApp", nombreApp)
                            putExtra("paqueteBloqueado", paqueteActual)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w("BloqueoAccessibility", "🛑 Servicio interrumpido")
    }

    override fun onServiceConnected() {
        Log.d("BloqueoAccessibility", "✅ Servicio de accesibilidad conectado")

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
    }

    private fun obtenerNombreApp(pm: android.content.pm.PackageManager, packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}

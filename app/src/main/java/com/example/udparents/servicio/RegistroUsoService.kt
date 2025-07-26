package com.example.udparents.servicio

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.udparents.R
import com.example.udparents.repositorio.RepositorioApps
import com.example.udparents.vista.pantallas.PantallaBloqueoComposeActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager

class RegistroUsoService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var tareaMonitoreo: Job? = null
    private val intervaloChequeo = 3000L // cada 3 segundos
    private var paqueteBloqueadoActual: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RegistroUsoService", "✅ Servicio iniciado correctamente")
        Log.d("RegistroUsoService", "🧬 Servicio sigue corriendo tras cierre")
        mostrarNotificacion()

        tareaMonitoreo = scope.launch {
            while (isActive) {
                try {
                    verificarAppEnUso()
                } catch (e: Exception) {
                    Log.e("RegistroUsoService", "❌ Error monitoreando apps: ${e.message}")
                }
                delay(intervaloChequeo)
            }
        }

        return START_STICKY
    }

    private suspend fun verificarAppEnUso() {
        val context = applicationContext
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val ahora = System.currentTimeMillis()
        val hace10Segundos = ahora - 10_000

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            hace10Segundos,
            ahora
        )

        if (stats.isNullOrEmpty()) return

        val appEnUso = stats.maxByOrNull { it.lastTimeUsed } ?: return
        val paqueteActual = appEnUso.packageName
        val uidHijo = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val repositorio = RepositorioApps()

        val bloqueada = repositorio.estaAppBloqueada(uidHijo, paqueteActual)
        if (bloqueada) {
            if (paqueteActual != paqueteBloqueadoActual) {
                paqueteBloqueadoActual = paqueteActual
                val nombreApp = obtenerNombreApp(context, paqueteActual)
                Log.d("RegistroUsoService", "🔒 App bloqueada detectada: $nombreApp ($paqueteActual)")

                val intent = Intent(context, PantallaBloqueoComposeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("nombreApp", nombreApp)
                    putExtra("paqueteBloqueado", paqueteActual)
                }
                context.startActivity(intent)
            }
        } else {
            if (paqueteBloqueadoActual != null) {
                Log.d("RegistroUsoService", "✅ App desbloqueada: $paqueteActual")
            }
            paqueteBloqueadoActual = null
        }
    }

    private fun obtenerNombreApp(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w("RegistroUsoService", "🛑 Servicio detenido inesperadamente")
        scope.cancel()
        tareaMonitoreo?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "uso_app_channel",
                "Registro de uso de aplicaciones",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente para el monitoreo de apps en segundo plano."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    private fun mostrarNotificacion() {
        crearCanalNotificacion()

        val notificacion: Notification = NotificationCompat.Builder(this, "uso_app_channel")
            .setContentTitle("UdParents")
            .setContentText("Monitoreando el uso de aplicaciones...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(1, notificacion)
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartService = Intent(applicationContext, RegistroUsoService::class.java)
        restartService.setPackage(packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartService)
        } else {
            applicationContext.startService(restartService)
        }
    }
}

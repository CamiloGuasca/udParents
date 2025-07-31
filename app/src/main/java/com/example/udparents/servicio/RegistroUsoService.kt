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
import com.example.udparents.utilidades.RegistroUsoApps
import com.example.udparents.vista.pantallas.PantallaBloqueoComposeActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import java.util.Calendar

class RegistroUsoService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var tareaMonitoreo: Job? = null
    private var tareaRegistroUso: Job? = null
    private val intervaloChequeoAppEnUso = 3000L // 3 segundos para el chequeo de bloqueo y el incremento "en vivo"
    private val intervaloRegistroUso = 30 * 1000L // <-- Asegúrate que sea 30 segundos aquí
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
                delay(intervaloChequeoAppEnUso)
            }
        }

        tareaRegistroUso = scope.launch {
            while (isActive) {
                try {
                    Log.d("RegistroUsoService", "📝 Iniciando registro periódico de uso de apps en Firebase (barrido general)...")
                    RegistroUsoApps.registrarUsoAplicaciones(applicationContext)
                    Log.d("RegistroUsoService", "✅ Registro periódico de uso de apps finalizado.")
                } catch (e: Exception) {
                    Log.e("RegistroUsoService", "❌ Error registrando uso de apps: ${e.message}", e)
                }
                delay(intervaloRegistroUso)
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

        if (stats.isNullOrEmpty()) {
            Log.d("RegistroUsoService", "No se encontraron estadísticas de uso recientes.")
            return
        }

        val appEnUso = stats.maxByOrNull { it.lastTimeUsed } ?: run {
            Log.d("RegistroUsoService", "No se pudo determinar la aplicación en uso más reciente.")
            return
        }
        val paqueteActual = appEnUso.packageName
        val uidHijo = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w("RegistroUsoService", "UID del hijo no disponible, no se puede verificar bloqueo.")
            return
        }
        val repositorio = RepositorioApps()

        val nombreAppActual = obtenerNombreApp(context, paqueteActual)

        // LÓGICA CLAVE: Incrementar el uso de la aplicación activa cada 3 segundos
        repositorio.incrementarUsoAplicacion(uidHijo, paqueteActual, nombreAppActual, intervaloChequeoAppEnUso)

        // Ahora, obtenemos el tiempo de uso *después* de haberlo incrementado
        val tiempoUsoActual = repositorio.obtenerUsoAppDelDia(uidHijo, paqueteActual)

        val bloqueadaManual = repositorio.estaAppBloqueada(uidHijo, paqueteActual)
        val tiempoLimite = repositorio.obtenerLimiteApp(uidHijo, paqueteActual)
        val bloqueadaPorLimite = tiempoLimite > 0L && tiempoUsoActual >= tiempoLimite

        val debeBloquear = bloqueadaManual || bloqueadaPorLimite

        if (debeBloquear) {
            if (paqueteActual != paqueteBloqueadoActual) {
                paqueteBloqueadoActual = paqueteActual
                val motivoBloqueo = when {
                    bloqueadaManual -> "Bloqueo manual"
                    bloqueadaPorLimite -> "Límite de tiempo excedido"
                    else -> "Desconocido"
                }
                Log.d("RegistroUsoService", "🔒 App bloqueada detectada: $nombreAppActual ($paqueteActual) - Motivo: $motivoBloqueo")

                val intent = Intent(context, PantallaBloqueoComposeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("nombreApp", nombreAppActual)
                    putExtra("paqueteBloqueado", paqueteActual)
                }
                context.startActivity(intent)
            }
        } else {
            if (paqueteBloqueadoActual != null) {
                Log.d("RegistroUsoService", "✅ App desbloqueada o cambio de app: $paqueteActual")
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
        tareaRegistroUso?.cancel()
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

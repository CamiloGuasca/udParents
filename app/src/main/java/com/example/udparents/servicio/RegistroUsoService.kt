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
import com.example.udparents.utilidades.RegistroUsoApps // Asegúrate de que esta importación sea correcta
import com.example.udparents.vista.pantallas.PantallaBloqueoComposeActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager

class RegistroUsoService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var tareaMonitoreo: Job? = null
    private var tareaRegistroUso: Job? = null // Nueva tarea para el registro de uso en Firebase
    private val intervaloChequeoAppEnUso = 3000L // 3 segundos para el chequeo de bloqueo
    private val intervaloRegistroUso = 3 * 1000L // 30 segundos para registrar el uso en Firebase (antes 5 minutos)
    private var paqueteBloqueadoActual: String? = null // Para evitar relanzar la pantalla de bloqueo repetidamente

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RegistroUsoService", "✅ Servicio iniciado correctamente")
        Log.d("RegistroUsoService", "🧬 Servicio sigue corriendo tras cierre")
        mostrarNotificacion()

        // Monitoreo de apps en uso para bloqueo (alta frecuencia)
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

        // Registro periódico del uso de apps en Firebase (frecuencia media)
        tareaRegistroUso = scope.launch {
            while (isActive) {
                try {
                    Log.d("RegistroUsoService", "📝 Iniciando registro periódico de uso de apps en Firebase...")
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
        val hace10Segundos = ahora - 10_000 // Consulta un rango corto para obtener la app más reciente

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, // Consulta el uso desde el inicio del día
            hace10Segundos, // Desde hace 10 segundos
            ahora // Hasta ahora
        )

        if (stats.isNullOrEmpty()) {
            Log.d("RegistroUsoService", "No se encontraron estadísticas de uso recientes.")
            return
        }

        // Obtiene la aplicación que ha estado más recientemente en primer plano
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

        // 1. Verificar bloqueo manual (si el padre la bloqueó directamente)
        val bloqueadaManual = repositorio.estaAppBloqueada(uidHijo, paqueteActual)

        // 2. Verificar bloqueo por límite de tiempo
        // Necesitamos el límite establecido y el uso acumulado de la app para el día
        val tiempoLimite = repositorio.obtenerLimiteApp(uidHijo, paqueteActual)
        val tiempoUsoActual = repositorio.obtenerUsoAppDelDia(uidHijo, paqueteActual)

        // La app está bloqueada por límite si hay un límite establecido (> 0)
        // Y el tiempo de uso actual es igual o ha excedido ese límite
        val bloqueadaPorLimite = tiempoLimite > 0L && tiempoUsoActual >= tiempoLimite

        // Determinar si la aplicación debe ser bloqueada por cualquier razón
        val debeBloquear = bloqueadaManual || bloqueadaPorLimite

        if (debeBloquear) {
            // Solo lanza la pantalla de bloqueo si la app es diferente a la que ya está bloqueada
            // Esto evita el bucle de relanzamiento de la misma pantalla.
            if (paqueteActual != paqueteBloqueadoActual) {
                paqueteBloqueadoActual = paqueteActual
                val nombreApp = obtenerNombreApp(context, paqueteActual)
                val motivoBloqueo = when {
                    bloqueadaManual -> "Bloqueo manual"
                    bloqueadaPorLimite -> "Límite de tiempo excedido"
                    else -> "Desconocido" // En caso de que ninguna condición anterior se cumpla (poco probable)
                }
                Log.d("RegistroUsoService", "🔒 App bloqueada detectada: $nombreApp ($paqueteActual) - Motivo: $motivoBloqueo")

                val intent = Intent(context, PantallaBloqueoComposeActivity::class.java).apply {
                    // FLAG_ACTIVITY_NEW_TASK es crucial para lanzar una actividad desde un contexto que no es una actividad
                    // FLAG_ACTIVITY_CLEAR_TOP asegura que si la actividad ya está en la pila, se suba a la cima y se eliminen las de arriba.
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("nombreApp", nombreApp)
                    putExtra("paqueteBloqueado", paqueteActual)
                }
                context.startActivity(intent)
            }
        } else {
            // Si la app actual no está bloqueada y teníamos una app bloqueada previamente,
            // significa que el bloqueo se levantó o la app cambió.
            if (paqueteBloqueadoActual != null) {
                Log.d("RegistroUsoService", "✅ App desbloqueada o cambio de app: $paqueteActual")
            }
            paqueteBloqueadoActual = null
        }
    }

    // --- Las siguientes funciones se mantienen EXACTAMENTE como las tenías ---

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
        tareaRegistroUso?.cancel() // ¡Importante: cancelar la nueva tarea también!
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
        // Reinicia el servicio si la tarea es eliminada para asegurar el monitoreo continuo.
        val restartService = Intent(applicationContext, RegistroUsoService::class.java)
        restartService.setPackage(packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartService)
        } else {
            applicationContext.startService(restartService)
        }
    }
}

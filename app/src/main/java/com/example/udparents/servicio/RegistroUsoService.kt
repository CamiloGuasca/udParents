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
import com.example.udparents.modelo.BloqueoRegistro
import com.example.udparents.repositorio.RepositorioApps
import com.example.udparents.repositorio.RepositorioBloqueos
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
    private val intervaloRegistroUso = 30 * 1000L // 30 segundos para registrar el uso en Firebase (barrido general)
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

        var debeBloquear = false
        var motivoBloqueo = "Desconocido"

        // 1. Verificar bloqueo manual
        val bloqueadaManual = repositorio.estaAppBloqueada(uidHijo, paqueteActual)
        if (bloqueadaManual) {
            debeBloquear = true
            motivoBloqueo = "Bloqueo manual"
        }

        // 2. Verificar bloqueo por límite de tiempo (solo si no está ya bloqueada manualmente)
        if (!debeBloquear) {
            val tiempoLimite = repositorio.obtenerLimiteApp(uidHijo, paqueteActual)
            val bloqueadaPorLimite = tiempoLimite > 0L && tiempoUsoActual >= tiempoLimite
            if (bloqueadaPorLimite) {
                debeBloquear = true
                motivoBloqueo = "Límite de tiempo excedido"
            }
        }

        // 3. Verificar bloqueo por horario (solo si no está ya bloqueada por las razones anteriores)
        if (!debeBloquear) {
            val restricciones = repositorio.obtenerRestriccionesHorario(uidHijo)
            val calendar = Calendar.getInstance()
            // Calendar.DAY_OF_WEEK usa: Domingo=1, Lunes=2, ..., Sábado=7
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val currentTimeMillisOfDay = (calendar.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000) +
                    (calendar.get(Calendar.MINUTE) * 60 * 1000) +
                    (calendar.get(Calendar.SECOND) * 1000)

            for (restriccion in restricciones) {
                if (restriccion.isEnabled &&
                    (restriccion.packageName == paqueteActual || restriccion.packageName == "ALL_APPS") &&
                    restriccion.daysOfWeek.contains(currentDayOfWeek)
                ) {
                    // Lógica para comprobar si la hora actual está dentro de la franja de bloqueo
                    if (restriccion.startTimeMillis < restriccion.endTimeMillis) {
                        // Horario normal (ej. 08:00 - 17:00)
                        if (currentTimeMillisOfDay >= restriccion.startTimeMillis &&
                            currentTimeMillisOfDay < restriccion.endTimeMillis) {
                            debeBloquear = true
                            motivoBloqueo = "Restricción por horario"
                            break
                        }
                    } else {
                        // Horario que cruza la medianoche (ej. 22:00 - 06:00)
                        // Si la hora actual es mayor o igual a la hora de inicio (ej. 22:00)
                        // O si la hora actual es menor que la hora de fin (ej. 06:00)
                        if (currentTimeMillisOfDay >= restriccion.startTimeMillis ||
                            currentTimeMillisOfDay < restriccion.endTimeMillis) {
                            debeBloquear = true
                            motivoBloqueo = "Restricción por horario"
                            break
                        }
                    }
                }
            }
        }

        if (debeBloquear) {
            if (paqueteActual != paqueteBloqueadoActual) {
                paqueteBloqueadoActual = paqueteActual
                Log.d("RegistroUsoService", "🔒 App bloqueada detectada: $nombreAppActual ($paqueteActual) - Motivo: $motivoBloqueo")

                // ✅ LÓGICA AGREGADA: Registrar el intento de acceso bloqueado
                val repoBloqueos = RepositorioBloqueos()
                val bloqueoRegistro = BloqueoRegistro(
                    uidHijo = uidHijo,
                    nombrePaquete = paqueteActual, // CORREGIDO para que coincida con la clase BloqueoRegistro
                    nombreApp = nombreAppActual,
                    razon = motivoBloqueo
                )
                scope.launch {
                    try {
                        repoBloqueos.registrarBloqueo(uidHijo, bloqueoRegistro)
                        Log.d("RegistroUsoService", "✅ Intento de bloqueo registrado en Firebase.")
                    } catch (e: Exception) {
                        Log.e("RegistroUsoService", "❌ Error al registrar bloqueo: ${e.message}", e)
                    }
                }

                val intent = Intent(context, PantallaBloqueoComposeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("nombreApp", nombreAppActual)
                    putExtra("paqueteBloqueado", paqueteActual)
                    putExtra("motivoBloqueo", motivoBloqueo) // Opcional: pasar el motivo a la pantalla de bloqueo
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

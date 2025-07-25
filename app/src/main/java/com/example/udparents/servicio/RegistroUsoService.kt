    package com.example.udparents.servicio

    import android.app.Service
    import android.content.Intent
    import android.os.IBinder
    import android.app.Notification
    import android.app.NotificationChannel
    import android.app.NotificationManager
    import android.os.Build
    import android.util.Log
    import androidx.core.app.NotificationCompat
    import com.example.udparents.R
    import com.example.udparents.utilidades.RegistroUsoApps
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.cancel
    import kotlinx.coroutines.launch

    class RegistroUsoService : Service() {

        private val scope = CoroutineScope(Dispatchers.IO + Job())

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            // Muestra la notificación para el Foreground Service
            mostrarNotificacion()

            // Lanza la corrutina para registrar el uso de aplicaciones.
            // Si el servicio debe ejecutarse continuamente, no llames a stopSelf() aquí.
            // Si es una tarea puntual, stopSelf() está bien, pero reconsidera el uso de FGS.
            scope.launch {
                try {
                    // Aquí, idealmente, RegistroUsoApps.registrarUsoAplicaciones debería
                    // ser una función que se ejecute periódicamente o que sea activada por eventos.
                    // Si solo lo llamas una vez y luego el servicio se detiene,
                    // no tiene mucho sentido que sea un Foreground Service continuo.
                    RegistroUsoApps.registrarUsoAplicaciones(applicationContext)
                    // Si necesitas que esto se ejecute periódicamente, podrías usar un loop con delay
                    // o un WorkManager para programar la tarea.
                } catch (e: Exception) {
                    Log.e("RegistroUsoService", "Error registrando uso: ${e.message}")
                }
                // Importante: Solo llama stopSelf() si el servicio ha completado su propósito
                // y no necesita seguir ejecutándose en primer plano.
                // Si quieres un monitoreo continuo, ¡elimina la línea de abajo!
                // stopSelf()
            }

            // START_STICKY es más apropiado si el servicio debe permanecer ejecutándose
            // y ser reiniciado por el sistema si se termina. START_NOT_STICKY si es una tarea corta.
            return START_STICKY
        }


        override fun onDestroy() {
            super.onDestroy()
            Log.d("RegistroUsoService", "Servicio detenido")
            // Asegúrate de cancelar el scope de la corrutina cuando el servicio se destruye
            scope.cancel()
        }

        override fun onBind(intent: Intent?): IBinder? = null

        private fun crearCanalNotificacion() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    "uso_app_channel",
                    "Registro de uso de aplicaciones",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notificación para el servicio de registro de uso de aplicaciones."
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(canal)
            }
        }

        private fun mostrarNotificacion() {
            crearCanalNotificacion()

            val notificacion: Notification = NotificationCompat.Builder(this, "uso_app_channel")
                .setContentTitle("UdParents")
                .setContentText("Registrando uso de apps...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Usa un ícono propio de tu app, no uno genérico de Android
                .setOngoing(true) // Hace la notificación persistente
                .build()

            startForeground(1, notificacion)
        }
    }
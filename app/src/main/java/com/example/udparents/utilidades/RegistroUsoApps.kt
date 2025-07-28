package com.example.udparents.utilidades
import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import android.util.Log
import com.example.udparents.modelo.AppUso
import com.example.udparents.repositorio.RepositorioApps
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.app.usage.UsageStats
import java.util.Calendar

object RegistroUsoApps {

    private const val TAG = "RegistroUsoAppsDEBUG"

    suspend fun registrarUsoAplicaciones(context: Context) {
        val uidHijo = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w(TAG, "UID del hijo no disponible, no se puede registrar el uso.")
            return
        }
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val ahora = System.currentTimeMillis()

        val calendar = Calendar.getInstance().apply {
            timeInMillis = ahora
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMillis = calendar.timeInMillis

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            todayStartMillis,
            ahora
        )

        // *** LOGS DE DEPURACIÓN CRÍTICOS ***
        if (stats.isNullOrEmpty()) {
            Log.w(TAG, "Sin permisos de uso o sin datos de uso para el día actual. `usageStatsManager.queryUsageStats` devolvió vacío.")
            if (!tienePermisoUsageStats(context)) {
                Log.e(TAG, "¡ALERTA! El permiso de ACCESO A DATOS DE USO no está concedido. Por favor, ve a Configuración > Acceso especial > Acceso a datos de uso y habilítalo para UdParents.")
            }
            return
        } else {
            Log.d(TAG, "📊 Se encontraron ${stats.size} UsageStats para el día. Procesando...")
        }

        val repositorio = RepositorioApps()

        withContext(Dispatchers.IO) {
            var appsProcesadas = 0
            var appsRegistradas = 0
            for (app in stats) {
                val tiempoUso = app.totalTimeInForeground
                val packageName = app.packageName

                // Ignorar la propia aplicación para evitar bucles o registros innecesarios
                if (packageName == context.packageName) {
                    Log.d(TAG, "➡️ Ignorando la propia aplicación: $packageName")
                    continue
                }

                // *** LOG DE FILTRADO - Tiempo de uso ***
                if (tiempoUso <= 0) {
                    Log.d(TAG, "➡️ Ignorando app sin tiempo en primer plano: $packageName (Tiempo: ${tiempoUso} ms)")
                    continue // Salta apps sin tiempo de uso en primer plano
                }

                // *** LOG DE FILTRADO - Aplicaciones del sistema ***
                // La lógica de esAplicacionSistema ha sido ajustada para este caso
                if (esAplicacionSistema(context, packageName)) {
                    Log.d(TAG, "➡️ Ignorando app del sistema: $packageName")
                    continue // Salta apps del sistema
                }

                // Si llega aquí, la app debería registrarse
                val nombreApp = obtenerNombreApp(context, packageName)
                Log.d(TAG, "✅ Lista para registrar: $nombreApp ($packageName), Tiempo: ${tiempoUso} ms")

                val appUso = AppUso(
                    nombrePaquete = packageName,
                    nombreApp = nombreApp,
                    fechaUso = todayStartMillis,
                    tiempoUso = tiempoUso
                )
                try {
                    repositorio.registrarUsoAplicacion(uidHijo, appUso)
                    appsRegistradas++
                } catch (e: Exception) {
                    Log.e(TAG, "❌ ERROR al registrar ${nombreApp} ($packageName) en Firebase: ${e.message}", e)
                }
                appsProcesadas++
            }
            Log.d(TAG, "🏁 Proceso de registro de apps finalizado. Se procesaron ${appsProcesadas} y se registraron ${appsRegistradas} aplicaciones válidas.")
        }
    }

    private fun obtenerNombreApp(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName // Si no se encuentra el nombre, usar el paquete como fallback
        }
    }

    private fun esAplicacionSistema(context: Context, packageName: String): Boolean {
        // Lista de paquetes que se EXCLUIRÁN explícitamente del monitoreo.
        // Aquí debes incluir aplicaciones que NO quieres ver nunca en el historial,
        // por ser puramente del sistema y sin interacción directa del usuario.
        // Ejemplos: "android" (el propio sistema), "com.google.android.gms" (servicios de Google Play).
        val excludedPackages = setOf(
            "android",
            "com.google.android.gms",
            "com.android.providers.media",
            "com.android.systemui",
            "com.google.android.packageinstaller",
            "com.google.android.permissioncontroller", // Controlador de permisos
            "com.android.phone", // Aplicación de teléfono (llamadas, etc.)
            "com.android.providers.telephony", // Proveedor de telefonía
            "com.android.settings", // Configuración
            "com.samsung.android.app.launcher", // Launcher de Samsung
            "com.google.android.apps.restore", // Restauración de datos
            "com.google.android.networkstack", // Componentes de red
            "com.google.android.networkstack.tethering", // Componentes de red (tethering)
            "com.sec.android.app.samsungapps", // Samsung Galaxy Store
            "com.google.android.webview", // Componente WebView de Android
            "com.sec.imsservice", // Servicio de IMS (VoLTE/VoWiFi)
            "com.samsung.android.honeyboard" // Teclado Samsung (si no lo quieres monitorear)
            // Agrega más paquetes aquí si encuentras otras apps del sistema que no te interesan
        )

        if (excludedPackages.contains(packageName)) {
            Log.d(TAG, "esAplicacionSistema: $packageName está en la lista de exclusión explícita.")
            return true
        }

        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)

            // La lógica ahora es: si NO está en la lista de exclusión explícita, entonces
            // DEBEMOS registrarla, independientemente de si es SYSTEM_APP o UPDATED_SYSTEM_APP.
            // Solo nos interesan las apps que el usuario interactúa.

            // La única excepción sería si una app es una PURE SYSTEM APP Y tiene un nombre de paquete
            // que NO es de interés para el monitoreo de uso del usuario (ej: servicios en segundo plano).
            // Pero como ya tenemos la lista de exclusión explícita, esa es la principal herramienta.
            // Si quieres monitorear la cámara, calendario, etc., entonces esta función
            // debe devolver 'false' para ellos.

            // Por lo tanto, si no está en excludedPackages, simplemente retornamos false aquí.
            // La única condición adicional que podrías agregar es si el tiempoInForeground es 0,
            // pero eso ya se maneja fuera de esta función.

            false // Si no está en la lista de exclusión, no la consideramos "del sistema a ignorar".

        } catch (e: PackageManager.NameNotFoundException) {
            // Si el paquete no se encuentra, NO asumimos que es del sistema.
            // Esto permite que el registro intente procesarla.
            Log.w(TAG, "esAplicacionSistema: Paquete no encontrado al verificar si es del sistema: $packageName. NO lo consideramos app del sistema para fines de registro. Error: ${e.message}")
            false // NO la consideramos del sistema, para que pueda ser registrada
        } catch (e: Exception) {
            Log.e(TAG, "esAplicacionSistema: Error inesperado al verificar paquete $packageName: ${e.message}", e)
            true // En caso de cualquier otro error grave, es más seguro no registrarla.
        }
    }


    fun tienePermisoUsageStats(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
package com.example.udparents.utilidades
import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import android.util.Log
import com.example.udparents.modelo.AppUso
import com.example.udparents.repositorio.RepositorioApps
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import com.example.udparents.vista.pantallas.PantallaBloqueoComposeActivity
import kotlinx.coroutines.runBlocking
import android.app.usage.UsageStats


object RegistroUsoApps {

    suspend fun registrarUsoAplicaciones(context: Context) {
        val uidHijo = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val ahora = System.currentTimeMillis()
        val hace24h = ahora - (24 * 60 * 60 * 1000)

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            hace24h,
            ahora
        )

        if (stats.isNullOrEmpty()) {
            Log.w("RegistroUsoApps", "Sin permisos o sin datos de uso.")
            return
        }

        val repositorio = RepositorioApps()

        withContext(Dispatchers.IO) {
            for (app in stats) {
                val tiempoUso = app.totalTimeInForeground
                if (tiempoUso > 0) {
                    val nombreApp = obtenerNombreApp(context, app.packageName)

                    // 🔒 Validación de bloqueo
                    val estaBloqueada = runBlocking {
                        repositorio.estaAppBloqueada(uidHijo, app.packageName)
                    }

                    if (estaBloqueada) {
                        Log.i("RegistroUsoApps", "🔒 App bloqueada detectada: ${app.packageName}")
                        val intent = Intent(context, com.example.udparents.vista.pantallas.PantallaBloqueoComposeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("nombreApp", nombreApp)
                        }
                        context.startActivity(intent)
                        return@withContext // ❌ NO registrar ni continuar con esa app
                    }

                    // ✅ Registrar uso si no está bloqueada
                    val appUso = AppUso(
                        nombrePaquete = app.packageName,
                        nombreApp = nombreApp,
                        fechaUso = ahora,
                        tiempoUso = tiempoUso
                    )
                    repositorio.registrarUsoAplicacion(uidHijo, appUso)
                }
            }
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

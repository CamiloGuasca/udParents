// com.example.udparents.repositorio/RepositorioApps.kt

package com.example.udparents.repositorio
import com.google.firebase.firestore.SetOptions
import com.example.udparents.modelo.AppUso
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log // Importar para logs
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Date // Mantener esta importación si la usas para logs o en otros lugares

class RepositorioApps {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "RepositorioApps" // Para logs más específicos
    private val ultimoUsoRegistrado = mutableMapOf<String, Long>()

    // *** MODIFICACIÓN PRINCIPAL AQUÍ ***


    suspend fun registrarUsoAplicacion(uidHijo: String, appUso: AppUso) {
        val clave = "${appUso.nombrePaquete}_${formatearFecha(appUso.fechaUso)}"
        val docRef = db.collection("hijos").document(uidHijo)
            .collection("uso_apps").document(clave)

        try {
            // Simplemente guardamos el AppUso. Firebase sobrescribirá si el documento existe,
            // o lo creará si no. El tiempoUso ya viene acumulado del UsageStatsManager.
            docRef.set(appUso, SetOptions.merge()) // Usa merge para solo actualizar los campos proporcionados
            Log.d(TAG, "✅ Uso de app registrado/actualizado: ${appUso.nombreApp} (${appUso.tiempoUso} ms) para ${uidHijo}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al registrar uso de app ${appUso.nombrePaquete}: ${e.message}", e)
        }
    }


    // El resto de tus funciones en RepositorioApps se mantienen IGUAL.
    // Ej: obtenerUsosPorFecha, obtenerHijosVinculados, bloquearApp, estaAppBloqueada.
    // La función obtenerUsosPorFecha ahora traerá los datos ya agrupados por día.

    suspend fun obtenerUsosPorFecha(idHijo: String, desde: Long, hasta: Long): List<AppUso> {
        val desdeDia = formatearFecha(desde)
        val hastaDia = formatearFecha(hasta)

        val snapshot = db.collection("hijos")
            .document(idHijo)
            .collection("uso_apps")
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(AppUso::class.java) }
            .filter { appUso ->
                val fecha = formatearFecha(appUso.fechaUso)
                fecha >= desdeDia && fecha <= hastaDia
            }
    }

    suspend fun obtenerHijosVinculados(idPadre: String): List<Pair<String, String>> {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("codigos_vinculacion")
            .whereEqualTo("idPadre", idPadre)
            .whereEqualTo("vinculado", true)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            val uid = it.getString("dispositivoHijo")
            val nombre = it.getString("nombreHijo") ?: "Hijo"
            if (uid != null) Pair(uid, nombre) else null
        }
    }
    suspend fun bloquearApp(uidHijo: String, paquete: String, bloquear: Boolean) {
        try {
            val ref = db.collection("bloqueos")
                .document(uidHijo)
                .collection("apps")
                .document(paquete)

            val datos = mapOf("bloqueada" to bloquear)
            ref.set(datos).await()
        } catch (e: Exception) {
            e.printStackTrace() // Puedes manejar esto con logs o mostrar error en UI
        }
    }

    // Consulta si una app está bloqueada para un hijo
    suspend fun estaAppBloqueada(uidHijo: String, paquete: String): Boolean {
        return try {
            val snapshot = db.collection("bloqueos")
                .document(uidHijo)
                .collection("apps")
                .document(paquete)
                .get()
                .await()

            snapshot.getBoolean("bloqueada") == true
        } catch (e: Exception) {
            e.printStackTrace()
            false // Si falla la consulta, asumimos que no está bloqueada
        }
    }
    suspend fun establecerLimiteApp(uidHijo: String, packageName: String, tiempoLimite: Long) {
        try {
            // Asegúrate de que la colección sea la misma que se consulta.
            // La colección "limites_apps" debe ser a nivel de hijo, y dentro
            // una subcolección "apps" con el documento del paquete.
            val ref = db.collection("hijos").document(uidHijo) // <-- Agregado "hijos" y uidHijo
                .collection("limites_apps") // <-- Nombre de colección consistente
                .document(packageName)

            ref.set(mapOf("tiempoLimite" to tiempoLimite)).await() // <-- Campo consistente
            Log.d(TAG, "✅ Límite establecido para $packageName: ${tiempoLimite} ms")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar límite para $packageName: ${e.message}", e)
        }
    }
    suspend fun obtenerLimitesApps(uidHijo: String): Map<String, Long> {
        return try {
            val snapshot = db.collection("hijos").document(uidHijo) // <-- Agregado "hijos" y uidHijo
                .collection("limites_apps") // <-- Nombre de colección consistente
                .get()
                .await()

            snapshot.documents.associate {
                val paquete = it.id
                val limite = it.getLong("tiempoLimite") ?: 0L
                paquete to limite
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener límites: ${e.message}", e)
            emptyMap()
        }
    }
    suspend fun obtenerUsoAppDelDia(uidHijo: String, paquete: String): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fecha = formatearFecha(calendar.timeInMillis)
        val docId = "${paquete}_$fecha"

        val doc = db.collection("hijos").document(uidHijo)
            .collection("uso_apps")
            .document(docId)
            .get()
            .await()

        return doc.getLong("tiempoUso") ?: 0L
    }

    suspend fun obtenerLimiteApp(uidHijo: String, paquete: String): Long {
        return try {
            val doc = Firebase.firestore
                .collection("hijos") // <-- Agregado "hijos"
                .document(uidHijo)
                .collection("limites_apps") // <-- Nombre de colección consistente
                .document(paquete)
                .get()
                .await()

            doc.getLong("tiempoLimite") ?: 0L // <-- Campo consistente
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener límite para $paquete: ${e.message}", e)
            0L
        }
    }
    fun formatearFecha(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(year, month, day)
    }


}
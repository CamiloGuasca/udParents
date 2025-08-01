package com.example.udparents.repositorio
import com.google.firebase.firestore.SetOptions
import com.example.udparents.modelo.AppUso
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.udparents.modelo.RestriccionHorario
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Date
import com.google.firebase.firestore.FieldValue // ¡Importante: Añadir esta importación!

class RepositorioApps {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "RepositorioApps"

    // *** Función existente: registrarUsoAplicacion (se mantiene igual para el barrido de 30s) ***
    suspend fun registrarUsoAplicacion(uidHijo: String, appUso: AppUso) {
        val clave = "${appUso.nombrePaquete}_${formatearFecha(appUso.fechaUso)}"
        val docRef = db.collection("hijos").document(uidHijo)
            .collection("uso_apps").document(clave)

        try {
            // Usa merge para solo actualizar los campos proporcionados, el tiempoUso ya viene acumulado.
            docRef.set(appUso, SetOptions.merge()).await() // Añadir .await() para asegurar que la operación se complete
            Log.d(TAG, "✅ Uso de app registrado/actualizado: ${appUso.nombreApp} (${appUso.tiempoUso} ms) para ${uidHijo}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al registrar uso de app ${appUso.nombrePaquete}: ${e.message}", e)
        }
    }

    // *** FUNCIÓN CORREGIDA: incrementarUsoAplicacion (para el monitoreo en tiempo real) ***
    suspend fun incrementarUsoAplicacion(uidHijo: String, packageName: String, appName: String, incrementBy: Long) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fecha = formatearFecha(calendar.timeInMillis)
        val docId = "${packageName}_$fecha"
        val docRef = db.collection("hijos").document(uidHijo)
            .collection("uso_apps").document(docId)

        try {
            // Intenta obtener el documento primero
            val docSnapshot = docRef.get().await()

            if (docSnapshot.exists()) {
                // Si el documento ya existe, usa FieldValue.increment() para actualizar de forma atómica.
                // Esto garantiza que múltiples actualizaciones concurrentes no sobrescriban el valor.
                docRef.update("tiempoUso", FieldValue.increment(incrementBy)).await()
                Log.d(TAG, "📈 Uso de $packageName incrementado en $incrementBy ms.")
            } else {
                // Si el documento no existe, lo creamos con el valor inicial.
                val appUsoInicial = AppUso(
                    nombrePaquete = packageName,
                    nombreApp = appName,
                    fechaUso = calendar.timeInMillis,
                    tiempoUso = incrementBy
                )
                // Usamos SetOptions.merge() para crear el documento o fusionarlo si ya existe con otros campos
                docRef.set(appUsoInicial, SetOptions.merge()).await()
                Log.d(TAG, "➕ Documento de uso para $packageName creado con $incrementBy ms.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado al incrementar/crear uso de $packageName: ${e.message}", e)
        }
    }

    // Las demás funciones se mantienen sin cambios

    suspend fun guardarRestriccionHorario(uidHijo: String, restriccion: RestriccionHorario) {
        val docRef = db.collection("hijos").document(uidHijo)
            .collection("horarios_restriccion").document(restriccion.id.ifEmpty { db.collection("hijos").document(uidHijo).collection("horarios_restriccion").document().id }) // Genera ID si está vacío
        try {
            docRef.set(restriccion).await()
            Log.d(TAG, "✅ Restricción de horario guardada/actualizada: ${restriccion.ruleName} (ID: ${docRef.id})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar restricción de horario: ${e.message}", e)
        }
    }

    suspend fun obtenerRestriccionesHorario(uidHijo: String): List<RestriccionHorario> {
        return try {
            val snapshot = db.collection("hijos").document(uidHijo)
                .collection("horarios_restriccion")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(RestriccionHorario::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener restricciones de horario: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun eliminarRestriccionHorario(uidHijo: String, restriccionId: String) {
        try {
            db.collection("hijos").document(uidHijo)
                .collection("horarios_restriccion").document(restriccionId)
                .delete().await()
            Log.d(TAG, "✅ Restricción de horario eliminada: $restriccionId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al eliminar restricción de horario: ${e.message}", e)
        }
    }

    suspend fun establecerLimiteApp(uidHijo: String, packageName: String, tiempoLimite: Long) {
        try {
            val ref = db.collection("hijos").document(uidHijo)
                .collection("limites_apps")
                .document(packageName)

            ref.set(mapOf("tiempoLimite" to tiempoLimite)).await()
            Log.d(TAG, "✅ Límite establecido para $packageName: ${tiempoLimite} ms")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al guardar límite para $packageName: ${e.message}", e)
        }
    }

    suspend fun obtenerLimiteApp(uidHijo: String, paquete: String): Long {
        return try {
            val doc = Firebase.firestore
                .collection("hijos")
                .document(uidHijo)
                .collection("limites_apps")
                .document(paquete)
                .get()
                .await()

            doc.getLong("tiempoLimite") ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener límite para $paquete: ${e.message}", e)
            0L
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

    suspend fun obtenerLimitesApps(uidHijo: String): Map<String, Long> {
        return try {
            val snapshot = db.collection("hijos").document(uidHijo)
                .collection("limites_apps")
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
            false
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

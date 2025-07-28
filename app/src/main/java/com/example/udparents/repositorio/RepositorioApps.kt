// com.example.udparents.repositorio/RepositorioApps.kt

package com.example.udparents.repositorio

import com.example.udparents.modelo.AppUso
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log // Importar para logs
import java.util.Date // Mantener esta importación si la usas para logs o en otros lugares

class RepositorioApps {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "RepositorioApps" // Para logs más específicos

    // *** MODIFICACIÓN PRINCIPAL AQUÍ ***
    suspend fun registrarUsoAplicacion(idHijo: String, appUso: AppUso) {
        // Creamos un ID de documento único basado en el paquete de la app y la fecha de uso (medianoche).
        // Esto asegura que solo haya un documento por app por día.
        val documentId = "${appUso.nombrePaquete}_${appUso.fechaUso}" // fechaUso ahora debe ser el inicio del día

        val docRef = db.collection("usos_apps")
            .document(idHijo)
            .collection("historial")
            .document(documentId)

        try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)

                if (snapshot.exists()) {
                    // Si el documento ya existe, sumamos el tiempo de uso.
                    val existingTime = snapshot.getLong("tiempoUso") ?: 0L
                    val newTotalTime = existingTime + appUso.tiempoUso
                    transaction.update(docRef, "tiempoUso", newTotalTime)
                    Log.d(TAG, "✅ Actualizado uso de ${appUso.nombreApp} (${appUso.nombrePaquete}) para el día ${Date(appUso.fechaUso)}. Tiempo total: ${newTotalTime} ms")
                } else {
                    // Si el documento no existe, lo creamos.
                    // Asegúrate de que los datos sean compatibles con tu modelo AppUso.
                    transaction.set(docRef, appUso)
                    Log.d(TAG, "➕ Nuevo registro de uso para ${appUso.nombreApp} (${appUso.nombrePaquete}) el día ${Date(appUso.fechaUso)}. Tiempo: ${appUso.tiempoUso} ms")
                }
                null // Una transacción exitosa debe devolver null o un valor.
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al registrar/actualizar uso de aplicación: ${appUso.nombreApp} - ${e.message}", e)
            throw e // Propaga la excepción para manejo superior si es necesario
        }
    }

    // El resto de tus funciones en RepositorioApps se mantienen IGUAL.
    // Ej: obtenerUsosPorFecha, obtenerHijosVinculados, bloquearApp, estaAppBloqueada.
    // La función obtenerUsosPorFecha ahora traerá los datos ya agrupados por día.

    suspend fun obtenerUsosPorFecha(idHijo: String, desde: Long, hasta: Long): List<AppUso> {
        println("📅 Consulta para $idHijo desde ${Date(desde)} hasta ${Date(hasta)}")
        val snapshot = db.collection("usos_apps")
            .document(idHijo)
            .collection("historial")
            .whereGreaterThanOrEqualTo("fechaUso", desde)
            .whereLessThanOrEqualTo("fechaUso", hasta)
            .get()
            .await()

        println("📄 Documentos encontrados: ${snapshot.documents.size}")
        snapshot.documents.forEachIndexed { i, doc ->
            println("🔍 Doc[$i] => ${doc.data}")
        }

        return snapshot.documents.mapNotNull { it.toObject(AppUso::class.java) }
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
}
package com.example.udparents.repositorio

import android.util.Log
import com.example.udparents.modelo.BloqueoRegistro
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar los datos de los registros de bloqueos en Firestore.
 * Utiliza un modelo de datos llamado BloqueoRegistro y se comunica con la base de datos de Firebase.
 */
class RepositorioBloqueos {

    // Instancia de Firestore para interactuar con la base de datos
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "RepositorioBloqueos"

    /**
     * Registra un intento de acceso bloqueado en Firestore.
     * La colección será `hijos/{uidHijo}/registros_bloqueo`.
     * Cada registro tendrá un ID único generado automáticamente por Firestore.
     * @param uidHijo El UID del hijo al que se le ha bloqueado la aplicación.
     * @param bloqueo El objeto [BloqueoRegistro] que contiene toda la información del evento.
     */
    suspend fun registrarBloqueo(uidHijo: String, bloqueo: BloqueoRegistro) {
        // Obtenemos la referencia a la colección específica para los registros de bloqueo del hijo
        val docRef = db.collection("hijos").document(uidHijo)
            .collection("registros_bloqueo")
            .document() // Firestore generará un ID único automáticamente para el documento

        try {
            // Guardamos el objeto BloqueoRegistro en el nuevo documento
            docRef.set(bloqueo).await()
            Log.d(TAG, "✅ Intento de acceso bloqueado registrado para: ${bloqueo.nombreApp}")
        } catch (e: Exception) {
            // Manejamos cualquier error que pueda ocurrir durante la operación de guardado
            Log.e(TAG, "❌ Error al registrar bloqueo para ${bloqueo.nombreApp}: ${e.message}", e)
        }
    }

    /**
     * Obtiene una lista de registros de bloqueos para un hijo específico.
     * La lista se ordena por la marca de tiempo (timestamp) de forma descendente,
     * mostrando los bloqueos más recientes primero.
     * @param uidHijo El UID del hijo.
     * @return Una lista de objetos [BloqueoRegistro] si se encuentran, o una lista vacía
     * si no hay registros o si ocurre un error.
     */
    suspend fun obtenerRegistrosDeBloqueo(uidHijo: String): List<BloqueoRegistro> {
        return try {
            // Obtenemos la instantánea de la colección de registros de bloqueo del hijo
            val snapshot = db.collection("hijos").document(uidHijo)
                .collection("registros_bloqueo")
                .get()
                .await()

            // Mapeamos los documentos de Firestore a objetos BloqueoRegistro
            // La ordenación se realiza en memoria después de obtener los datos para evitar
            // la necesidad de crear índices compuestos en Firestore, lo cual simplifica la configuración.
            val bloqueos = snapshot.documents.mapNotNull { it.toObject(BloqueoRegistro::class.java) }
            bloqueos.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            // Manejamos cualquier error que pueda ocurrir al obtener los datos
            Log.e(TAG, "❌ Error al obtener registros de bloqueo: ${e.message}", e)
            emptyList()
        }
    }
}

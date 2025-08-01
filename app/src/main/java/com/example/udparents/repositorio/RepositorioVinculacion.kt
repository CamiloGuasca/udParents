package com.example.udparents.repositorio

import com.example.udparents.modelo.CodigoVinculacion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RepositorioVinculacion {

    private val db = FirebaseFirestore.getInstance()

    suspend fun guardarCodigo(codigo: CodigoVinculacion) {
        db.collection("codigos_vinculacion")
            .document(codigo.codigo)
            .set(codigo)
            .await()
    }

    suspend fun existeCodigo(codigo: String): Boolean {
        val doc = db.collection("codigos_vinculacion").document(codigo).get().await()
        return doc.exists()
    }

    suspend fun verificarCodigoValido(codigo: String): Boolean {
        val doc = db.collection("codigos_vinculacion").document(codigo).get().await()
        val data = doc.toObject(CodigoVinculacion::class.java)

        if (data != null) {
            val tiempoActual = System.currentTimeMillis()
            val tiempoExpiracion = 5 * 60 * 1000 // 5 minutos
            return (tiempoActual - data.timestampCreacion) <= tiempoExpiracion
        }
        return false
    }

    fun marcarCodigoComoVinculado(codigo: String, idHijo: String, onResult: (Boolean) -> Unit) {
        db.collection("codigos_vinculacion")
            .document(codigo)
            .update(
                mapOf(
                    "vinculado" to true,
                    "dispositivoHijo" to idHijo
                )
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    suspend fun obtenerDispositivosVinculados(idPadre: String): List<CodigoVinculacion> {
        val snapshot = db.collection("codigos_vinculacion")
            .whereEqualTo("idPadre", idPadre)
            .whereEqualTo("vinculado", true)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(CodigoVinculacion::class.java) }
    }

    fun vincularConDatos(
        codigoVinculacion: CodigoVinculacion,
        onResult: (Boolean) -> Unit
    ) {
        val datos = mapOf(
            "vinculado" to true,
            "dispositivoHijo" to codigoVinculacion.dispositivoHijo,
            "nombreHijo" to codigoVinculacion.nombreHijo,
            "edadHijo" to codigoVinculacion.edadHijo,
            "sexoHijo" to codigoVinculacion.sexoHijo
        )

        db.collection("codigos_vinculacion")
            .document(codigoVinculacion.codigo)
            .update(datos)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
    suspend fun dispositivoYaVinculado(idDispositivo: String): Boolean {
        val snapshot = db.collection("codigos_vinculacion")
            .whereEqualTo("dispositivoHijo", idDispositivo)
            .get()
            .await()
        return !snapshot.isEmpty
    }
    fun actualizarVinculacion(
        uidPadre: String,
        dispositivo: CodigoVinculacion,
        onResult: (Boolean) -> Unit
    ) {
        // En Firestore, el documento del hijo está bajo el código de vinculación.
        // Se actualizan solo los campos que pueden ser editados.
        db.collection("codigos_vinculacion")
            .document(dispositivo.codigo)
            .update(
                mapOf(
                    "nombreHijo" to dispositivo.nombreHijo,
                    "edadHijo" to dispositivo.edadHijo,
                    "sexoHijo" to dispositivo.sexoHijo
                )
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Elimina una vinculación de la base de datos de Firestore.
     * @param uidPadre El UID del padre.
     * @param uidHijo El UID del hijo a desvincular.
     * @param onResult Callback que indica si la operación fue exitosa o no.
     */
    fun eliminarVinculacion(
        uidPadre: String,
        uidHijo: String,
        onResult: (Boolean) -> Unit
    ) {
        // En este caso, buscaremos el documento por el uidHijo para eliminarlo.
        db.collection("codigos_vinculacion")
            .whereEqualTo("idPadre", uidPadre)
            .whereEqualTo("dispositivoHijo", uidHijo)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Asumimos una única vinculación por hijo
                    document.reference.delete()
                        .addOnSuccessListener { onResult(true) }
                        .addOnFailureListener { onResult(false) }
                } else {
                    onResult(false) // No se encontró el documento para eliminar
                }
            }
            .addOnFailureListener { onResult(false) }
    }
}

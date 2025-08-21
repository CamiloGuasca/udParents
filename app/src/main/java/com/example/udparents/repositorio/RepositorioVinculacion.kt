package com.example.udparents.repositorio

import com.example.udparents.modelo.CodigoVinculacion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RepositorioVinculacion {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionCodigos = db.collection("codigos_vinculacion") // Usamos una variable para evitar errores de escritura

    suspend fun guardarCodigo(codigo: CodigoVinculacion) {
        coleccionCodigos
            .document(codigo.codigo)
            .set(codigo)
            .await()
    }

    suspend fun existeCodigo(codigo: String): Boolean {
        val doc = coleccionCodigos.document(codigo).get().await()
        return doc.exists()
    }

    suspend fun verificarCodigoValido(codigo: String): Boolean {
        val doc = coleccionCodigos.document(codigo).get().await()
        val data = doc.toObject(CodigoVinculacion::class.java)

        if (data != null) {
            val tiempoActual = System.currentTimeMillis()
            val tiempoExpiracion = 5 * 60 * 1000 // 5 minutos
            // 💡 Se añade la verificación para asegurar que el código no esté ya vinculado
            return (tiempoActual - data.timestampCreacion) <= tiempoExpiracion && !data.vinculado
        }
        return false
    }

    fun marcarCodigoComoVinculado(codigo: String, idHijo: String, onResult: (Boolean) -> Unit) {
        coleccionCodigos
            .document(codigo)
            .update(
                mapOf(
                    "vinculado" to true,
                    "dispositivoHijo" to idHijo,
                    "timestampVinculacion" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * 💡 NUEVA FUNCIÓN AÑADIDA: Obtiene un objeto CodigoVinculacion completo a partir de su código.
     * Esta función es necesaria para obtener el UID del padre.
     */
    suspend fun obtenerCodigoPorID(codigo: String): CodigoVinculacion? {
        val documento = coleccionCodigos.document(codigo).get().await()
        return if (documento.exists()) {
            documento.toObject(CodigoVinculacion::class.java)
        } else {
            null
        }
    }

    suspend fun obtenerDispositivosVinculados(idPadre: String): List<CodigoVinculacion> {
        val snapshot = coleccionCodigos
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
            "sexoHijo" to codigoVinculacion.sexoHijo,
            // ✅ Consentimiento
            "termsAccepted" to codigoVinculacion.termsAccepted,
            "termsVersion" to codigoVinculacion.termsVersion,
            "termsAcceptedAt" to (codigoVinculacion.termsAcceptedAt ?: System.currentTimeMillis()),

            // (opcional) útil para auditoría
            "timestampVinculacion" to System.currentTimeMillis()
        )

        coleccionCodigos
            .document(codigoVinculacion.codigo)
            .update(datos)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
    suspend fun dispositivoYaVinculado(idDispositivo: String): Boolean {
        val snapshot = coleccionCodigos
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
        coleccionCodigos
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
        coleccionCodigos
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
    fun obtenerEstadoAlertaContenidoPadre(
        uidPadre: String,
        onResultado: (Boolean) -> Unit
    ) {
        FirebaseFirestore.getInstance().collection("usuarios")
            .document(uidPadre)
            .get()
            .addOnSuccessListener { documento ->
                val activado = documento.getBoolean("alertaContenidoProhibido") ?: false
                onResultado(activado)
            }
            .addOnFailureListener {
                onResultado(false)
            }
    }

}

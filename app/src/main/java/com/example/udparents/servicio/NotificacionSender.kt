package com.example.udparents.servicio

import android.util.Log
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Clase para enviar notificaciones a través de una Cloud Function de Firebase.
 * Esta clase debe ser usada en la aplicación del hijo.
 */
class NotificacionSender {

    private val TAG = "NotificacionSender"
    // CORRECCIÓN: Usamos la región 'us-central1'
    private val functions = Firebase.functions("us-central1")

    /**
     * Envía una notificación al dispositivo del padre a través de una Cloud Function.
     *
     * @param uidPadre El UID del padre para obtener su token de FCM.
     * @param titulo El título de la notificación.
     * @param mensaje El cuerpo del mensaje de la notificación.
     */
    suspend fun enviarNotificacionAlPadre(uidPadre: String, titulo: String, mensaje: String) {

        // --- 🔍 LOGS DE VERIFICACIÓN ADICIONALES ---
        Log.d(TAG, "🔍 Verificando valores antes de crear el payload:")
        Log.d(TAG, "  -> uidPadre: '$uidPadre'")
        Log.d(TAG, "  -> titulo: '$titulo'")
        Log.d(TAG, "  -> mensaje: '$mensaje'")

        if (uidPadre.isEmpty() || titulo.isEmpty() || mensaje.isEmpty()) {
            Log.e(TAG, "❌ ERROR: Se detectó un valor vacío antes de la llamada a la Cloud Function.")
            // Aquí puedes lanzar una excepción o manejar el error si lo deseas.
            // No se procederá con la llamada.
            return
        }

        // Prepara los datos que se enviarán a la Cloud Function
        val data = hashMapOf(
            "uidPadre" to uidPadre,
            "titulo" to titulo,
            "mensaje" to mensaje
        )

        Log.d(TAG, "✅ Payload preparado correctamente. Enviando a Cloud Function...")

        try {
            // Llama a la Cloud Function que creaste.
            // La función se llama "enviarNotificacionAlPadre".
            val result = functions
                .getHttpsCallable("enviarNotificacionAlPadre")
                .call(data)
                .await()

            Log.d(TAG, "✅ Cloud Function llamada exitosamente. Resultado: ${result.data}")
        } catch (e: FirebaseFunctionsException) {
            Log.e(TAG, "❌ Error al llamar a la Cloud Function: ${e.code} - ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado:", e)
        }
    }
}

    package com.example.udparents.repositorio

    import com.example.udparents.modelo.AppUso
    import com.google.firebase.firestore.FirebaseFirestore
    import kotlinx.coroutines.tasks.await
    import java.util.Date

    class RepositorioApps {

        private val db = FirebaseFirestore.getInstance()

        suspend fun registrarUsoAplicacion(idHijo: String, appUso: AppUso) {
            db.collection("usos_apps")
                .document(idHijo)
                .collection("historial")
                .add(appUso)
                .await()
        }

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

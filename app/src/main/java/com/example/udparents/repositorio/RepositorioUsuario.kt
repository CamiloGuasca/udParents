package com.example.udparents.repositorio

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.udparents.modelo.Usuario
import com.google.firebase.auth.FirebaseUser

class RepositorioUsuario {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun registrarUsuario(
        usuario: Usuario,
        onResultado: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(usuario.correo, usuario.contrasena)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    val usuarioFirestore = Usuario(usuario.nombre.trim(), usuario.correo.trim())
                    val uid = auth.currentUser?.uid ?: ""

                    // 🟢 CAMBIO: Se añade el timestampRegistro al documento
                    db.collection("usuarios")
                        .document(uid)
                        .set(mapOf(
                            "nombre" to usuarioFirestore.nombre,
                            "correo" to usuarioFirestore.correo,
                            "timestampRegistro" to System.currentTimeMillis() // 🟢 ¡AQUÍ ESTÁ EL CAMBIO CLAVE!
                        ))
                        .addOnSuccessListener {
                            onResultado(true, null)
                        }
                        .addOnFailureListener { error ->
                            onResultado(false, "Error guardando datos: ${error.message}")
                        }
                } else {
                    onResultado(false, tarea.exception?.message)
                }
            }
    }

    fun iniciarSesion(usuario: Usuario, onResultado: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(usuario.correo, usuario.contrasena)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    val usuarioFirebase = auth.currentUser
                    if (usuarioFirebase != null) {
                        // 🟢 VERIFICACIÓN DE CADUCIDAD
                        if (!usuarioFirebase.isEmailVerified) {
                            // Si el correo no está verificado, se chequea el timestamp
                            db.collection("usuarios").document(usuarioFirebase.uid).get()
                                .addOnSuccessListener { documento ->
                                    val timestampRegistro = documento.getLong("timestampRegistro")
                                    if (timestampRegistro != null) {
                                        val tiempoActual = System.currentTimeMillis()
                                        val tresMinutosEnMs = 3 * 60 * 1000

                                        if (tiempoActual - timestampRegistro > tresMinutosEnMs) {
                                            // ⚠️ El tiempo ha expirado, eliminar la cuenta
                                            usuarioFirebase.delete().addOnCompleteListener {
                                                db.collection("usuarios").document(usuarioFirebase.uid).delete()
                                                onResultado(false, "El tiempo para verificar el correo ha expirado. Por favor, regístrate de nuevo.")
                                            }
                                        } else {
                                            // El tiempo no ha expirado
                                            onResultado(false, "Debes verificar tu correo.")
                                        }
                                    } else {
                                        // No se encontró el timestamp, mostrar error.
                                        onResultado(false, "Error: No se encontró el registro de tiempo.")
                                    }
                                }
                                .addOnFailureListener {
                                    onResultado(false, "Error al acceder a los datos de registro.")
                                }
                        } else {
                            // El correo está verificado
                            onResultado(true, null)
                        }
                    } else {
                        // Esto no debería ocurrir si la tarea fue exitosa.
                        onResultado(false, "Usuario no encontrado.")
                    }
                } else {
                    onResultado(false, tarea.exception?.localizedMessage)
                }
            }
    }

// ... (resto del código)

    fun recuperarContrasena(usuario: Usuario, onResultado: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(usuario.correo)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    onResultado(true, null)
                } else {
                    onResultado(false, tarea.exception?.localizedMessage)
                }
            }
    }
    fun obtenerUsuarioActual(): FirebaseUser? {
        return auth.currentUser
    }
    fun obtenerEstadoAlertaContenido(
        uid: String,
        onResultado: (Boolean?) -> Unit
    ) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { documento ->
                onResultado(documento.getBoolean("alertaContenidoProhibido"))
            }
            .addOnFailureListener {
                onResultado(null)
            }
    }

    fun actualizarEstadoAlertaContenido(
        uid: String,
        nuevoEstado: Boolean,
        onResultado: (Boolean) -> Unit
    ) {
        db.collection("usuarios").document(uid)
            .update("alertaContenidoProhibido", nuevoEstado)
            .addOnSuccessListener { onResultado(true) }
            .addOnFailureListener { onResultado(false) }
    }

}

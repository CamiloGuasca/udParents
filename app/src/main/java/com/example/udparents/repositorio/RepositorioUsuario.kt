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

                    db.collection("usuarios")
                        .document(auth.currentUser?.uid ?: "")
                        .set(usuarioFirestore)
                        .addOnSuccessListener {
                            onResultado(true, null)
                        }
                        .addOnFailureListener { error ->
                            onResultado(false, "Error guardando datos: ${'$'}{error.message}")
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
                    if (usuarioFirebase != null && usuarioFirebase.isEmailVerified) {
                        onResultado(true, null)
                    } else {
                        onResultado(false, "Debes verificar tu correo.")
                    }
                } else {
                    onResultado(false, tarea.exception?.localizedMessage)
                }
            }
    }

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
}

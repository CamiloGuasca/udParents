package com.example.udparents.repositorio

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.udparents.modelo.Usuario
import com.google.firebase.auth.FirebaseUser

/**
 * Repositorio que maneja el registro de usuarios con Firebase Authentication y guarda los datos en Firestore.
 */
class RepositorioAutenticacion {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Registra un usuario con correo y contraseña en Firebase Authentication.
     * Luego guarda su nombre y correo en Firestore.
     */
    fun registrarUsuario(
        nombre: String,
        correo: String,
        contrasena: String,
        onResultado: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    val usuario = Usuario(nombre.trim(), correo.trim())

                    db.collection("usuarios")
                        .document(auth.currentUser?.uid ?: "")
                        .set(usuario)
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
    //NUEVO: Obtener el usuario actual
    fun obtenerUsuarioActual(): FirebaseUser? {
        return auth.currentUser
    }

}

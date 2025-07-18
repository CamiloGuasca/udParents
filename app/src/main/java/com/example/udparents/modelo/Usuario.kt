package com.example.udparents.modelo

/**
 * Clase que representa los datos de un usuario.
 * Este modelo se puede guardar en Firestore.
 */
data class Usuario(
    val nombre: String = "",
    val correo: String = ""
)

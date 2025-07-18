package com.example.udparents.utilidades

/**
 * Funciones auxiliares para validar campos de texto.
 */

fun esCorreoValido(correo: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()
}

fun esContrasenaValida(contrasena: String): Boolean {
    return contrasena.trim().length >= 6
}

fun campoNoVacio(texto: String): Boolean {
    return texto.trim().isNotEmpty()
}

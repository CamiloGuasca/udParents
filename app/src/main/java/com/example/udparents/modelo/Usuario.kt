package com.example.udparents.modelo

import com.example.udparents.utilidades.campoNoVacio
import com.example.udparents.utilidades.esContrasenaValida
import com.example.udparents.utilidades.esCorreoValido

data class Usuario(
    val nombre: String = "",
    val correo: String = "",
    val contrasena: String = "" // también puede estar aquí si se requiere validación completa
) {
    fun esValido(): Boolean {
        return  campoNoVacio(nombre) && esCorreoValido(correo) && esContrasenaValida(contrasena)
    }
    fun esValidoParaLogin(): Boolean {
        return esCorreoValido(correo) && esContrasenaValida(contrasena)
    }
    fun esValidoParaRecuperar(): Boolean {
        return esCorreoValido(correo)
    }
}

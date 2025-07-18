package com.example.udparents.viewmodel

import androidx.lifecycle.ViewModel
import com.example.udparents.repositorio.RepositorioAutenticacion

/**
 * ViewModel que se comunica con el repositorio para registrar un usuario.
 */
class VistaModeloRegistro : ViewModel() {

    private val repositorio = RepositorioAutenticacion()

    fun registrar(
        nombre: String,
        correo: String,
        contrasena: String,
        onResultado: (Boolean, String?) -> Unit
    ) {
        repositorio.registrarUsuario(nombre, correo, contrasena, onResultado)
    }
}

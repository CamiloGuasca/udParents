package com.example.udparents.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.udparents.repositorio.RepositorioUsuario
import com.example.udparents.modelo.Usuario
import com.example.udparents.utilidades.esCorreoValido
import com.example.udparents.utilidades.esContrasenaValida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VistaModeloUsuario : ViewModel() {

    private val repositorio = RepositorioUsuario()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val _usuario = MutableStateFlow(Usuario())
    val usuario: StateFlow<Usuario> = _usuario
    fun actualizarCorreo(correo: String) {
        _usuario.value = _usuario.value.copy(correo = correo.trim())
    }

    fun actualizarContrasena(contrasena: String) {
        _usuario.value = _usuario.value.copy(contrasena = contrasena.trim())
    }

    fun actualizarNombre(nombre: String) {
        _usuario.value = _usuario.value.copy(nombre = nombre.trim())
    }
    fun registrarUsuario(onResultado: (Boolean, String?) -> Unit) {
        val usuarioActual = _usuario.value

        if (!usuarioActual.esValido()) {
            _mensaje.value = "Por favor completa los campos correctamente"
            onResultado(false, _mensaje.value)
            return
        }

        _cargando.value = true
        repositorio.registrarUsuario(usuarioActual) { exito, error ->
            _cargando.value = false
            if (exito) {
                val usuarioFirebase = repositorio.obtenerUsuarioActual()
                usuarioFirebase?.sendEmailVerification()
                    ?.addOnCompleteListener { verificacion ->
                        if (verificacion.isSuccessful) {
                            _mensaje.value = "✅ Registro exitoso. Revisa tu correo para verificar tu cuenta."
                            onResultado(true, null)
                        } else {
                            _mensaje.value = "⚠️ Usuario creado, pero no se pudo enviar el correo de verificación."
                            onResultado(false, _mensaje.value)
                        }
                    }
            } else {
                _mensaje.value = error ?: "Error desconocido"
                onResultado(false, _mensaje.value)
            }
        }
    }

    fun iniciarSesion(onExito: () -> Unit, onError: (String) -> Unit) {
        val usuarioActual = _usuario.value
        if (!usuarioActual.esValidoParaLogin()) {
            onError("Correo o contraseña inválidos")
            return
        }

        _cargando.value = true
        repositorio.iniciarSesion(usuarioActual) { exito, mensaje ->
            _cargando.value = false
            if (exito) onExito() else onError(mensaje ?: "Error desconocido")
        }
    }
    fun recuperarContrasena( onResultado: (Boolean, String?) -> Unit) {
        val usuarioActual = _usuario.value
        if (!usuarioActual.esValidoParaRecuperar()) {
            _mensaje.value = "Por favor completa los campos correctamente"
            onResultado(false, _mensaje.value)
            return
        }
        _cargando.value = true
        repositorio.recuperarContrasena(usuarioActual) { exito, mensaje ->
            _cargando.value = false
            if (exito) {
                _mensaje.value = "✅ Se ha enviado un correo para restablecer tu contraseña."
                onResultado(true, _mensaje.value)
            } else {
                _mensaje.value = mensaje ?: "Error desconocido"
                 onResultado(false, _mensaje.value)
            }
        }

    }
    fun limpiarMensaje() {
        _mensaje.value = null
    }
}


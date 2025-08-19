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

    private val _alertaContenido = MutableStateFlow(false)
    val alertaContenido: StateFlow<Boolean> = _alertaContenido

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val _usuario = MutableStateFlow(Usuario())
    val usuario: StateFlow<Usuario> = _usuario
    private companion object {
        // mínimo de letras (sin contar espacios); súbelo si quieres algo más estricto
        const val MIN_LETRAS_SIN_ESPACIOS = 10
    }
    fun actualizarCorreo(correo: String) {
        _usuario.value = _usuario.value.copy(correo = correo.trim())
    }

    fun actualizarContrasena(contrasena: String) {
        _usuario.value = _usuario.value.copy(contrasena = contrasena.trim())
    }

    fun actualizarNombre(nombre: String) {
        _usuario.value = _usuario.value.copy(nombre = nombre)
    }
    private fun normalizarNombreEntrada(nombre: String): String =
        nombre.trim().replace("\\s+".toRegex(), " ")
    fun registrarUsuario(onResultado: (Boolean, String?) -> Unit) {
        val usuarioActual = _usuario.value
        if (!validarNombrePadre(usuarioActual.nombre)) {
            _mensaje.value = "Escribe nombre y apellido (mín. $MIN_LETRAS_SIN_ESPACIOS letras en total)."
            onResultado(false, _mensaje.value)
            return
        }
        if (!validarContrasenaSegura(usuarioActual.contrasena)) {
            _mensaje.value = "La contraseña debe tener al menos 8 caracteres, incluyendo mayúsculas, minúsculas, números y símbolos."
            onResultado(false, _mensaje.value)
            return
        }

        if (!usuarioActual.esValido()) {
            _mensaje.value = "Por favor completa los campos correctamente"
            onResultado(false, _mensaje.value)
            return
        }

        _cargando.value = true
        val usuarioNormalizado = usuarioActual.copy(
            nombre = normalizarNombreEntrada(usuarioActual.nombre)
        )
        repositorio.registrarUsuario(usuarioNormalizado) { exito, error ->
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
    fun cargarEstadoAlerta(uid: String) {
        repositorio.obtenerEstadoAlertaContenido(uid) { estado ->
            estado?.let { _alertaContenido.value = it }
        }
    }

    fun actualizarEstadoAlerta(uid: String, nuevoEstado: Boolean) {
        _alertaContenido.value = nuevoEstado
        repositorio.actualizarEstadoAlertaContenido(uid, nuevoEstado) { exito ->
            if (!exito) {
                // Podrías revertir el cambio local o mostrar un mensaje
            }
        }
    }
    private fun validarNombrePadre(nombre: String): Boolean {
        val n = normalizarNombreEntrada(nombre)

        // Debe tener al menos dos palabras (nombre y apellido) con 2+ caracteres cada una
        val partes = n.split(" ")
        val tieneNombreApellido = partes.size >= 2 &&
                partes[0].length >= 2 &&
                partes[1].length >= 2

        // Mínimo de letras totales (sin contar espacios)
        val largoOk = n.replace(" ", "").length >= MIN_LETRAS_SIN_ESPACIOS

        return tieneNombreApellido && largoOk
    }

    private fun validarContrasenaSegura(contrasena: String): Boolean {
        // La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un dígito y un carácter especial.
        val regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$".toRegex()
        return contrasena.matches(regex)
    }
}


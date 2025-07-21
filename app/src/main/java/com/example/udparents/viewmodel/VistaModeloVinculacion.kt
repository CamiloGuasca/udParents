package com.example.udparents.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.udparents.modelo.CodigoVinculacion
import com.example.udparents.repositorio.RepositorioVinculacion
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.provider.Settings


class VistaModeloVinculacion(
    private val repositorio: RepositorioVinculacion = RepositorioVinculacion()
) : ViewModel() {

    private val _codigoGenerado = MutableStateFlow<String?>(null)
    val codigoGenerado: StateFlow<String?> get() = _codigoGenerado

    private val _dispositivosVinculados = MutableStateFlow<List<CodigoVinculacion>>(emptyList())
    val dispositivosVinculados: StateFlow<List<CodigoVinculacion>> get() = _dispositivosVinculados

    fun generarCodigo(idPadre: String) {
        viewModelScope.launch {
            var nuevoCodigo: String
            do {
                nuevoCodigo = Random.nextInt(100000, 999999).toString()
            } while (repositorio.existeCodigo(nuevoCodigo))

            val codigo = CodigoVinculacion(
                codigo = nuevoCodigo,
                idPadre = idPadre,
                timestampCreacion = System.currentTimeMillis(),
                vinculado = false
            )

            repositorio.guardarCodigo(codigo)
            _codigoGenerado.value = nuevoCodigo
        }
    }

    fun validarCodigoHijo(
        codigo: String,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (codigo.isBlank()) {
                onError("Por favor ingrese un código.")
                return@launch
            }

            val resultado = repositorio.verificarCodigoValido(codigo)
            if (resultado) {
                onExito()
            } else {
                onError("Código inválido o expirado.")
            }
        }
    }

    fun validarCodigoVinculacion(
        codigo: String,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existe = repositorio.existeCodigo(codigo)
                if (existe) {
                    val idHijo = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    repositorio.marcarCodigoComoVinculado(codigo, idHijo) { actualizado ->
                        if (actualizado) {
                            onExito()
                        } else {
                            onError("No se pudo actualizar la vinculación.")
                        }
                    }
                } else {
                    onError("Código no válido")
                }
            } catch (e: Exception) {
                onError("Error al validar el código: ${e.message}")
            }
        }
    }

    fun cargarDispositivosVinculados(idPadre: String) {
        viewModelScope.launch {
            val lista = repositorio.obtenerDispositivosVinculados(idPadre)
            _dispositivosVinculados.value = lista
        }
    }

    fun vincularHijoConDatos(
        context: Context,
        codigo: String,
        nombreHijo: String,
        edadHijo: Int,
        sexoHijo: String,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existe = repositorio.existeCodigo(codigo)
                if (!existe) {
                    onError("Código no válido")
                    return@launch
                }

                val esValido = repositorio.verificarCodigoValido(codigo)
                if (!esValido) {
                    onError("Código expirado")
                    return@launch
                }

                val idHijo = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

                val yaVinculado = repositorio.dispositivoYaVinculado(idHijo)
                if (yaVinculado) {
                    onError("Este dispositivo ya ha sido vinculado previamente.")
                    return@launch
                }

                repositorio.vincularConDatos(codigo, idHijo, nombreHijo, edadHijo, sexoHijo) { exito ->
                    if (exito) {
                        onExito()
                    } else {
                        onError("No se pudo vincular el dispositivo.")
                    }
                }
            } catch (e: Exception) {
                onError("Error al procesar la vinculación: ${e.message}")
            }
        }
    }

}

package com.example.udparents.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.udparents.modelo.AppUso
import com.example.udparents.repositorio.RepositorioApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class VistaModeloApps : ViewModel() {

    private val repositorio = RepositorioApps()

    private val _listaUsos = MutableStateFlow<List<AppUso>>(emptyList())
    val listaUsos: StateFlow<List<AppUso>> = _listaUsos
    private val _hijosVinculados = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val hijosVinculados: StateFlow<List<Pair<String, String>>> = _hijosVinculados
    private val _estadoBloqueoApp = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val estadoBloqueoApp: StateFlow<Map<String, Boolean>> = _estadoBloqueoApp
    private val _limitesApp = MutableStateFlow<Map<String, Long>>(emptyMap())
    val limitesApp: StateFlow<Map<String, Long>> = _limitesApp

    fun cargarUsos(uidHijo: String, desde: Long, hasta: Long) {
        viewModelScope.launch {
            try {
                val usos = repositorio.obtenerUsosPorFecha(uidHijo, desde, hasta)
                _listaUsos.value = usos
                Log.d("VistaModeloApps", "Usos cargados: ${usos.size} apps.")
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al cargar usos: ${e.message}", e)
            }
        }
    }
    fun cargarHijos(idPadre: String) {
        viewModelScope.launch {
            val hijos = repositorio.obtenerHijosVinculados(idPadre)
            _hijosVinculados.value = hijos
        }
    }

    // Cambia el estado de bloqueo (true para bloquear, false para desbloquear)
    fun cambiarEstadoBloqueo(uidHijo: String, paquete: String, bloquear: Boolean) {
        viewModelScope.launch {
            try {
                repositorio.bloquearApp(uidHijo, paquete, bloquear)
                // Actualiza el estado localmente para reflejar el cambio
                _estadoBloqueoApp.value = _estadoBloqueoApp.value.toMutableMap().apply {
                    put(paquete, bloquear)
                }
                Log.d("VistaModeloApps", "App $paquete ${if (bloquear) "bloqueada" else "desbloqueada"}.")
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al cambiar estado de bloqueo para $paquete: ${e.message}", e)
            }
        }
    }

    // Verifica si una app está bloqueada y actualiza el estado local
    fun verificarSiEstaBloqueada(uidHijo: String, paquete: String) {
        viewModelScope.launch {
            try {
                val bloqueada = repositorio.estaAppBloqueada(uidHijo, paquete)
                _estadoBloqueoApp.value = _estadoBloqueoApp.value.toMutableMap().apply {
                    put(paquete, bloqueada)
                }
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al verificar bloqueo para $paquete: ${e.message}", e)
            }
        }
    }
    fun cargarLimites(uidHijo: String) {
        viewModelScope.launch {
            try {
                // Esta llamada ahora usará la ruta y el campo correctos.
                val limites = repositorio.obtenerLimitesApps(uidHijo)
                _limitesApp.value = limites
                Log.d("VistaModeloApps", "Límites cargados: ${limites.size} apps.")
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al cargar límites: ${e.message}", e)
            }
        }
    }
    fun establecerLimite(uidHijo: String, paquete: String, tiempoLimite: Long) {
        viewModelScope.launch {
            try {
                // Esta llamada ahora usará la ruta y el campo correctos.
                repositorio.establecerLimiteApp(uidHijo, paquete, tiempoLimite)
                // Actualiza el estado localmente
                _limitesApp.value = _limitesApp.value.toMutableMap().apply {
                    put(paquete, tiempoLimite)
                }
                Log.d("VistaModeloApps", "Límite establecido para $paquete: $tiempoLimite ms.")
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al establecer límite para $paquete: ${e.message}", e)
            }
        }
    }


}
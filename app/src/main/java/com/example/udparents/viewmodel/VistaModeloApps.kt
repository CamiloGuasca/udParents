package com.example.udparents.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.udparents.modelo.AppUso
import com.example.udparents.modelo.RestriccionHorario
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
    private val _restriccionesHorario = MutableStateFlow<List<RestriccionHorario>>(emptyList())
    val restriccionesHorario: StateFlow<List<RestriccionHorario>> = _restriccionesHorario
    private val _tiempoPantallaDiario = MutableStateFlow<Map<String, Long>>(emptyMap())
    val tiempoPantallaDiario: StateFlow<Map<String, Long>> = _tiempoPantallaDiario
    private val _tiempoPantallaSemanal = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val tiempoPantallaSemanal: StateFlow<Map<Int, Long>> = _tiempoPantallaSemanal

    fun cargarRestriccionesHorario(uidHijo: String) {
        viewModelScope.launch {
            try {
                _restriccionesHorario.value = repositorio.obtenerRestriccionesHorario(uidHijo)
                Log.d("VistaModeloApps", "Restricciones de horario cargadas: ${_restriccionesHorario.value.size}")
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al cargar restricciones de horario: ${e.message}", e)
            }
        }
    }

    fun guardarRestriccionHorario(uidHijo: String, restriccion: RestriccionHorario) {
        viewModelScope.launch {
            try {
                repositorio.guardarRestriccionHorario(uidHijo, restriccion)
                cargarRestriccionesHorario(uidHijo) // Recargar para actualizar la UI
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al guardar restricción de horario: ${e.message}", e)
            }
        }
    }

    fun eliminarRestriccionHorario(uidHijo: String, restriccionId: String) {
        viewModelScope.launch {
            try {
                repositorio.eliminarRestriccionHorario(uidHijo, restriccionId)
                cargarRestriccionesHorario(uidHijo) // Recargar para actualizar la UI
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al eliminar restricción de horario: ${e.message}", e)
            }
        }
    }

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
    /**
     * Carga el resumen de tiempo de pantalla por día para un hijo específico.
     * Los datos se almacenan en _tiempoPantallaDiario.
     * @param uidHijo El UID del hijo del que se obtendrán los datos.
     */
    fun cargarResumenTiempoPantallaDiario(uidHijo: String) {
        viewModelScope.launch {
            try {
                // Llama a una nueva función en el repositorio (que crearemos después).
                val resumenDiario = repositorio.obtenerTiempoPantallaDiario(uidHijo)
                _tiempoPantallaDiario.value = resumenDiario
                Log.d("VistaModeloApps", "Resumen diario de tiempo de pantalla cargado: ${resumenDiario.size} días.")
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al cargar resumen diario: ${e.message}", e)
            }
        }
    }

    /**
     * Carga el resumen de tiempo de pantalla por semana para un hijo específico.
     * Los datos se almacenan en _tiempoPantallaSemanal.
     * @param uidHijo El UID del hijo del que se obtendrán los datos.
     */
    fun cargarResumenTiempoPantallaSemanal(uidHijo: String) {
        viewModelScope.launch {
            try {
                // Llama a una nueva función en el repositorio (que crearemos después).
                val resumenSemanal = repositorio.obtenerTiempoPantallaSemanal(uidHijo)
                _tiempoPantallaSemanal.value = resumenSemanal
                Log.d("VistaModeloApps", "Resumen semanal de tiempo de pantalla cargado: ${resumenSemanal.size} semanas.")
            } catch (e: Exception) {
                Log.e("VistaModeloApps", "Error al cargar resumen semanal: ${e.message}", e)
            }
        }
    }


}
package com.example.udparents.viewmodel

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

    fun cargarUsos(idHijo: String, desde: Long, hasta: Long) {
        viewModelScope.launch {
            println("🟢 Iniciando carga de usos para ID: $idHijo desde ${Date(desde)} hasta ${Date(hasta)}")
            val usos = repositorio.obtenerUsosPorFecha(idHijo, desde, hasta)
            println("📦 Se obtuvieron ${usos.size} registros de uso de aplicaciones.")
            usos.forEach {
                println("➡️ ${it.nombreApp} - ${Date(it.fechaUso)} - ${it.tiempoUso} ms")
            }
            _listaUsos.value = usos
        }
    }
    fun cargarHijos(idPadre: String) {
        viewModelScope.launch {
            val hijos = repositorio.obtenerHijosVinculados(idPadre)
            _hijosVinculados.value = hijos
        }
    }

}
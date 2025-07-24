package com.example.udparents.modelo

data class AppUso(
    val nombrePaquete: String = "",
    val nombreApp: String = "",
    val fechaUso: Long = 0L,
    val tiempoUso: Long = 0L // en milisegundos
)

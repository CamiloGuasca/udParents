package com.example.udparents.modelo

data class CodigoVinculacion(
    val codigo: String = "",
    val idPadre: String = "",
    val timestampCreacion: Long = 0L,
    val vinculado: Boolean = false,
    val dispositivoHijo: String = "",
    val nombreHijo: String = "",
    val edadHijo: Int = 0,
    val sexoHijo: String = ""
)

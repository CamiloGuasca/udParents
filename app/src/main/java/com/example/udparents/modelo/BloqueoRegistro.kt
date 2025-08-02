package com.example.udparents.modelo

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Clase de datos para representar un intento de acceso a una app bloqueada.
 * Se utiliza para registrar la información en Firestore.
 */
data class BloqueoRegistro(
    val nombreApp: String = "",
    val nombrePaquete: String = "",
    val uidHijo: String = "",
    val razon: String = "",
    @ServerTimestamp // Anota para que Firestore establezca automáticamente la hora del servidor
    val timestamp: Date? = null
)

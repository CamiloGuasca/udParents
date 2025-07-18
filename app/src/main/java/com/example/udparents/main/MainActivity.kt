package com.example.udparents.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.udparents.navegacion.NavegacionApp
import com.example.udparents.tema.UdParentsTheme

/**
 * Actividad principal que configura el contenido de la aplicación.
 * Aquí se aplica el tema y se lanza la navegación principal.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UdParentsTheme {
                NavegacionApp()
            }
        }
    }
}

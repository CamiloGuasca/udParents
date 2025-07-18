package com.example.udparents.navegacion

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.udparents.vista.pantallas.PantallaInicioSesion
import com.example.udparents.vista.pantallas.PantallaPrincipal
import com.example.udparents.vista.pantallas.PantallaRegistro
import com.example.udparents.vista.pantallas.PantallaRecuperarContrasena

/**
 * Rutas nombradas para facilitar la navegación.
 */
object Rutas {
    const val INICIO_SESION = "inicio_sesion"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    const val PRINCIPAL = "principal"

}

/**
 * Controlador de navegación que define las pantallas disponibles y su flujo.
 */
@Composable
fun NavegacionApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rutas.INICIO_SESION
    ) {
        // Pantalla de INICIO DE SESIÓN
        composable(Rutas.INICIO_SESION) {
            PantallaInicioSesion(
                onIniciarSesionExitoso = {
                    navController.navigate(Rutas.PRINCIPAL) {
                        popUpTo(Rutas.INICIO_SESION) { inclusive = true }
                    }
                },
                onIrARegistro = {
                    navController.navigate(Rutas.REGISTRO)
                },
                onOlvidoContrasena = {
                    navController.navigate(Rutas.RECUPERAR)
                }
            )
        }

        // Pantalla de REGISTRO
        composable(Rutas.REGISTRO) {
            PantallaRegistro(
                onRegistroExitoso = {
                    // Si el registro fue exitoso, volvemos al inicio de sesión
                    navController.navigate(Rutas.INICIO_SESION) {
                        popUpTo(Rutas.INICIO_SESION) { inclusive = true }
                    }
                },
                onIrAInicioSesion = {
                    navController.popBackStack(Rutas.INICIO_SESION, inclusive = false)
                }
            )
        }
        composable(Rutas.RECUPERAR) {
            PantallaRecuperarContrasena(
                onRecuperacionEnviada = {
                    navController.popBackStack(Rutas.INICIO_SESION, false)
                },
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
        composable(Rutas.PRINCIPAL) {
            PantallaPrincipal(
                onCerrarSesion = {
                    navController.navigate(Rutas.INICIO_SESION) {
                        popUpTo(Rutas.INICIO_SESION) { inclusive = true }
                    }
                }
            )
        }

    }
}

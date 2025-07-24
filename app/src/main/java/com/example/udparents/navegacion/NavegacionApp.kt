package com.example.udparents.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import com.example.udparents.viewmodel.VistaModeloUsuario
import com.example.udparents.vista.pantallas.PantallaBienvenida
import com.example.udparents.vista.pantallas.PantallaCodigoPadre
import com.example.udparents.vista.pantallas.PantallaDispositivosVinculados
import com.example.udparents.vista.pantallas.PantallaInicioSesion
import com.example.udparents.vista.pantallas.PantallaPrincipal
import com.example.udparents.vista.pantallas.PantallaRegistro
import com.example.udparents.vista.pantallas.PantallaRecuperarContrasena
import com.example.udparents.vista.pantallas.PantallaReporteApps
import com.example.udparents.vista.pantallas.PantallaVinculacionHijo

/**
 * Rutas nombradas para facilitar la navegación.
 */
object Rutas {
    const val INICIO_SESION = "inicio_sesion"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    const val PRINCIPAL = "principal"
    const val CODIGO_PADRE = "codigo_padre"
    const val VINCULACION_HIJO = "vinculacion_hijo"
    const val BIENVENIDA = "bienvenida"
    const val DISPOSITIVOS_VINCULADOS = "dispositivos_vinculados"
    const val REPORTE_APPS = "reporte_apps"


}

/**
 * Controlador de navegación que define las pantallas disponibles y su flujo.
 */
@Composable
fun NavegacionApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rutas.BIENVENIDA
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
            val viewModel = remember { VistaModeloUsuario() }

            PantallaRegistro(
                viewModel = viewModel,
                onRegistroExitoso = {
                    navController.navigate(Rutas.INICIO_SESION) {
                        popUpTo(Rutas.REGISTRO) { inclusive = true }
                    }
                },
                onIrAInicioSesion = {
                    navController.popBackStack(Rutas.INICIO_SESION, inclusive = false)
                }
            )
        }
        composable(Rutas.RECUPERAR) {
            val viewModel = remember { VistaModeloUsuario() }

            PantallaRecuperarContrasena(
                viewModel = viewModel,
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
                        popUpTo(0)
                        launchSingleTop = true
                    }
                },
                onIrAVinculacionPadre = {
                    navController.navigate(Rutas.CODIGO_PADRE)
                },
                onIrADispositivosVinculados = {
                    navController.navigate(Rutas.DISPOSITIVOS_VINCULADOS)
                },
                onIrAReporteApps = { hijos ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("hijosVinculados", hijos)
                    navController.navigate(Rutas.REPORTE_APPS)
                }

            )

        }


        composable(Rutas.CODIGO_PADRE) {
            PantallaCodigoPadre(
                onVolverAlMenuPrincipal = {
                    navController.navigate(Rutas.PRINCIPAL) {
                        popUpTo(Rutas.PRINCIPAL) { inclusive = true }
                    }
                }
            )
        }


        composable(Rutas.DISPOSITIVOS_VINCULADOS) {
            PantallaDispositivosVinculados(
                onVolverAlMenuPadre = {
                    navController.navigate(Rutas.PRINCIPAL) {
                        popUpTo(Rutas.PRINCIPAL) { inclusive = true }
                    }
                }
            )
        }


        composable(Rutas.VINCULACION_HIJO) {
            PantallaVinculacionHijo(
                vistaModelo = viewModel(),
                onVolverAlPadre = {
                    navController.navigate(Rutas.BIENVENIDA) {
                        popUpTo(Rutas.VINCULACION_HIJO) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.BIENVENIDA) {
            PantallaBienvenida(
                onPadreSeleccionado = {
                    navController.navigate(Rutas.INICIO_SESION)
                },
                onHijoSeleccionado = {
                    navController.navigate(Rutas.VINCULACION_HIJO)
                }
            )
        }

        composable(Rutas.REPORTE_APPS) {
            val hijos = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<Pair<String, String>>>("hijosVinculados")
                ?: emptyList()

            PantallaReporteApps(
                listaHijos = hijos,
                onVolver = {
                    navController.popBackStack()
                }
            )
        }



    }
}

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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.udparents.viewmodel.VistaModeloUsuario
import com.example.udparents.vista.pantallas.PantallaBienvenida
import com.example.udparents.vista.pantallas.PantallaCodigoPadre
import com.example.udparents.vista.pantallas.PantallaControlApps
import com.example.udparents.vista.pantallas.PantallaDispositivosVinculados
import com.example.udparents.vista.pantallas.PantallaInicioSesion
import com.example.udparents.vista.pantallas.PantallaPrincipal
import com.example.udparents.vista.pantallas.PantallaProgramarRestricciones
import com.example.udparents.vista.pantallas.PantallaRegistro
import com.example.udparents.vista.pantallas.PantallaRecuperarContrasena
import com.example.udparents.vista.pantallas.PantallaReporteApps
import com.example.udparents.vista.pantallas.PantallaSeleccionHijo
import com.example.udparents.vista.pantallas.PantallaVinculacionHijo
import com.example.udparents.vista.pantallas.PantallaResumenTiempoPantalla // Importa la nueva pantalla

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
    const val CONTROL_APPS = "control_apps"
    const val PROGRAMAR_RESTRICCIONES = "programar_restricciones"
    const val DETALLES_RESTRICCIONES = "programar_restricciones_detalles/{uidHijo}/{nombreHijo}"
    const val DETALLES_CONTROL = "control_apps/{uidHijo}"
    const val RESUMEN_TIEMPO = "resumen_tiempo" // NUEVO: Ruta para la pantalla de resumen de tiempo
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
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "hijosVinculados",
                        hijos
                    )
                    navController.navigate(Rutas.REPORTE_APPS)
                },
                onIrAControlApps = { hijos ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "hijosVinculados",
                        hijos
                    )
                    // Navegamos a la pantalla de selección con un título
                    navController.navigate(Rutas.CONTROL_APPS)
                },
                onIrAProgramarRestricciones = { hijos ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "hijosVinculados",
                        hijos
                    )
                    // Navegamos a la pantalla de selección con un título diferente
                    navController.navigate(Rutas.PROGRAMAR_RESTRICCIONES)
                },
                // NUEVO: Conecta el callback para la nueva pantalla
                onIrAResumenTiempoPantalla = { hijos ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "hijosVinculados",
                        hijos
                    )
                    navController.navigate(Rutas.RESUMEN_TIEMPO)
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
        // Ruta para la pantalla de selección de hijo para Control de Apps
        composable(Rutas.CONTROL_APPS) {
            val hijos = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<Pair<String, String>>>("hijosVinculados")
                ?: emptyList()

            PantallaSeleccionHijo(
                titulo = "Control de Aplicaciones",
                listaHijos = hijos,
                onHijoSeleccionado = { uidHijo, _ ->
                    navController.navigate(Rutas.DETALLES_CONTROL.replace("{uidHijo}", uidHijo))
                },
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
        // Ruta para la pantalla de selección de hijo para Programar Restricciones
        composable(Rutas.PROGRAMAR_RESTRICCIONES) {
            val hijos = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<Pair<String, String>>>("hijosVinculados")
                ?: emptyList()

            PantallaSeleccionHijo(
                titulo = "Programar Restricciones",
                listaHijos = hijos,
                onHijoSeleccionado = { uidHijo, nombreHijo ->
                    navController.navigate(Rutas.DETALLES_RESTRICCIONES.replace("{uidHijo}", uidHijo).replace("{nombreHijo}", nombreHijo))
                },
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
        // NUEVO: Composable para la pantalla de Resumen de Tiempo de Pantalla
        composable(Rutas.RESUMEN_TIEMPO) {
            val hijos = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<List<Pair<String, String>>>("hijosVinculados")
                ?: emptyList()

            PantallaResumenTiempoPantalla(
                onVolverAlMenuPadre = { navController.popBackStack() }
            )
        }
        // Ruta de destino para la pantalla de detalles de Control de Apps
        composable(
            route = Rutas.DETALLES_CONTROL,
            arguments = listOf(navArgument("uidHijo") { type = NavType.StringType })
        ) { backStackEntry ->
            val uidHijo = backStackEntry.arguments?.getString("uidHijo") ?: ""
            PantallaControlApps(uidHijo = uidHijo)
        }
        // Ruta de destino para la pantalla de detalles de Programar Restricciones
        composable(
            route = Rutas.DETALLES_RESTRICCIONES,
            arguments = listOf(
                navArgument("uidHijo") { type = NavType.StringType },
                navArgument("nombreHijo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uidHijo = backStackEntry.arguments?.getString("uidHijo")
            val nombreHijo = backStackEntry.arguments?.getString("nombreHijo")
            if (uidHijo != null && nombreHijo != null) {
                PantallaProgramarRestricciones(
                    uidHijo = uidHijo,
                    nombreHijo = nombreHijo,
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}

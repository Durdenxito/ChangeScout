package com.app.changescout.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.changescout.ui.screens.product.detail.PantallaDetalleProducto
import com.app.changescout.ui.screens.product.form.PantallaFormularioProducto
import com.app.changescout.ui.screens.radar.PantallaRadarProductos

@Composable
fun NavegacionChangeScout() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DestinoApp.RADAR_PRODUCTOS
    ) {
        composable(DestinoApp.RADAR_PRODUCTOS) {
            PantallaRadarProductos(
                onNavegarAFormulario = {
                    navController.navigate(DestinoApp.FORMULARIO_PRODUCTO)
                },
                onNavegarADetalle = { productoId ->
                    navController.navigate(DestinoApp.rutaDetalle(productoId))
                }
            )
        }

        composable(DestinoApp.FORMULARIO_PRODUCTO) {
            PantallaFormularioProducto(
                onNavegarAtras = { navController.popBackStack() }
            )
        }

        composable(
            route = DestinoApp.DETALLE_PRODUCTO,
            arguments = listOf(
                navArgument(DestinoApp.ARG_PRODUCTO_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            PantallaDetalleProducto(
                onNavegarAtras = { navController.popBackStack() }
            )
        }
    }
}

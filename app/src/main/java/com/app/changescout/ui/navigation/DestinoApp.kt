package com.app.changescout.ui.navigation

object DestinoApp {
    const val RADAR_PRODUCTOS = "radar_productos"
    const val FORMULARIO_PRODUCTO = "formulario_producto"
    const val DETALLE_PRODUCTO_BASE = "detalle_producto"
    const val ARG_PRODUCTO_ID = "productoId"
    const val DETALLE_PRODUCTO = "$DETALLE_PRODUCTO_BASE/{$ARG_PRODUCTO_ID}"

    fun rutaDetalle(productoId: Long): String = "$DETALLE_PRODUCTO_BASE/$productoId"
}

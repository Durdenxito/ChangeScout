package com.app.changescout.data.api.marketplace.demo

import com.app.changescout.domain.model.CondicionPublicacion
import com.app.changescout.domain.model.Moneda
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.repository.ProveedorMarketplace
import javax.inject.Inject

class ProveedorMarketplaceDemo @Inject constructor() : ProveedorMarketplace {
    override val nombreProveedor: String = NOMBRE_PROVEEDOR

    override suspend fun buscar(
        query: String,
        limit: Int
    ): ResultadoOperacion<List<PublicacionMercado>> {
        if (query.isBlank()) {
            return ResultadoOperacion.Fallo(
                ErrorOperacion.Validacion("El query de competencia no puede estar vacio.")
            )
        }

        val limiteSeguro = limit.coerceIn(1, PLANTILLAS_PUBLICACIONES.size)
        val factorPrecio = resolverFactorPrecio(query)
        val publicaciones = PLANTILLAS_PUBLICACIONES
            .take(limiteSeguro)
            .mapIndexed { index, plantilla ->
                plantilla.toPublicacionMercado(
                    index = index,
                    query = query,
                    factorPrecio = factorPrecio
                )
            }

        return ResultadoOperacion.Exito(publicaciones)
    }

    private fun resolverFactorPrecio(query: String): Double {
        val seed = query.lowercase().sumOf { char -> char.code }
        val ajuste = (seed % 9) - 4
        return 1.0 + (ajuste * 0.025)
    }

    private data class PlantillaPublicacion(
        val sufijoTitulo: String,
        val precioBasePen: Double,
        val condicion: CondicionPublicacion,
        val moneda: Moneda = Moneda.PEN
    ) {
        fun toPublicacionMercado(
            index: Int,
            query: String,
            factorPrecio: Double
        ): PublicacionMercado {
            val tituloNormalizado = query.trim().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }

            return PublicacionMercado(
                publicacionId = "demo-${query.hashCode()}-$index",
                titulo = "$tituloNormalizado $sufijoTitulo",
                precio = precioBasePen * factorPrecio,
                moneda = moneda,
                condicion = condicion,
                permalink = null,
                nombreFuente = NOMBRE_PROVEEDOR
            )
        }
    }

    private companion object {
        const val NOMBRE_PROVEEDOR = "Marketplace Demo"

        val PLANTILLAS_PUBLICACIONES = listOf(
            PlantillaPublicacion("nuevo sellado", 629.0, CondicionPublicacion.NUEVO),
            PlantillaPublicacion("nuevo entrega inmediata", 649.0, CondicionPublicacion.NUEVO),
            PlantillaPublicacion("nuevo garantia tienda", 619.0, CondicionPublicacion.NUEVO),
            PlantillaPublicacion("pack importado nuevo", 669.0, CondicionPublicacion.NUEVO),
            PlantillaPublicacion("oferta unidad nueva", 599.0, CondicionPublicacion.NUEVO),
            PlantillaPublicacion("usado excelente estado", 489.0, CondicionPublicacion.USADO),
            PlantillaPublicacion("repuesto compatible", 89.0, CondicionPublicacion.NUEVO),
            PlantillaPublicacion("accesorio funda premium", 59.0, CondicionPublicacion.NUEVO),
            PlantillaPublicacion("replica alternativa", 219.0, CondicionPublicacion.DESCONOCIDO),
            PlantillaPublicacion("nuevo caja abierta", 579.0, CondicionPublicacion.DESCONOCIDO)
        )
    }
}

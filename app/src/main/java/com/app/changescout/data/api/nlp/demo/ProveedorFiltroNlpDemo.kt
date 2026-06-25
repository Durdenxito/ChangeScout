package com.app.changescout.data.api.nlp.demo

import com.app.changescout.domain.model.CondicionPublicacion
import com.app.changescout.domain.model.Moneda
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.PublicacionComparable
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ResultadoFiltroNlp
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.repository.ProveedorFiltroNlp
import javax.inject.Inject

class ProveedorFiltroNlpDemo @Inject constructor() : ProveedorFiltroNlp {
    override val nombreProveedor: String = NOMBRE_PROVEEDOR

    override suspend fun filtrar(
        publicaciones: List<PublicacionMercado>,
        producto: ProductoImportado
    ): ResultadoOperacion<ResultadoFiltroNlp> {
        val decisiones = publicaciones.map { publicacion ->
            evaluarPublicacion(publicacion)
        }
        val validas = decisiones.mapNotNull { decision -> decision.publicacionComparable }
        val razonesDescarte = decisiones
            .mapNotNull { decision -> decision.razonDescarte }
            .groupingBy { razon -> razon }
            .eachCount()
            .map { (razon, cantidad) -> "$razon: $cantidad" }
        val precioPromedio = validas
            .takeIf { publicacionesValidas -> publicacionesValidas.isNotEmpty() }
            ?.map { publicacion -> publicacion.precioPen }
            ?.average()

        return ResultadoOperacion.Exito(
            ResultadoFiltroNlp(
                publicacionesValidas = validas,
                cantidadDescartadas = decisiones.count { decision ->
                    decision.publicacionComparable == null
                },
                razonesDescarte = razonesDescarte,
                precioPromedioRealPen = precioPromedio,
                competidoresValidos = validas.size,
                puntajeConfianza = calcularConfianza(
                    publicacionesTotales = publicaciones.size,
                    publicacionesValidas = validas.size
                ),
                trazaProveedor = construirTraza(producto, publicaciones.size, validas.size)
            )
        )
    }

    private fun evaluarPublicacion(
        publicacion: PublicacionMercado
    ): DecisionFiltro {
        val tituloNormalizado = publicacion.titulo.normalizarTitulo()

        return when {
            publicacion.precio <= 0.0 -> DecisionFiltro.descartada("precio no valido")
            publicacion.moneda != Moneda.PEN -> DecisionFiltro.descartada("moneda no PEN")
            publicacion.condicion == CondicionPublicacion.USADO -> DecisionFiltro.descartada("condicion usada")
            PALABRAS_CONTAMINANTES.any { palabra -> tituloNormalizado.contains(palabra) } -> {
                DecisionFiltro.descartada("titulo contaminado")
            }
            else -> DecisionFiltro.valida(
                PublicacionComparable(
                    publicacionOrigenId = publicacion.publicacionId,
                    tituloNormalizado = tituloNormalizado,
                    precioPen = publicacion.precio
                )
            )
        }
    }

    private fun calcularConfianza(
        publicacionesTotales: Int,
        publicacionesValidas: Int
    ): Double {
        if (publicacionesTotales == 0) return 0.0

        val proporcionValida = publicacionesValidas.toDouble() / publicacionesTotales.toDouble()
        val evidenciaPorVolumen = (publicacionesValidas * 0.08).coerceAtMost(0.32)
        return (0.45 + proporcionValida * 0.35 + evidenciaPorVolumen)
            .coerceIn(0.0, 0.95)
    }

    private fun construirTraza(
        producto: ProductoImportado,
        publicacionesTotales: Int,
        publicacionesValidas: Int
    ): String {
        return "proveedor=$NOMBRE_PROVEEDOR | producto=${producto.id} | " +
            "total=$publicacionesTotales | validas=$publicacionesValidas"
    }

    private fun String.normalizarTitulo(): String {
        return trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    private data class DecisionFiltro(
        val publicacionComparable: PublicacionComparable?,
        val razonDescarte: String?
    ) {
        companion object {
            fun valida(publicacion: PublicacionComparable): DecisionFiltro {
                return DecisionFiltro(publicacionComparable = publicacion, razonDescarte = null)
            }

            fun descartada(razon: String): DecisionFiltro {
                return DecisionFiltro(publicacionComparable = null, razonDescarte = razon)
            }
        }
    }

    private companion object {
        const val NOMBRE_PROVEEDOR = "Filtro NLP Demo"

        val PALABRAS_CONTAMINANTES = listOf(
            "usado",
            "usada",
            "repuesto",
            "reparacion",
            "accesorio",
            "funda",
            "case",
            "mica",
            "cable",
            "compatible",
            "replica",
            "imitacion",
            "alternativa",
            "caja abierta"
        )
    }
}

package com.app.changescout.data.importer

import com.app.changescout.BuildConfig
import com.app.changescout.domain.model.ComponentesCostoImportacion
import com.app.changescout.domain.model.EstadoEvaluacion
import com.app.changescout.domain.model.EvaluacionComercial
import com.app.changescout.domain.model.MetricasTendencia
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.VeredictoComercial
import com.app.changescout.domain.repository.RepositorioEvaluacionComercial
import com.app.changescout.domain.repository.RepositorioProductoImportado
import com.google.gson.Gson
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class ImportadorDatosLocales @Inject constructor(
    private val repositorioProducto: RepositorioProductoImportado,
    private val repositorioEvaluacion: RepositorioEvaluacionComercial,
    private val clock: Clock,
    private val gson: Gson
) {
    suspend fun importar(json: String): Int {
        check(BuildConfig.DEBUG) { "Importacion local disponible solo en debug." }

        val archivo = gson.fromJson(json, ArchivoDatosLocales::class.java)
        require(archivo.productos.isNotEmpty()) { "El archivo no trae productos." }

        var evaluacionesImportadas = 0
        archivo.productos.forEach { productoJson ->
            val productoId = repositorioProducto.upsert(productoJson.aProducto())
            productoJson.evaluaciones.forEach { evaluacion ->
                repositorioEvaluacion.guardarEvaluacion(evaluacion.aEvaluacion(productoId, clock.instant()))
                evaluacionesImportadas++
            }
        }
        return evaluacionesImportadas
    }
}

private data class ArchivoDatosLocales(
    val productos: List<ProductoLocalJson> = emptyList()
)

private data class ProductoLocalJson(
    val nombre: String = "",
    val queryCompetencia: String = "",
    val precioFobUsd: Double = 0.0,
    val fleteUsd: Double = 0.0,
    val seguroUsd: Double = 0.0,
    val arancelesUsd: Double = 0.0,
    val otrosCargosUsd: Double = 0.0,
    val cantidadDisponible: Int = 0,
    val margenObjetivoPct: Double = 20.0,
    val evaluaciones: List<EvaluacionLocalJson> = emptyList()
) {
    fun aProducto(): ProductoImportado {
        return ProductoImportado(
            id = 0L,
            nombre = nombre,
            queryCompetencia = queryCompetencia,
            componentesCosto = ComponentesCostoImportacion(
                precioFobUsd = precioFobUsd,
                fleteUsd = fleteUsd,
                seguroUsd = seguroUsd,
                arancelesUsd = arancelesUsd,
                otrosCargosUsd = otrosCargosUsd
            ),
            cantidadDisponible = cantidadDisponible,
            margenObjetivoPct = margenObjetivoPct
        )
    }
}

private data class EvaluacionLocalJson(
    val diasAtras: Long = 0,
    val costoTotalUsd: Double? = null,
    val costoTotalPen: Double? = null,
    val tipoCambioVentaUsdPen: Double? = null,
    val precioPromedioRealPen: Double? = null,
    val precioVentaSugeridoPen: Double? = null,
    val brechaPrecioSugeridoMercadoPct: Double? = null,
    val margenObjetivoPct: Double? = null,
    val competidoresValidos: Int = 0,
    val margenNetoPct: Double? = null,
    val erosionPrecioLocalPct: Double? = null,
    val variacionCompetidoresPct: Double? = null,
    val presionCambiariaPct: Double? = null,
    val ventanaHistoricaDias: Int = 0,
    val veredicto: String? = null,
    val estadoEvaluacion: String = "OBSOLETO"
) {
    fun aEvaluacion(productoId: Long, ahora: Instant): EvaluacionComercial {
        return EvaluacionComercial(
            evaluacionId = 0L,
            productoId = productoId,
            costoTotalUsd = costoTotalUsd,
            costoTotalPen = costoTotalPen,
            tipoCambioVentaUsdPen = tipoCambioVentaUsdPen,
            precioPromedioRealPen = precioPromedioRealPen,
            margenObjetivoPct = margenObjetivoPct,
            precioVentaSugeridoPen = precioVentaSugeridoPen,
            brechaPrecioSugeridoMercadoPct = brechaPrecioSugeridoMercadoPct,
            competidoresValidos = competidoresValidos,
            margenNetoPct = margenNetoPct,
            metricasTendencia = MetricasTendencia(
                erosionPrecioLocalPct = erosionPrecioLocalPct,
                variacionCompetidoresPct = variacionCompetidoresPct,
                presionCambiariaPct = presionCambiariaPct,
                ventanaHistoricaDias = ventanaHistoricaDias
            ),
            veredicto = veredicto?.aEnumOrNull<VeredictoComercial>(),
            estadoEvaluacion = estadoEvaluacion.aEnumOrNull<EstadoEvaluacion>() ?: EstadoEvaluacion.OBSOLETO,
            evaluadoEn = ahora.minusSeconds(diasAtras * SEGUNDOS_DIA),
            versionAlgoritmo = "seed-local-v1",
            trazaProveedor = "importacion-local"
        )
    }
}

private inline fun <reified T : Enum<T>> String.aEnumOrNull(): T? {
    return runCatching { enumValueOf<T>(this) }.getOrNull()
}

private const val SEGUNDOS_DIA = 86_400L

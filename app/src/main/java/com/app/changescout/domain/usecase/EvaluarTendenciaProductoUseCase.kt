package com.app.changescout.domain.usecase

import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.model.EstadoEvaluacion
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.model.ResultadoFiltroNlp
import com.app.changescout.domain.model.EvaluacionComercial
import com.app.changescout.domain.repository.ProveedorFiltroNlp
import com.app.changescout.domain.repository.ProveedorMarketplace
import com.app.changescout.domain.repository.ProveedorTipoCambio
import com.app.changescout.domain.repository.RepositorioEvaluacionComercial
import com.app.changescout.domain.repository.RepositorioProductoImportado
import com.app.changescout.domain.rules.CalculadoraLandedCost
import com.app.changescout.domain.rules.ClasificadorVeredictoComercial
import com.app.changescout.domain.rules.MotorTendenciaComercial
import com.app.changescout.domain.rules.PoliticaEvidencia
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class EvaluarTendenciaProductoUseCase @Inject constructor(
    private val repositorioProducto: RepositorioProductoImportado,
    private val repositorioEvaluacion: RepositorioEvaluacionComercial,
    private val proveedorTipoCambio: ProveedorTipoCambio,
    private val proveedorMarketplace: ProveedorMarketplace,
    private val proveedorFiltroNlp: ProveedorFiltroNlp,
    private val calculadoraLandedCost: CalculadoraLandedCost,
    private val politicaEvidencia: PoliticaEvidencia,
    private val motorTendencia: MotorTendenciaComercial,
    private val clasificadorVeredicto: ClasificadorVeredictoComercial,
    private val clock: Clock
) {
    suspend operator fun invoke(productoId: Long): ResultadoOperacion<EvaluacionComercial> {
        return try {
            evaluar(productoId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IllegalArgumentException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.Validacion(error.message ?: "La evaluacion no es valida.")
            )
        } catch (error: Exception) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.Desconocido(error.message ?: "No se pudo evaluar el producto.")
            )
        }
    }

    private suspend fun evaluar(productoId: Long): ResultadoOperacion<EvaluacionComercial> {
        val producto = repositorioProducto.obtenerPorId(productoId)
            ?: return ResultadoOperacion.Fallo(
                ErrorOperacion.SinDatos("No se encontro el producto importado.")
            )

        if (!producto.estaListoParaEvaluacion()) {
            return ResultadoOperacion.Fallo(
                ErrorOperacion.Validacion("El producto no tiene datos suficientes para evaluarse.")
            )
        }

        var causaObsolescencia: ErrorOperacion? = null

        val cotizacion = when (val resultado = proveedorTipoCambio.obtenerTasaVentaUsdPen()) {
            is ResultadoOperacion.Exito -> resultado.data
            is ResultadoOperacion.DatosObsoletos -> {
                causaObsolescencia = causaObsolescencia ?: resultado.causa
                resultado.data
            }
            is ResultadoOperacion.Fallo -> return resultado
        }

        val publicaciones = when (
            val resultado = proveedorMarketplace.buscar(
                query = producto.queryCompetencia,
                limit = LIMITE_PUBLICACIONES_MERCADO
            )
        ) {
            is ResultadoOperacion.Exito -> resultado.data
            is ResultadoOperacion.DatosObsoletos -> {
                causaObsolescencia = causaObsolescencia ?: resultado.causa
                resultado.data
            }
            is ResultadoOperacion.Fallo -> return resultado
        }

        val filtroNlp = when (val resultado = proveedorFiltroNlp.filtrar(publicaciones, producto)) {
            is ResultadoOperacion.Exito -> resultado.data
            is ResultadoOperacion.DatosObsoletos -> {
                causaObsolescencia = causaObsolescencia ?: resultado.causa
                resultado.data
            }
            is ResultadoOperacion.Fallo -> return resultado
        }

        val resultadoEvidencia = politicaEvidencia.evaluar(filtroNlp)
        val evidenciaSuficiente = resultadoEvidencia.esSuficiente
        val costoTotalUsd = calculadoraLandedCost.calcularCostoTotalUsd(producto.componentesCosto)
        val costoTotalPen = calculadoraLandedCost.calcularCostoTotalPen(
            costoTotalUsd = costoTotalUsd,
            tipoCambioVentaUsdPen = cotizacion.tasaVentaUsdPen
        )
        // ponytail: margen fijo por ahora; hacerlo editable cuando exista una necesidad real por producto.
        val margenObjetivoPct = MARGEN_OBJETIVO_PCT
        val precioVentaSugeridoPen = calculadoraLandedCost.calcularPrecioVentaSugeridoPen(
            costoTotalPen = costoTotalPen,
            margenObjetivoPct = margenObjetivoPct
        )
        val brechaPrecioSugeridoMercadoPct = if (evidenciaSuficiente) {
            filtroNlp.precioPromedioRealPen
                ?.takeIf { precioPromedio -> precioPromedio > 0.0 }
                ?.let { precioPromedio ->
                    ((precioPromedio - precioVentaSugeridoPen) / precioVentaSugeridoPen) * 100.0
                }
        } else {
            null
        }
        val margenNetoPct = if (evidenciaSuficiente) {
            calculadoraLandedCost.calcularMargenNetoPct(
                precioPromedioRealPen = requireNotNull(filtroNlp.precioPromedioRealPen),
                costoTotalPen = costoTotalPen
            )
        } else {
            null
        }

        val evaluacionBase = construirEvaluacionBase(
            productoId = producto.id,
            costoTotalUsd = costoTotalUsd,
            costoTotalPen = costoTotalPen,
            tipoCambioVentaUsdPen = cotizacion.tasaVentaUsdPen,
            filtroNlp = filtroNlp,
            margenObjetivoPct = margenObjetivoPct,
            precioVentaSugeridoPen = precioVentaSugeridoPen,
            brechaPrecioSugeridoMercadoPct = brechaPrecioSugeridoMercadoPct,
            margenNetoPct = margenNetoPct,
            causaObsolescencia = causaObsolescencia,
            motivoEvidenciaInsuficiente = resultadoEvidencia.motivoInsuficiente
        )
        val historial = repositorioEvaluacion.obtenerHistorial(
            productoId = producto.id,
            limite = LIMITE_HISTORIAL_TENDENCIA
        )
        val metricas = if (evidenciaSuficiente) {
            motorTendencia.calcular(evaluacionBase, historial)
        } else {
            null
        }
        val veredicto = clasificadorVeredicto.clasificar(
            margenNetoPct = margenNetoPct,
            evidenciaSuficiente = evidenciaSuficiente,
            metricasTendencia = metricas
        )
        val evaluacion = evaluacionBase.copy(
            metricasTendencia = metricas,
            veredicto = veredicto
        )

        repositorioEvaluacion.guardarEvaluacion(evaluacion)

        return causaObsolescencia?.let { causa ->
            ResultadoOperacion.DatosObsoletos(evaluacion, causa)
        } ?: ResultadoOperacion.Exito(evaluacion)
    }

    private fun construirEvaluacionBase(
        productoId: Long,
        costoTotalUsd: Double,
        costoTotalPen: Double,
        tipoCambioVentaUsdPen: Double,
        filtroNlp: ResultadoFiltroNlp,
        margenObjetivoPct: Double,
        precioVentaSugeridoPen: Double,
        brechaPrecioSugeridoMercadoPct: Double?,
        margenNetoPct: Double?,
        causaObsolescencia: ErrorOperacion?,
        motivoEvidenciaInsuficiente: String?
    ): EvaluacionComercial {
        return EvaluacionComercial(
            evaluacionId = 0L,
            productoId = productoId,
            costoTotalUsd = costoTotalUsd,
            costoTotalPen = costoTotalPen,
            tipoCambioVentaUsdPen = tipoCambioVentaUsdPen,
            precioPromedioRealPen = filtroNlp.precioPromedioRealPen,
            margenObjetivoPct = margenObjetivoPct,
            precioVentaSugeridoPen = precioVentaSugeridoPen,
            brechaPrecioSugeridoMercadoPct = brechaPrecioSugeridoMercadoPct,
            competidoresValidos = filtroNlp.competidoresValidos,
            margenNetoPct = margenNetoPct,
            metricasTendencia = null,
            veredicto = null,
            estadoEvaluacion = resolverEstadoEvaluacion(
                evidenciaSuficiente = margenNetoPct != null,
                causaObsolescencia = causaObsolescencia
            ),
            evaluadoEn = Instant.now(clock),
            versionAlgoritmo = VERSION_ALGORITMO,
            trazaProveedor = construirTrazaProveedor(filtroNlp),
            motivoEvidenciaInsuficiente = motivoEvidenciaInsuficiente
        )
    }

    private fun resolverEstadoEvaluacion(
        evidenciaSuficiente: Boolean,
        causaObsolescencia: ErrorOperacion?
    ): EstadoEvaluacion {
        return when {
            !evidenciaSuficiente -> EstadoEvaluacion.INCONCLUSO
            causaObsolescencia != null -> EstadoEvaluacion.OBSOLETO
            else -> EstadoEvaluacion.VIGENTE
        }
    }

    private fun construirTrazaProveedor(filtroNlp: ResultadoFiltroNlp): String {
        return listOf(
            "fx=${proveedorTipoCambio.nombreProveedor}",
            "marketplace=${proveedorMarketplace.nombreProveedor}",
            "nlp=${proveedorFiltroNlp.nombreProveedor}",
            filtroNlp.trazaProveedor
        )
            .filter { traza -> traza.isNotBlank() }
            .joinToString(separator = " | ")
    }

    private companion object {
        const val LIMITE_PUBLICACIONES_MERCADO = 5
        const val LIMITE_HISTORIAL_TENDENCIA = 12
        const val MARGEN_OBJETIVO_PCT = 20.0
        const val VERSION_ALGORITMO = "trend-v1"
    }
}

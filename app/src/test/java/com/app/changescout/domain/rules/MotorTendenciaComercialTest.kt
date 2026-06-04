package com.app.changescout.domain.rules

import com.app.changescout.domain.model.EstadoSnapshot
import com.app.changescout.domain.model.MetricasTendencia
import com.app.changescout.domain.model.SnapshotEvaluacionComercial
import com.app.changescout.domain.model.VeredictoComercial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MotorTendenciaComercialTest {
    private val motor = MotorTendenciaComercial()
    private val actualFecha = Instant.parse("2026-06-04T12:00:00Z")

    @Test
    fun calcular_sinHistorial_retornaMetricasNulasYVentanaCero() {
        val metricas = motor.calcular(
            actual = snapshotActual(),
            historial = emptyList()
        )

        assertNull(metricas.erosionPrecioLocalPct)
        assertNull(metricas.variacionCompetidoresPct)
        assertNull(metricas.presionCambiariaPct)
        assertEquals(0, metricas.ventanaHistoricaDias)
    }

    @Test
    fun calcular_calculaErosionContraPrecioPromedioHistorico() {
        val metricas = motor.calcular(
            actual = snapshotActual(precioPromedioRealPen = 88.0),
            historial = listOf(
                snapshotHistorico(precioPromedioRealPen = 100.0),
                snapshotHistorico(precioPromedioRealPen = 120.0)
            )
        )

        assertEquals(-20.0, metricas.erosionPrecioLocalPct ?: 0.0, 0.0001)
    }

    @Test
    fun calcular_calculaCrecimientoDeCompetidoresContraPromedioHistorico() {
        val metricas = motor.calcular(
            actual = snapshotActual(competidoresValidos = 30),
            historial = listOf(
                snapshotHistorico(competidoresValidos = 10),
                snapshotHistorico(competidoresValidos = 20)
            )
        )

        assertEquals(100.0, metricas.variacionCompetidoresPct ?: 0.0, 0.0001)
    }

    @Test
    fun calcular_calculaPresionCambiariaContraPromedioHistorico() {
        val metricas = motor.calcular(
            actual = snapshotActual(tipoCambioVentaUsdPen = 3.96),
            historial = listOf(
                snapshotHistorico(tipoCambioVentaUsdPen = 3.5),
                snapshotHistorico(tipoCambioVentaUsdPen = 3.7)
            )
        )

        assertEquals(10.0, metricas.presionCambiariaPct ?: 0.0, 0.0001)
    }

    @Test
    fun calcular_ignoraSnapshotsNoComparables() {
        val metricas = motor.calcular(
            actual = snapshotActual(precioPromedioRealPen = 90.0),
            historial = listOf(
                snapshotHistorico(productoId = 99L, precioPromedioRealPen = 10.0),
                snapshotHistorico(
                    precioPromedioRealPen = 20.0,
                    estadoSnapshot = EstadoSnapshot.FALLIDO
                ),
                snapshotHistorico(
                    precioPromedioRealPen = 30.0,
                    evaluadoEn = actualFecha.plusSeconds(60)
                ),
                snapshotHistorico(precioPromedioRealPen = 100.0)
            )
        )

        assertEquals(-10.0, metricas.erosionPrecioLocalPct ?: 0.0, 0.0001)
    }

    @Test
    fun calcular_calculaVentanaHistoricaEnDias() {
        val metricas = motor.calcular(
            actual = snapshotActual(),
            historial = listOf(
                snapshotHistorico(evaluadoEn = actualFecha.minusSeconds(2 * 24 * 60 * 60)),
                snapshotHistorico(evaluadoEn = actualFecha.minusSeconds(7 * 24 * 60 * 60))
            )
        )

        assertEquals(7, metricas.ventanaHistoricaDias)
    }

    private fun snapshotActual(
        productoId: Long = 1L,
        precioPromedioRealPen: Double? = 100.0,
        competidoresValidos: Int = 10,
        tipoCambioVentaUsdPen: Double? = 3.8
    ): SnapshotEvaluacionComercial {
        return snapshot(
            productoId = productoId,
            precioPromedioRealPen = precioPromedioRealPen,
            competidoresValidos = competidoresValidos,
            tipoCambioVentaUsdPen = tipoCambioVentaUsdPen,
            evaluadoEn = actualFecha
        )
    }

    private fun snapshotHistorico(
        productoId: Long = 1L,
        precioPromedioRealPen: Double? = 100.0,
        competidoresValidos: Int = 10,
        tipoCambioVentaUsdPen: Double? = 3.8,
        estadoSnapshot: EstadoSnapshot = EstadoSnapshot.OBSOLETO,
        evaluadoEn: Instant = actualFecha.minusSeconds(24 * 60 * 60)
    ): SnapshotEvaluacionComercial {
        return snapshot(
            productoId = productoId,
            precioPromedioRealPen = precioPromedioRealPen,
            competidoresValidos = competidoresValidos,
            tipoCambioVentaUsdPen = tipoCambioVentaUsdPen,
            estadoSnapshot = estadoSnapshot,
            evaluadoEn = evaluadoEn
        )
    }

    private fun snapshot(
        productoId: Long,
        precioPromedioRealPen: Double?,
        competidoresValidos: Int,
        tipoCambioVentaUsdPen: Double?,
        estadoSnapshot: EstadoSnapshot = EstadoSnapshot.VIGENTE,
        evaluadoEn: Instant
    ): SnapshotEvaluacionComercial {
        return SnapshotEvaluacionComercial(
            snapshotId = 1L,
            productoId = productoId,
            costoTotalUsd = 100.0,
            costoTotalPen = 380.0,
            tipoCambioVentaUsdPen = tipoCambioVentaUsdPen,
            precioPromedioRealPen = precioPromedioRealPen,
            competidoresValidos = competidoresValidos,
            margenNetoPct = 20.0,
            metricasTendencia = MetricasTendencia(
                erosionPrecioLocalPct = null,
                variacionCompetidoresPct = null,
                presionCambiariaPct = null,
                ventanaHistoricaDias = 0
            ),
            veredicto = VeredictoComercial.SALUDABLE,
            estadoSnapshot = estadoSnapshot,
            evaluadoEn = evaluadoEn,
            versionAlgoritmo = "test",
            trazaProveedor = null
        )
    }
}

package com.app.changescout.domain.rules

import com.app.changescout.domain.model.EstadoSnapshot
import com.app.changescout.domain.model.MetricasTendencia
import com.app.changescout.domain.model.SnapshotEvaluacionComercial
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class MotorTendenciaComercial @Inject constructor() {
    fun calcular(
        actual: SnapshotEvaluacionComercial,
        historial: List<SnapshotEvaluacionComercial>
    ): MetricasTendencia {
        val historialValido = historial
            .filter { snapshot -> snapshot.esComparableCon(actual) }
            .sortedByDescending { snapshot -> snapshot.evaluadoEn }

        return MetricasTendencia(
            erosionPrecioLocalPct = calcularVariacionContraPromedio(
                actual = actual.precioPromedioRealPen,
                historicos = historialValido.mapNotNull { it.precioPromedioRealPen }
            ),
            variacionCompetidoresPct = calcularVariacionContraPromedio(
                actual = actual.competidoresValidos.toDouble(),
                historicos = historialValido.map { it.competidoresValidos.toDouble() }
            ),
            presionCambiariaPct = calcularVariacionContraPromedio(
                actual = actual.tipoCambioVentaUsdPen,
                historicos = historialValido.mapNotNull { it.tipoCambioVentaUsdPen }
            ),
            ventanaHistoricaDias = calcularVentanaHistoricaDias(
                actual = actual,
                historial = historialValido
            )
        )
    }

    private fun SnapshotEvaluacionComercial.esComparableCon(
        actual: SnapshotEvaluacionComercial
    ): Boolean {
        return productoId == actual.productoId &&
            evaluadoEn.isBefore(actual.evaluadoEn) &&
            estadoSnapshot != EstadoSnapshot.FALLIDO &&
            estadoSnapshot != EstadoSnapshot.INCONCLUSO
    }

    private fun calcularVariacionContraPromedio(
        actual: Double?,
        historicos: List<Double>
    ): Double? {
        if (actual == null || actual < 0.0) return null

        val valoresHistoricos = historicos.filter { it > 0.0 }
        if (valoresHistoricos.isEmpty()) return null

        val promedioHistorico = valoresHistoricos.average()
        if (promedioHistorico <= 0.0) return null

        return ((actual - promedioHistorico) / promedioHistorico) * 100.0
    }

    private fun calcularVentanaHistoricaDias(
        actual: SnapshotEvaluacionComercial,
        historial: List<SnapshotEvaluacionComercial>
    ): Int {
        val masAntiguo = historial.minByOrNull { it.evaluadoEn } ?: return 0
        return ChronoUnit.DAYS
            .between(masAntiguo.evaluadoEn, actual.evaluadoEn)
            .toInt()
            .coerceAtLeast(0)
    }
}

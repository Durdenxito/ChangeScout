package com.app.changescout.domain.model

import java.time.Instant

data class SnapshotEvaluacionComercial(
    val snapshotId: Long,
    val productoId: Long,
    val costoTotalUsd: Double?,
    val costoTotalPen: Double?,
    val tipoCambioVentaUsdPen: Double?,
    val precioPromedioRealPen: Double?,
    val competidoresValidos: Int,
    val margenNetoPct: Double?,
    val metricasTendencia: MetricasTendencia?,
    val veredicto: VeredictoComercial?,
    val estadoSnapshot: EstadoSnapshot,
    val evaluadoEn: Instant,
    val versionAlgoritmo: String,
    val trazaProveedor: String?
) {
    fun esConclusivo(): Boolean {
        return estadoSnapshot != EstadoSnapshot.INCONCLUSO &&
            estadoSnapshot != EstadoSnapshot.FALLIDO &&
            veredicto != null &&
            veredicto != VeredictoComercial.INCONCLUSO
    }
}

data class MetricasTendencia(
    val erosionPrecioLocalPct: Double?,
    val variacionCompetidoresPct: Double?,
    val presionCambiariaPct: Double?,
    val ventanaHistoricaDias: Int
)

enum class VeredictoComercial {
    SALUDABLE,
    PRECAUCION,
    ALERTA_TEMPRANA_QUIEBRE,
    LIQUIDACION,
    INCONCLUSO
}

enum class EstadoSnapshot {
    VIGENTE,
    OBSOLETO,
    INCONCLUSO,
    FALLIDO
}

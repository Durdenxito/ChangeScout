package com.app.changescout.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.app.changescout.domain.model.EstadoSnapshot
import com.app.changescout.domain.model.MetricasTendencia
import com.app.changescout.domain.model.SnapshotEvaluacionComercial
import com.app.changescout.domain.model.VeredictoComercial
import java.time.Instant

@Entity(
    tableName = "snapshots_evaluacion_comercial",
    foreignKeys = [
        ForeignKey(
            entity = ProductoImportadoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productoId"]),
        Index(value = ["productoId", "evaluadoEnEpochMillis"])
    ]
)
data class SnapshotEvaluacionComercialEntity(
    @PrimaryKey(autoGenerate = true)
    val snapshotId: Long = 0L,
    val productoId: Long,
    val costoTotalUsd: Double?,
    val costoTotalPen: Double?,
    val tipoCambioVentaUsdPen: Double?,
    val precioPromedioRealPen: Double?,
    val competidoresValidos: Int,
    val margenNetoPct: Double?,
    val erosionPrecioLocalPct: Double?,
    val variacionCompetidoresPct: Double?,
    val presionCambiariaPct: Double?,
    val ventanaHistoricaDias: Int?,
    val veredicto: String?,
    val estadoSnapshot: String,
    val evaluadoEnEpochMillis: Long,
    val versionAlgoritmo: String,
    val trazaProveedor: String?
) {
    fun toDomain(): SnapshotEvaluacionComercial {
        return SnapshotEvaluacionComercial(
            snapshotId = snapshotId,
            productoId = productoId,
            costoTotalUsd = costoTotalUsd,
            costoTotalPen = costoTotalPen,
            tipoCambioVentaUsdPen = tipoCambioVentaUsdPen,
            precioPromedioRealPen = precioPromedioRealPen,
            competidoresValidos = competidoresValidos,
            margenNetoPct = margenNetoPct,
            metricasTendencia = toMetricasTendencia(),
            veredicto = veredicto?.toEnumOrNull<VeredictoComercial>(),
            estadoSnapshot = estadoSnapshot.toEnumOrNull<EstadoSnapshot>() ?: EstadoSnapshot.FALLIDO,
            evaluadoEn = Instant.ofEpochMilli(evaluadoEnEpochMillis),
            versionAlgoritmo = versionAlgoritmo,
            trazaProveedor = trazaProveedor
        )
    }

    private fun toMetricasTendencia(): MetricasTendencia? {
        if (
            erosionPrecioLocalPct == null &&
            variacionCompetidoresPct == null &&
            presionCambiariaPct == null &&
            ventanaHistoricaDias == null
        ) {
            return null
        }

        return MetricasTendencia(
            erosionPrecioLocalPct = erosionPrecioLocalPct,
            variacionCompetidoresPct = variacionCompetidoresPct,
            presionCambiariaPct = presionCambiariaPct,
            ventanaHistoricaDias = ventanaHistoricaDias ?: 0
        )
    }

    companion object {
        fun fromDomain(snapshot: SnapshotEvaluacionComercial): SnapshotEvaluacionComercialEntity {
            return SnapshotEvaluacionComercialEntity(
                snapshotId = snapshot.snapshotId,
                productoId = snapshot.productoId,
                costoTotalUsd = snapshot.costoTotalUsd,
                costoTotalPen = snapshot.costoTotalPen,
                tipoCambioVentaUsdPen = snapshot.tipoCambioVentaUsdPen,
                precioPromedioRealPen = snapshot.precioPromedioRealPen,
                competidoresValidos = snapshot.competidoresValidos,
                margenNetoPct = snapshot.margenNetoPct,
                erosionPrecioLocalPct = snapshot.metricasTendencia?.erosionPrecioLocalPct,
                variacionCompetidoresPct = snapshot.metricasTendencia?.variacionCompetidoresPct,
                presionCambiariaPct = snapshot.metricasTendencia?.presionCambiariaPct,
                ventanaHistoricaDias = snapshot.metricasTendencia?.ventanaHistoricaDias,
                veredicto = snapshot.veredicto?.name,
                estadoSnapshot = snapshot.estadoSnapshot.name,
                evaluadoEnEpochMillis = snapshot.evaluadoEn.toEpochMilli(),
                versionAlgoritmo = snapshot.versionAlgoritmo,
                trazaProveedor = snapshot.trazaProveedor
            )
        }
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? {
    return runCatching { enumValueOf<T>(this) }.getOrNull()
}

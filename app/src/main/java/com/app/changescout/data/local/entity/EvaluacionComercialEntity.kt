package com.app.changescout.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.app.changescout.domain.model.EstadoEvaluacion
import com.app.changescout.domain.model.EvaluacionComercial
import com.app.changescout.domain.model.MetricasTendencia
import com.app.changescout.domain.model.VeredictoComercial
import java.time.Instant

@Entity(
    tableName = "evaluaciones_comerciales",
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
data class EvaluacionComercialEntity(
    @PrimaryKey(autoGenerate = true)
    val evaluacionId: Long = 0L,
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
    val estadoEvaluacion: String,
    val evaluadoEnEpochMillis: Long,
    val versionAlgoritmo: String,
    val trazaProveedor: String?
) {
    fun toDomain(): EvaluacionComercial {
        return EvaluacionComercial(
            evaluacionId = evaluacionId,
            productoId = productoId,
            costoTotalUsd = costoTotalUsd,
            costoTotalPen = costoTotalPen,
            tipoCambioVentaUsdPen = tipoCambioVentaUsdPen,
            precioPromedioRealPen = precioPromedioRealPen,
            competidoresValidos = competidoresValidos,
            margenNetoPct = margenNetoPct,
            metricasTendencia = toMetricasTendencia(),
            veredicto = veredicto?.toEnumOrNull<VeredictoComercial>(),
            estadoEvaluacion = estadoEvaluacion.toEnumOrNull<EstadoEvaluacion>()
                ?: EstadoEvaluacion.FALLIDO,
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
        fun fromDomain(evaluacion: EvaluacionComercial): EvaluacionComercialEntity {
            return EvaluacionComercialEntity(
                evaluacionId = evaluacion.evaluacionId,
                productoId = evaluacion.productoId,
                costoTotalUsd = evaluacion.costoTotalUsd,
                costoTotalPen = evaluacion.costoTotalPen,
                tipoCambioVentaUsdPen = evaluacion.tipoCambioVentaUsdPen,
                precioPromedioRealPen = evaluacion.precioPromedioRealPen,
                competidoresValidos = evaluacion.competidoresValidos,
                margenNetoPct = evaluacion.margenNetoPct,
                erosionPrecioLocalPct = evaluacion.metricasTendencia?.erosionPrecioLocalPct,
                variacionCompetidoresPct = evaluacion.metricasTendencia?.variacionCompetidoresPct,
                presionCambiariaPct = evaluacion.metricasTendencia?.presionCambiariaPct,
                ventanaHistoricaDias = evaluacion.metricasTendencia?.ventanaHistoricaDias,
                veredicto = evaluacion.veredicto?.name,
                estadoEvaluacion = evaluacion.estadoEvaluacion.name,
                evaluadoEnEpochMillis = evaluacion.evaluadoEn.toEpochMilli(),
                versionAlgoritmo = evaluacion.versionAlgoritmo,
                trazaProveedor = evaluacion.trazaProveedor
            )
        }
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? {
    return runCatching { enumValueOf<T>(this) }.getOrNull()
}

package com.app.changescout.domain.rules

import com.app.changescout.domain.model.EstadoSnapshot
import com.app.changescout.domain.model.SnapshotEvaluacionComercial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PoliticaObsolescenciaSnapshotTest {
    private val politica = PoliticaObsolescenciaSnapshot()
    private val base = Instant.parse("2026-06-04T12:00:00Z")

    @Test
    fun estaVigente_retornaTrueDentroDeVentana() {
        assertTrue(
            politica.estaVigente(
                evaluadoEn = base,
                now = base.plusSeconds(11 * 60 * 60)
            )
        )
    }

    @Test
    fun estaVigente_retornaFalseFueraDeVentana() {
        assertFalse(
            politica.estaVigente(
                evaluadoEn = base,
                now = base.plusSeconds(13 * 60 * 60)
            )
        )
    }

    @Test
    fun estaVigente_rechazaNowAnteriorAEvaluacion() {
        assertThrows(IllegalArgumentException::class.java) {
            politica.estaVigente(
                evaluadoEn = base,
                now = base.minusSeconds(1)
            )
        }
    }

    @Test
    fun resolverEstado_preservaSnapshotFallido() {
        val snapshot = snapshot(EstadoSnapshot.FALLIDO)

        val estado = politica.resolverEstado(
            snapshot = snapshot,
            now = base.plusSeconds(20 * 60 * 60)
        )

        assertEquals(EstadoSnapshot.FALLIDO, estado)
    }

    @Test
    fun resolverEstado_marcaObsoletoSiSuperaVentana() {
        val snapshot = snapshot(EstadoSnapshot.VIGENTE)

        val estado = politica.resolverEstado(
            snapshot = snapshot,
            now = base.plusSeconds(13 * 60 * 60)
        )

        assertEquals(EstadoSnapshot.OBSOLETO, estado)
    }

    private fun snapshot(estado: EstadoSnapshot): SnapshotEvaluacionComercial {
        return SnapshotEvaluacionComercial(
            snapshotId = 1L,
            productoId = 1L,
            costoTotalUsd = null,
            costoTotalPen = null,
            tipoCambioVentaUsdPen = null,
            precioPromedioRealPen = null,
            competidoresValidos = 0,
            margenNetoPct = null,
            metricasTendencia = null,
            veredicto = null,
            estadoSnapshot = estado,
            evaluadoEn = base,
            versionAlgoritmo = "test",
            trazaProveedor = null
        )
    }
}

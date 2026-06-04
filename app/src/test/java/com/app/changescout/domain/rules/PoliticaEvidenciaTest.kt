package com.app.changescout.domain.rules

import com.app.changescout.domain.model.ResultadoFiltroNlp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaEvidenciaTest {
    private val politica = PoliticaEvidencia()

    @Test
    fun tieneEvidenciaSuficiente_retornaTrueConPrecioCompetidoresYConfianza() {
        val resultado = resultadoFiltro(
            precioPromedioRealPen = 100.0,
            competidoresValidos = 3,
            puntajeConfianza = 0.8
        )

        assertTrue(politica.tieneEvidenciaSuficiente(resultado))
    }

    @Test
    fun tieneEvidenciaSuficiente_retornaFalseSinPrecioPromedio() {
        val resultado = resultadoFiltro(
            precioPromedioRealPen = null,
            competidoresValidos = 5,
            puntajeConfianza = 0.9
        )

        assertFalse(politica.tieneEvidenciaSuficiente(resultado))
    }

    @Test
    fun tieneEvidenciaSuficiente_retornaFalseConPocosCompetidores() {
        val resultado = resultadoFiltro(
            precioPromedioRealPen = 100.0,
            competidoresValidos = 2,
            puntajeConfianza = 0.9
        )

        assertFalse(politica.tieneEvidenciaSuficiente(resultado))
    }

    @Test
    fun tieneEvidenciaSuficiente_aceptaConfianzaNula() {
        val resultado = resultadoFiltro(
            precioPromedioRealPen = 100.0,
            competidoresValidos = 3,
            puntajeConfianza = null
        )

        assertTrue(politica.tieneEvidenciaSuficiente(resultado))
    }

    @Test
    fun tieneEvidenciaSuficiente_retornaFalseConConfianzaBaja() {
        val resultado = resultadoFiltro(
            precioPromedioRealPen = 100.0,
            competidoresValidos = 3,
            puntajeConfianza = 0.4
        )

        assertFalse(politica.tieneEvidenciaSuficiente(resultado))
    }

    private fun resultadoFiltro(
        precioPromedioRealPen: Double?,
        competidoresValidos: Int,
        puntajeConfianza: Double?
    ): ResultadoFiltroNlp {
        return ResultadoFiltroNlp(
            publicacionesValidas = emptyList(),
            cantidadDescartadas = 0,
            razonesDescarte = emptyList(),
            precioPromedioRealPen = precioPromedioRealPen,
            competidoresValidos = competidoresValidos,
            puntajeConfianza = puntajeConfianza,
            trazaProveedor = "test"
        )
    }
}

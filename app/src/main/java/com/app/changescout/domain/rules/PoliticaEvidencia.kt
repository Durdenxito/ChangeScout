package com.app.changescout.domain.rules

import com.app.changescout.domain.model.ResultadoFiltroNlp

class PoliticaEvidencia(
    private val minimoCompetidoresValidos: Int = 3,
    private val puntajeConfianzaMinimo: Double = 0.65
) {
    fun tieneEvidenciaSuficiente(resultado: ResultadoFiltroNlp): Boolean {
        val tienePrecio = resultado.precioPromedioRealPen != null &&
            resultado.precioPromedioRealPen > 0.0
        val tieneCompetidores = resultado.competidoresValidos >= minimoCompetidoresValidos
        val tieneConfianza = resultado.puntajeConfianza == null ||
            resultado.puntajeConfianza >= puntajeConfianzaMinimo

        return tienePrecio && tieneCompetidores && tieneConfianza
    }
}

package com.app.changescout.domain.rules

import com.app.changescout.domain.model.EstadoEvaluacion
import com.app.changescout.domain.model.EvaluacionComercial
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class PoliticaObsolescenciaEvaluacion @Inject constructor(
    private val ventanaVigencia: Duration = Duration.ofHours(12)
) {
    fun estaVigente(
        evaluadoEn: Instant,
        now: Instant
    ): Boolean {
        require(!now.isBefore(evaluadoEn)) {
            "La fecha actual no puede ser anterior a la evaluacion."
        }

        return Duration.between(evaluadoEn, now) <= ventanaVigencia
    }

    fun resolverEstado(
        evaluacion: EvaluacionComercial,
        now: Instant
    ): EstadoEvaluacion {
        if (
            evaluacion.estadoEvaluacion == EstadoEvaluacion.FALLIDO ||
            evaluacion.estadoEvaluacion == EstadoEvaluacion.INCONCLUSO
        ) {
            return evaluacion.estadoEvaluacion
        }

        return if (estaVigente(evaluacion.evaluadoEn, now)) {
            EstadoEvaluacion.VIGENTE
        } else {
            EstadoEvaluacion.OBSOLETO
        }
    }
}

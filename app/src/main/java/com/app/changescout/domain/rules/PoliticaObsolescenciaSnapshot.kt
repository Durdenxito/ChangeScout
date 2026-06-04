package com.app.changescout.domain.rules

import com.app.changescout.domain.model.EstadoSnapshot
import com.app.changescout.domain.model.SnapshotEvaluacionComercial
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class PoliticaObsolescenciaSnapshot @Inject constructor(
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
        snapshot: SnapshotEvaluacionComercial,
        now: Instant
    ): EstadoSnapshot {
        if (
            snapshot.estadoSnapshot == EstadoSnapshot.FALLIDO ||
            snapshot.estadoSnapshot == EstadoSnapshot.INCONCLUSO
        ) {
            return snapshot.estadoSnapshot
        }

        return if (estaVigente(snapshot.evaluadoEn, now)) {
            EstadoSnapshot.VIGENTE
        } else {
            EstadoSnapshot.OBSOLETO
        }
    }
}

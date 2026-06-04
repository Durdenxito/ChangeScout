package com.app.changescout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.app.changescout.data.local.entity.EvaluacionComercialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluacionComercialDao {
    @Query(
        """
        SELECT *
        FROM snapshots_evaluacion_comercial
        WHERE productoId = :productoId
        ORDER BY evaluadoEnEpochMillis DESC
        LIMIT 1
        """
    )
    fun observarUltimo(productoId: Long): Flow<EvaluacionComercialEntity?>

    @Query(
        """
        SELECT evaluacion.*
        FROM snapshots_evaluacion_comercial AS evaluacion
        INNER JOIN (
            SELECT productoId, MAX(evaluadoEnEpochMillis) AS ultimoEvaluadoEn
            FROM snapshots_evaluacion_comercial
            GROUP BY productoId
        ) AS ultimo
        ON evaluacion.productoId = ultimo.productoId
        AND evaluacion.evaluadoEnEpochMillis = ultimo.ultimoEvaluadoEn
        ORDER BY evaluacion.evaluadoEnEpochMillis DESC
        """
    )
    fun observarUltimosDeTodos(): Flow<List<EvaluacionComercialEntity>>

    @Query(
        """
        SELECT *
        FROM snapshots_evaluacion_comercial
        WHERE productoId = :productoId
        ORDER BY evaluadoEnEpochMillis DESC
        LIMIT :limite
        """
    )
    suspend fun obtenerHistorial(
        productoId: Long,
        limite: Int
    ): List<EvaluacionComercialEntity>

    @Insert
    suspend fun insertar(evaluacion: EvaluacionComercialEntity): Long
}

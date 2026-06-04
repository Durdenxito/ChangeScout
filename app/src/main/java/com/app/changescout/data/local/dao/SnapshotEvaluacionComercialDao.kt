package com.app.changescout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.app.changescout.data.local.entity.SnapshotEvaluacionComercialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotEvaluacionComercialDao {
    @Query(
        """
        SELECT *
        FROM snapshots_evaluacion_comercial
        WHERE productoId = :productoId
        ORDER BY evaluadoEnEpochMillis DESC
        LIMIT 1
        """
    )
    fun observarUltimo(productoId: Long): Flow<SnapshotEvaluacionComercialEntity?>

    @Query(
        """
        SELECT snapshot.*
        FROM snapshots_evaluacion_comercial AS snapshot
        INNER JOIN (
            SELECT productoId, MAX(evaluadoEnEpochMillis) AS ultimoEvaluadoEn
            FROM snapshots_evaluacion_comercial
            GROUP BY productoId
        ) AS ultimo
        ON snapshot.productoId = ultimo.productoId
        AND snapshot.evaluadoEnEpochMillis = ultimo.ultimoEvaluadoEn
        ORDER BY snapshot.evaluadoEnEpochMillis DESC
        """
    )
    fun observarUltimosDeTodos(): Flow<List<SnapshotEvaluacionComercialEntity>>

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
    ): List<SnapshotEvaluacionComercialEntity>

    @Insert
    suspend fun insertar(snapshot: SnapshotEvaluacionComercialEntity): Long
}

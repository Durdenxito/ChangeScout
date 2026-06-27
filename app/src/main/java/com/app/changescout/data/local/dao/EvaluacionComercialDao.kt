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
        FROM evaluaciones_comerciales
        WHERE usuarioId = :usuarioId AND productoId = :productoId
        ORDER BY evaluadoEnEpochMillis DESC
        LIMIT 1
        """
    )
    fun observarUltimo(usuarioId: String, productoId: Long): Flow<EvaluacionComercialEntity?>

    @Query(
        """
        SELECT evaluacion.*
        FROM evaluaciones_comerciales AS evaluacion
        INNER JOIN (
            SELECT productoId, MAX(evaluadoEnEpochMillis) AS ultimoEvaluadoEn
            FROM evaluaciones_comerciales
            WHERE usuarioId = :usuarioId
            GROUP BY productoId
        ) AS ultimo
        ON evaluacion.productoId = ultimo.productoId
        AND evaluacion.evaluadoEnEpochMillis = ultimo.ultimoEvaluadoEn
        WHERE evaluacion.usuarioId = :usuarioId
        ORDER BY evaluacion.evaluadoEnEpochMillis DESC
        """
    )
    fun observarUltimosDeTodos(usuarioId: String): Flow<List<EvaluacionComercialEntity>>

    @Query(
        """
        SELECT *
        FROM evaluaciones_comerciales
        WHERE usuarioId = :usuarioId AND productoId = :productoId
        ORDER BY evaluadoEnEpochMillis DESC
        LIMIT :limite
        """
    )
    suspend fun obtenerHistorial(
        usuarioId: String,
        productoId: Long,
        limite: Int
    ): List<EvaluacionComercialEntity>

    @Insert
    suspend fun insertar(evaluacion: EvaluacionComercialEntity): Long
}

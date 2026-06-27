package com.app.changescout.data.repository

import com.app.changescout.data.auth.AlmacenSesion
import com.app.changescout.data.local.dao.ProductoImportadoDao
import com.app.changescout.data.local.dao.EvaluacionComercialDao
import com.app.changescout.data.local.entity.ProductoImportadoEntity
import com.app.changescout.data.local.entity.EvaluacionComercialEntity
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.EvaluacionComercial
import com.app.changescout.domain.repository.RepositorioEvaluacionComercial
import com.app.changescout.domain.repository.RepositorioProductoImportado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RepositorioProductoImportadoRoom(
    private val productoDao: ProductoImportadoDao,
    private val almacenSesion: AlmacenSesion
) : RepositorioProductoImportado {
    override fun observarTodos(): Flow<List<ProductoImportado>> {
        return productoDao.observarTodos(usuarioIdActual())
            .map { productos -> productos.map { it.toDomain() } }
    }

    override fun observarPorId(productoId: Long): Flow<ProductoImportado?> {
        return productoDao.observarPorId(usuarioIdActual(), productoId)
            .map { producto -> producto?.toDomain() }
    }

    override suspend fun obtenerPorId(productoId: Long): ProductoImportado? {
        return productoDao.obtenerPorId(usuarioIdActual(), productoId)?.toDomain()
    }

    override suspend fun upsert(producto: ProductoImportado): Long {
        return productoDao.upsert(
            ProductoImportadoEntity.fromDomain(producto, usuarioIdActual())
        )
    }

    override suspend fun eliminar(productoId: Long) {
        productoDao.eliminar(usuarioIdActual(), productoId)
    }

    private fun usuarioIdActual(): String = almacenSesion.usuarioIdActual() ?: USUARIO_SIN_SESION
}

class RepositorioEvaluacionComercialRoom(
    private val evaluacionDao: EvaluacionComercialDao,
    private val almacenSesion: AlmacenSesion
) : RepositorioEvaluacionComercial {
    override fun observarUltimo(productoId: Long): Flow<EvaluacionComercial?> {
        return evaluacionDao.observarUltimo(usuarioIdActual(), productoId)
            .map { evaluacion -> evaluacion?.toDomain() }
    }

    override fun observarUltimosDeTodos(): Flow<List<EvaluacionComercial>> {
        return evaluacionDao.observarUltimosDeTodos(usuarioIdActual())
            .map { evaluaciones -> evaluaciones.map { it.toDomain() } }
    }

    override suspend fun obtenerHistorial(
        productoId: Long,
        limite: Int
    ): List<EvaluacionComercial> {
        return evaluacionDao.obtenerHistorial(usuarioIdActual(), productoId, limite)
            .map { it.toDomain() }
    }

    override suspend fun guardarEvaluacion(evaluacion: EvaluacionComercial) {
        evaluacionDao.insertar(
            EvaluacionComercialEntity.fromDomain(evaluacion, usuarioIdActual())
        )
    }

    private fun usuarioIdActual(): String = almacenSesion.usuarioIdActual() ?: USUARIO_SIN_SESION
}

private const val USUARIO_SIN_SESION = "sin_sesion"

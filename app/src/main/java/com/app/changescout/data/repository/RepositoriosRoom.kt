package com.app.changescout.data.repository

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
    private val productoDao: ProductoImportadoDao
) : RepositorioProductoImportado {
    override fun observarTodos(): Flow<List<ProductoImportado>> {
        return productoDao.observarTodos()
            .map { productos -> productos.map { it.toDomain() } }
    }

    override fun observarPorId(productoId: Long): Flow<ProductoImportado?> {
        return productoDao.observarPorId(productoId)
            .map { producto -> producto?.toDomain() }
    }

    override suspend fun obtenerPorId(productoId: Long): ProductoImportado? {
        return productoDao.obtenerPorId(productoId)?.toDomain()
    }

    override suspend fun upsert(producto: ProductoImportado): Long {
        return productoDao.upsert(ProductoImportadoEntity.fromDomain(producto))
    }
}

class RepositorioEvaluacionComercialRoom(
    private val evaluacionDao: EvaluacionComercialDao
) : RepositorioEvaluacionComercial {
    override fun observarUltimo(productoId: Long): Flow<EvaluacionComercial?> {
        return evaluacionDao.observarUltimo(productoId)
            .map { evaluacion -> evaluacion?.toDomain() }
    }

    override fun observarUltimosDeTodos(): Flow<List<EvaluacionComercial>> {
        return evaluacionDao.observarUltimosDeTodos()
            .map { evaluaciones -> evaluaciones.map { it.toDomain() } }
    }

    override suspend fun obtenerHistorial(
        productoId: Long,
        limite: Int
    ): List<EvaluacionComercial> {
        return evaluacionDao.obtenerHistorial(productoId, limite)
            .map { it.toDomain() }
    }

    override suspend fun guardarEvaluacion(evaluacion: EvaluacionComercial) {
        evaluacionDao.insertar(EvaluacionComercialEntity.fromDomain(evaluacion))
    }
}

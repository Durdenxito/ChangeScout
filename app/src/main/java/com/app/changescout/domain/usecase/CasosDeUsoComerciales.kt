package com.app.changescout.domain.usecase

import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.ProductoRadarItem
import com.app.changescout.domain.model.SnapshotEvaluacionComercial
import com.app.changescout.domain.repository.RepositorioEvaluacionComercial
import com.app.changescout.domain.repository.RepositorioProductoImportado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObservarRadarProductosUseCase @Inject constructor(
    private val repositorioProducto: RepositorioProductoImportado,
    private val repositorioEvaluacion: RepositorioEvaluacionComercial
) {
    operator fun invoke(): Flow<List<ProductoRadarItem>> {
        return combine(
            repositorioProducto.observarTodos(),
            repositorioEvaluacion.observarUltimosDeTodos()
        ) { productos, snapshots ->
            val snapshotsPorProducto = snapshots.associateBy { it.productoId }
            productos.map { producto ->
                ProductoRadarItem(
                    producto = producto,
                    ultimoSnapshot = snapshotsPorProducto[producto.id]
                )
            }
        }
    }
}

class ObservarDetalleProductoUseCase @Inject constructor(
    private val repositorioProducto: RepositorioProductoImportado
) {
    operator fun invoke(productoId: Long): Flow<ProductoImportado?> {
        return repositorioProducto.observarPorId(productoId)
    }
}

class ObservarUltimoSnapshotUseCase @Inject constructor(
    private val repositorioEvaluacion: RepositorioEvaluacionComercial
) {
    operator fun invoke(productoId: Long): Flow<SnapshotEvaluacionComercial?> {
        return repositorioEvaluacion.observarUltimo(productoId)
    }
}

class GuardarProductoImportadoUseCase @Inject constructor(
    private val repositorioProducto: RepositorioProductoImportado
) {
    suspend operator fun invoke(producto: ProductoImportado): Long {
        validar(producto)
        return repositorioProducto.upsert(producto)
    }

    private fun validar(producto: ProductoImportado) {
        require(producto.nombre.isNotBlank()) { "Ingresa un nombre para el producto." }
        require(producto.queryCompetencia.isNotBlank()) {
            "Ingresa un query de competencia para el radar."
        }
        require(producto.cantidadDisponible > 0) { "La cantidad disponible debe ser mayor a cero." }
        require(producto.componentesCosto.tieneValoresValidos()) {
            "Revisa los componentes de costo: FOB debe ser mayor a cero y los demas cargos no pueden ser negativos."
        }
    }
}

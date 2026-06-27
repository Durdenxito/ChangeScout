package com.app.changescout.domain.usecase

import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.ProductoRadarItem
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.model.EvaluacionComercial
import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.repository.ProveedorMarketplace
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
        ) { productos, evaluaciones ->
            val evaluacionesPorProducto = evaluaciones.associateBy { it.productoId }
            productos.map { producto ->
                ProductoRadarItem(
                    producto = producto,
                    ultimaEvaluacion = evaluacionesPorProducto[producto.id]
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

class ObservarUltimaEvaluacionUseCase @Inject constructor(
    private val repositorioEvaluacion: RepositorioEvaluacionComercial
) {
    operator fun invoke(productoId: Long): Flow<EvaluacionComercial?> {
        return repositorioEvaluacion.observarUltimo(productoId)
    }
}

class PrevisualizarCompetenciaUseCase @Inject constructor(
    private val proveedorMarketplace: ProveedorMarketplace
) {
    suspend operator fun invoke(query: String): ResultadoOperacion<List<PublicacionMercado>> {
        val querySeguro = query.trim()
        if (querySeguro.isBlank()) {
            return ResultadoOperacion.Fallo(
                ErrorOperacion.Validacion(
                    "Escribe lo que buscarias en MercadoLibre para comparar este producto."
                )
            )
        }

        return proveedorMarketplace.buscar(
            query = querySeguro,
            limit = LIMITE_PREVIEW_COMPETENCIA
        )
    }

    private companion object {
        const val LIMITE_PREVIEW_COMPETENCIA = 5
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

class EliminarProductoImportadoUseCase @Inject constructor(
    private val repositorioProducto: RepositorioProductoImportado
) {
    suspend operator fun invoke(productoId: Long) {
        require(productoId > 0L) { "Producto no valido para eliminar." }
        repositorioProducto.eliminar(productoId)
    }
}

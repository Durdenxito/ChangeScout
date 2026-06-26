package com.app.changescout.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.changescout.domain.model.EstadoEvaluacion
import com.app.changescout.domain.model.VeredictoComercial
import com.app.changescout.domain.usecase.ObservarRadarProductosUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class EstadoUiRadarProductos(
    val productos: List<TarjetaProductoRadarUiModel> = emptyList(),
    val estaCargando: Boolean = true,
    val mensajeError: String? = null
)

data class TarjetaProductoRadarUiModel(
    val productoId: Long,
    val nombre: String,
    val cantidadDisponible: Int,
    val margenNetoPct: Double?,
    val veredicto: VeredictoComercial?,
    val estadoEvaluacion: EstadoEvaluacion?,
    val evaluadoEn: String?
)

sealed interface EventoRadarProductos {
    data object AgregarProductoSolicitado : EventoRadarProductos
    data class ProductoSeleccionado(val productoId: Long) : EventoRadarProductos
}

sealed interface EfectoRadarProductos {
    data object NavegarAFormularioProducto : EfectoRadarProductos
    data class NavegarADetalleProducto(val productoId: Long) : EfectoRadarProductos
}

@HiltViewModel
class ViewModelRadarProductos @Inject constructor(
    private val observarRadarProductosUseCase: ObservarRadarProductosUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(EstadoUiRadarProductos())
    val uiState: StateFlow<EstadoUiRadarProductos> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<EfectoRadarProductos>()
    val uiEffect: SharedFlow<EfectoRadarProductos> = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            observarRadarProductosUseCase().collect { radar ->
                _uiState.update { estado ->
                    estado.copy(
                        productos = radar.map { item ->
                            TarjetaProductoRadarUiModel(
                                productoId = item.producto.id,
                                nombre = item.producto.nombre,
                                cantidadDisponible = item.producto.cantidadDisponible,
                                margenNetoPct = item.ultimaEvaluacion?.margenNetoPct,
                                veredicto = item.ultimaEvaluacion?.veredicto,
                                estadoEvaluacion = item.ultimaEvaluacion?.estadoEvaluacion,
                                evaluadoEn = item.ultimaEvaluacion?.evaluadoEn?.aTextoRelativo()
                            )
                        },
                        estaCargando = false,
                        mensajeError = null
                    )
                }
            }
        }
    }

    fun onEvent(event: EventoRadarProductos) {
        viewModelScope.launch {
            when (event) {
                EventoRadarProductos.AgregarProductoSolicitado -> {
                    _uiEffect.emit(EfectoRadarProductos.NavegarAFormularioProducto)
                }

                is EventoRadarProductos.ProductoSeleccionado -> {
                    _uiEffect.emit(EfectoRadarProductos.NavegarADetalleProducto(event.productoId))
                }
            }
        }
    }
}

private fun java.time.Instant.aTextoRelativo(): String {
    val segundos = ((System.currentTimeMillis() - toEpochMilli()) / 1000).coerceAtLeast(0)
    return when {
        segundos < 60 -> "Evaluado hace menos de un minuto"
        segundos < 3_600 -> "Evaluado hace ${segundos / 60} min"
        segundos < 86_400 -> "Evaluado hace ${segundos / 3_600} h"
        else -> "Evaluado hace ${segundos / 86_400} dias"
    }
}

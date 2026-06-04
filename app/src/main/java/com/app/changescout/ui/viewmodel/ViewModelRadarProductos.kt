package com.app.changescout.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.changescout.domain.model.EstadoSnapshot
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
    val estadoSnapshot: EstadoSnapshot?,
    val evaluadoEn: String?
)

sealed interface EventoRadarProductos {
    data object AgregarProductoSolicitado : EventoRadarProductos
    data class ProductoSeleccionado(val productoId: Long) : EventoRadarProductos
    data object EvaluacionPendienteConsultada : EventoRadarProductos
}

sealed interface EfectoRadarProductos {
    data object NavegarAFormularioProducto : EfectoRadarProductos
    data class NavegarADetalleProducto(val productoId: Long) : EfectoRadarProductos
    data class MostrarMensajeRadar(val mensaje: String) : EfectoRadarProductos
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
                                margenNetoPct = item.ultimoSnapshot?.margenNetoPct,
                                veredicto = item.ultimoSnapshot?.veredicto,
                                estadoSnapshot = item.ultimoSnapshot?.estadoSnapshot,
                                evaluadoEn = item.ultimoSnapshot?.evaluadoEn?.toString()
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

                EventoRadarProductos.EvaluacionPendienteConsultada -> {
                    _uiEffect.emit(
                        EfectoRadarProductos.MostrarMensajeRadar(
                            "Las lecturas de tendencia apareceran aqui cuando esten disponibles."
                        )
                    )
                }
            }
        }
    }
}

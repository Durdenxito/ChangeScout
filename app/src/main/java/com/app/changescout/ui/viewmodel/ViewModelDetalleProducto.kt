package com.app.changescout.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.model.EvaluacionComercial
import com.app.changescout.domain.model.VeredictoComercial
import com.app.changescout.domain.usecase.EliminarProductoImportadoUseCase
import com.app.changescout.domain.usecase.EvaluarTendenciaProductoUseCase
import com.app.changescout.domain.usecase.ObservarDetalleProductoUseCase
import com.app.changescout.domain.usecase.ObservarUltimaEvaluacionUseCase
import com.app.changescout.ui.navigation.DestinoApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class EstadoUiDetalleProducto(
    val producto: ProductoImportado? = null,
    val evaluacion: EvaluacionComercial? = null,
    val estaCargando: Boolean = true,
    val estaEvaluando: Boolean = false,
    val estaEliminando: Boolean = false,
    val mostrarConfirmarEliminacion: Boolean = false,
    val mensajeError: String? = null
)

sealed interface EventoDetalleProducto {
    data object EvaluarProductoActualSolicitado : EventoDetalleProducto
    data object EditarProductoSolicitado : EventoDetalleProducto
    data object EliminarProductoSolicitado : EventoDetalleProducto
    data object CancelarEliminacionSolicitada : EventoDetalleProducto
    data object ConfirmarEliminacionSolicitada : EventoDetalleProducto
    data object RegresarDesdeDetalleSolicitado : EventoDetalleProducto
}

sealed interface EfectoDetalleProducto {
    data object NavegarAtrasDesdeDetalle : EfectoDetalleProducto
    data class NavegarAEditarProducto(val producto: ProductoImportado) : EfectoDetalleProducto
    data class MostrarMensajeDetalle(val mensaje: String) : EfectoDetalleProducto
}

@HiltViewModel
class ViewModelDetalleProducto @Inject constructor(
    private val observarDetalleProductoUseCase: ObservarDetalleProductoUseCase,
    private val observarUltimaEvaluacionUseCase: ObservarUltimaEvaluacionUseCase,
    private val eliminarProductoImportadoUseCase: EliminarProductoImportadoUseCase,
    private val evaluarTendenciaProductoUseCase: EvaluarTendenciaProductoUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val productoId: Long = savedStateHandle.get<Long>(DestinoApp.ARG_PRODUCTO_ID) ?: 0L

    private val _uiState = MutableStateFlow(EstadoUiDetalleProducto())
    val uiState: StateFlow<EstadoUiDetalleProducto> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<EfectoDetalleProducto>()
    val uiEffect: SharedFlow<EfectoDetalleProducto> = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                observarDetalleProductoUseCase(productoId),
                observarUltimaEvaluacionUseCase(productoId)
            ) { producto, evaluacion ->
                producto to evaluacion
            }.collect { (producto, evaluacion) ->
                _uiState.update { estado ->
                    estado.copy(
                        producto = producto,
                        evaluacion = evaluacion,
                        estaCargando = false,
                        mensajeError = if (producto == null) {
                            "No encontramos este producto en el radar actual."
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    fun onEvent(event: EventoDetalleProducto) {
        viewModelScope.launch {
            when (event) {
                EventoDetalleProducto.EvaluarProductoActualSolicitado -> {
                    evaluarProductoActual()
                }

                EventoDetalleProducto.EditarProductoSolicitado -> {
                    _uiState.value.producto?.let { producto ->
                        _uiEffect.emit(EfectoDetalleProducto.NavegarAEditarProducto(producto))
                    }
                }

                EventoDetalleProducto.EliminarProductoSolicitado -> {
                    _uiState.update { it.copy(mostrarConfirmarEliminacion = true) }
                }

                EventoDetalleProducto.CancelarEliminacionSolicitada -> {
                    _uiState.update { it.copy(mostrarConfirmarEliminacion = false) }
                }

                EventoDetalleProducto.ConfirmarEliminacionSolicitada -> {
                    eliminarProductoActual()
                }

                EventoDetalleProducto.RegresarDesdeDetalleSolicitado -> {
                    _uiEffect.emit(EfectoDetalleProducto.NavegarAtrasDesdeDetalle)
                }
            }
        }
    }

    private suspend fun eliminarProductoActual() {
        if (_uiState.value.estaEliminando) return

        _uiState.update {
            it.copy(
                estaEliminando = true,
                mostrarConfirmarEliminacion = false,
                mensajeError = null
            )
        }

        runCatching {
            eliminarProductoImportadoUseCase(productoId)
        }.onSuccess {
            _uiEffect.emit(EfectoDetalleProducto.NavegarAtrasDesdeDetalle)
        }.onFailure { throwable ->
            val mensaje = throwable.message ?: "No se pudo eliminar el producto."
            _uiState.update {
                it.copy(
                    estaEliminando = false,
                    mensajeError = mensaje
                )
            }
            _uiEffect.emit(EfectoDetalleProducto.MostrarMensajeDetalle(mensaje))
        }
    }

    private suspend fun evaluarProductoActual() {
        if (_uiState.value.estaEvaluando) return

        _uiState.update { estado ->
            estado.copy(estaEvaluando = true, mensajeError = null)
        }

        when (val resultado = evaluarTendenciaProductoUseCase(productoId)) {
            is ResultadoOperacion.Exito -> {
                _uiEffect.emit(
                    EfectoDetalleProducto.MostrarMensajeDetalle(
                        "Lectura actualizada: ${resultado.data.veredicto.aTextoPresentable()}."
                    )
                )
            }
            is ResultadoOperacion.DatosObsoletos -> {
                _uiEffect.emit(
                    EfectoDetalleProducto.MostrarMensajeDetalle(
                        "Lectura generada con datos obsoletos: ${resultado.causa.mensaje}"
                    )
                )
            }
            is ResultadoOperacion.Fallo -> {
                _uiState.update { estado ->
                    estado.copy(mensajeError = resultado.error.mensaje)
                }
                _uiEffect.emit(
                    EfectoDetalleProducto.MostrarMensajeDetalle(resultado.error.mensaje)
                )
            }
        }

        _uiState.update { estado ->
            estado.copy(estaEvaluando = false)
        }
    }

    private fun VeredictoComercial?.aTextoPresentable(): String {
        return when (this) {
            VeredictoComercial.SALUDABLE -> "Saludable"
            VeredictoComercial.PRECAUCION -> "Precaucion"
            VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE -> "Margen en riesgo"
            VeredictoComercial.LIQUIDACION -> "Conviene liquidar stock"
            VeredictoComercial.INCONCLUSO -> "Sin datos suficientes"
            null -> "Sin clasificar"
        }
    }
}

package com.app.changescout.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.changescout.domain.model.ComponentesCostoImportacion
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.usecase.GuardarProductoImportadoUseCase
import com.app.changescout.domain.usecase.PrevisualizarCompetenciaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private object FormularioProductoKeys {
    const val NOMBRE = "form_producto_nombre"
    const val PRECIO_FOB = "form_producto_precio_fob"
    const val FLETE = "form_producto_flete"
    const val SEGURO = "form_producto_seguro"
    const val ARANCELES = "form_producto_aranceles"
    const val OTROS_CARGOS = "form_producto_otros_cargos"
    const val CANTIDAD = "form_producto_cantidad"
    const val QUERY = "form_producto_query"
}

data class EstadoUiFormularioProducto(
    val nombre: String = "",
    val precioFobUsd: String = "",
    val fleteUsd: String = "",
    val seguroUsd: String = "",
    val arancelesUsd: String = "",
    val otrosCargosUsd: String = "",
    val cantidadDisponible: String = "",
    val queryCompetencia: String = "",
    val estaGuardando: Boolean = false,
    val estaPrevisualizando: Boolean = false,
    val previewCompetencia: List<PublicacionPreviewUi> = emptyList(),
    val mensajePreview: String? = null,
    val mensajeValidacion: String? = null
) {
    val puedeEnviar: Boolean
        get() = nombre.isNotBlank() &&
            precioFobUsd.isNotBlank() &&
            cantidadDisponible.isNotBlank() &&
            queryCompetencia.isNotBlank() &&
            !estaGuardando

    val puedePrevisualizar: Boolean
        get() = !estaPrevisualizando && (queryCompetencia.isNotBlank() || nombre.isNotBlank())
}

data class PublicacionPreviewUi(
    val titulo: String,
    val precio: String,
    val fuente: String
)

sealed interface EventoFormularioProducto {
    data class NombreCambiado(val value: String) : EventoFormularioProducto
    data class PrecioFobUsdCambiado(val value: String) : EventoFormularioProducto
    data class FleteUsdCambiado(val value: String) : EventoFormularioProducto
    data class SeguroUsdCambiado(val value: String) : EventoFormularioProducto
    data class ArancelesUsdCambiado(val value: String) : EventoFormularioProducto
    data class OtrosCargosUsdCambiado(val value: String) : EventoFormularioProducto
    data class CantidadDisponibleCambiada(val value: String) : EventoFormularioProducto
    data class QueryCompetenciaCambiado(val value: String) : EventoFormularioProducto
    data object PrevisualizarCompetenciaSolicitada : EventoFormularioProducto
    data object GuardarProductoSolicitado : EventoFormularioProducto
    data object RegresarDesdeFormularioSolicitado : EventoFormularioProducto
}

sealed interface EfectoFormularioProducto {
    data object NavegarAtrasDesdeFormulario : EfectoFormularioProducto
    data class MostrarMensajeFormulario(val mensaje: String) : EfectoFormularioProducto
}

@HiltViewModel
class ViewModelFormularioProducto @Inject constructor(
    private val guardarProductoImportadoUseCase: GuardarProductoImportadoUseCase,
    private val previsualizarCompetenciaUseCase: PrevisualizarCompetenciaUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        EstadoUiFormularioProducto(
            nombre = savedStateHandle[FormularioProductoKeys.NOMBRE] ?: "",
            precioFobUsd = savedStateHandle[FormularioProductoKeys.PRECIO_FOB] ?: "",
            fleteUsd = savedStateHandle[FormularioProductoKeys.FLETE] ?: "",
            seguroUsd = savedStateHandle[FormularioProductoKeys.SEGURO] ?: "",
            arancelesUsd = savedStateHandle[FormularioProductoKeys.ARANCELES] ?: "",
            otrosCargosUsd = savedStateHandle[FormularioProductoKeys.OTROS_CARGOS] ?: "",
            cantidadDisponible = savedStateHandle[FormularioProductoKeys.CANTIDAD] ?: "",
            queryCompetencia = savedStateHandle[FormularioProductoKeys.QUERY] ?: ""
        )
    )
    val uiState: StateFlow<EstadoUiFormularioProducto> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<EfectoFormularioProducto>()
    val uiEffect: SharedFlow<EfectoFormularioProducto> = _uiEffect.asSharedFlow()

    fun onEvent(event: EventoFormularioProducto) {
        when (event) {
            is EventoFormularioProducto.NombreCambiado -> actualizarBorrador(nombre = event.value)
            is EventoFormularioProducto.PrecioFobUsdCambiado -> actualizarBorrador(precioFob = event.value)
            is EventoFormularioProducto.FleteUsdCambiado -> actualizarBorrador(flete = event.value)
            is EventoFormularioProducto.SeguroUsdCambiado -> actualizarBorrador(seguro = event.value)
            is EventoFormularioProducto.ArancelesUsdCambiado -> actualizarBorrador(aranceles = event.value)
            is EventoFormularioProducto.OtrosCargosUsdCambiado -> actualizarBorrador(otrosCargos = event.value)
            is EventoFormularioProducto.CantidadDisponibleCambiada -> actualizarBorrador(cantidad = event.value)
            is EventoFormularioProducto.QueryCompetenciaCambiado -> actualizarBorrador(query = event.value)
            EventoFormularioProducto.PrevisualizarCompetenciaSolicitada -> previsualizarCompetencia()
            EventoFormularioProducto.GuardarProductoSolicitado -> guardarProducto()
            EventoFormularioProducto.RegresarDesdeFormularioSolicitado -> {
                viewModelScope.launch {
                    _uiEffect.emit(EfectoFormularioProducto.NavegarAtrasDesdeFormulario)
                }
            }
        }
    }

    private fun actualizarBorrador(
        nombre: String? = null,
        precioFob: String? = null,
        flete: String? = null,
        seguro: String? = null,
        aranceles: String? = null,
        otrosCargos: String? = null,
        cantidad: String? = null,
        query: String? = null
    ) {
        _uiState.update { estado ->
            val nuevoNombre = nombre ?: estado.nombre
            val nuevoQuery = when {
                query != null -> query
                nombre != null && (estado.queryCompetencia.isBlank() || estado.queryCompetencia == estado.nombre) -> nombre
                else -> estado.queryCompetencia
            }
            estado.copy(
                nombre = nuevoNombre,
                precioFobUsd = precioFob ?: estado.precioFobUsd,
                fleteUsd = flete ?: estado.fleteUsd,
                seguroUsd = seguro ?: estado.seguroUsd,
                arancelesUsd = aranceles ?: estado.arancelesUsd,
                otrosCargosUsd = otrosCargos ?: estado.otrosCargosUsd,
                cantidadDisponible = cantidad ?: estado.cantidadDisponible,
                queryCompetencia = nuevoQuery,
                mensajePreview = null,
                mensajeValidacion = null
            )
        }
        savedStateHandle[FormularioProductoKeys.NOMBRE] = _uiState.value.nombre
        savedStateHandle[FormularioProductoKeys.PRECIO_FOB] = _uiState.value.precioFobUsd
        savedStateHandle[FormularioProductoKeys.FLETE] = _uiState.value.fleteUsd
        savedStateHandle[FormularioProductoKeys.SEGURO] = _uiState.value.seguroUsd
        savedStateHandle[FormularioProductoKeys.ARANCELES] = _uiState.value.arancelesUsd
        savedStateHandle[FormularioProductoKeys.OTROS_CARGOS] = _uiState.value.otrosCargosUsd
        savedStateHandle[FormularioProductoKeys.CANTIDAD] = _uiState.value.cantidadDisponible
        savedStateHandle[FormularioProductoKeys.QUERY] = _uiState.value.queryCompetencia
    }

    private fun previsualizarCompetencia() {
        viewModelScope.launch {
            val query = _uiState.value.queryCompetencia.ifBlank { _uiState.value.nombre }.trim()
            _uiState.update {
                it.copy(
                    estaPrevisualizando = true,
                    previewCompetencia = emptyList(),
                    mensajePreview = null
                )
            }

            when (val resultado = previsualizarCompetenciaUseCase(query)) {
                is ResultadoOperacion.Exito -> {
                    _uiState.update {
                        it.copy(
                            estaPrevisualizando = false,
                            previewCompetencia = resultado.data.map { publicacion -> publicacion.toPreviewUi() },
                            mensajePreview = if (resultado.data.isEmpty()) {
                                "No se encontraron publicaciones para esa busqueda."
                            } else {
                                null
                            }
                        )
                    }
                }
                is ResultadoOperacion.DatosObsoletos -> {
                    _uiState.update {
                        it.copy(
                            estaPrevisualizando = false,
                            previewCompetencia = resultado.data.map { publicacion -> publicacion.toPreviewUi() },
                            mensajePreview = "Se muestran resultados guardados porque el mercado en vivo no respondio."
                        )
                    }
                }
                is ResultadoOperacion.Fallo -> {
                    _uiState.update {
                        it.copy(
                            estaPrevisualizando = false,
                            previewCompetencia = emptyList(),
                            mensajePreview = resultado.error.mensaje
                        )
                    }
                }
            }
        }
    }

    private fun guardarProducto() {
        viewModelScope.launch {
            _uiState.update { it.copy(estaGuardando = true, mensajeValidacion = null) }
            runCatching {
                guardarProductoImportadoUseCase(construirProductoImportado())
            }.onSuccess {
                limpiarBorrador()
                _uiEffect.emit(EfectoFormularioProducto.NavegarAtrasDesdeFormulario)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        estaGuardando = false,
                        mensajeValidacion = throwable.message ?: "No se pudo guardar la ficha."
                    )
                }
                _uiEffect.emit(
                    EfectoFormularioProducto.MostrarMensajeFormulario(
                        _uiState.value.mensajeValidacion ?: "No se pudo guardar la ficha."
                    )
                )
            }
        }
    }

    private fun construirProductoImportado(): ProductoImportado {
        val precioFob = _uiState.value.precioFobUsd.parsearMontoUsd("precio FOB")
        val flete = _uiState.value.fleteUsd.parsearMontoUsdOpcional()
        val seguro = _uiState.value.seguroUsd.parsearMontoUsdOpcional()
        val aranceles = _uiState.value.arancelesUsd.parsearMontoUsdOpcional()
        val otrosCargos = _uiState.value.otrosCargosUsd.parsearMontoUsdOpcional()
        val cantidad = _uiState.value.cantidadDisponible.trim().toIntOrNull()
            ?: error("Ingresa una cantidad disponible valida.")

        return ProductoImportado(
            id = 0L,
            nombre = _uiState.value.nombre.trim(),
            queryCompetencia = _uiState.value.queryCompetencia.trim(),
            componentesCosto = ComponentesCostoImportacion(
                precioFobUsd = precioFob,
                fleteUsd = flete,
                seguroUsd = seguro,
                arancelesUsd = aranceles,
                otrosCargosUsd = otrosCargos
            ),
            cantidadDisponible = cantidad
        )
    }

    private fun limpiarBorrador() {
        savedStateHandle[FormularioProductoKeys.NOMBRE] = ""
        savedStateHandle[FormularioProductoKeys.PRECIO_FOB] = ""
        savedStateHandle[FormularioProductoKeys.FLETE] = ""
        savedStateHandle[FormularioProductoKeys.SEGURO] = ""
        savedStateHandle[FormularioProductoKeys.ARANCELES] = ""
        savedStateHandle[FormularioProductoKeys.OTROS_CARGOS] = ""
        savedStateHandle[FormularioProductoKeys.CANTIDAD] = ""
        savedStateHandle[FormularioProductoKeys.QUERY] = ""
        _uiState.value = EstadoUiFormularioProducto()
    }

    private fun String.parsearMontoUsd(nombreCampo: String): Double {
        return trim()
            .replace(",", ".")
            .toDoubleOrNull()
            ?: error("Ingresa un monto USD valido para $nombreCampo.")
    }

    private fun String.parsearMontoUsdOpcional(): Double {
        return takeIf { valor -> valor.isNotBlank() }
            ?.parsearMontoUsd("el costo opcional")
            ?: 0.0
    }

    private fun PublicacionMercado.toPreviewUi(): PublicacionPreviewUi {
        val simboloMoneda = when (moneda.name) {
            "PEN" -> "S/"
            "USD" -> "USD"
            else -> moneda.name
        }

        return PublicacionPreviewUi(
            titulo = titulo,
            precio = "$simboloMoneda ${"%.2f".format(precio)}",
            fuente = nombreFuente
        )
    }
}

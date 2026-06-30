package com.app.changescout.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.changescout.data.auth.RepositorioSesionSupabase
import com.app.changescout.domain.model.EstadoEvaluacion
import com.app.changescout.domain.model.ProductoRadarItem
import com.app.changescout.domain.model.VeredictoComercial
import com.app.changescout.domain.usecase.ObservarRadarProductosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class EstadoUiCuenta(
    val email: String = "",
    val nombreUsuario: String = "",
    val totalProductos: Int = 0,
    val saludables: Int = 0,
    val precaucion: Int = 0,
    val margenEnRiesgo: Int = 0,
    val liquidarStock: Int = 0,
    val sinDatos: Int = 0,
    val desactualizados: Int = 0,
    val ultimaLectura: String = "Sin lecturas todavia",
    val estaCargando: Boolean = true,
    val mensajeError: String? = null
)

@HiltViewModel
class ViewModelCuenta @Inject constructor(
    private val observarRadarProductosUseCase: ObservarRadarProductosUseCase,
    repositorioSesion: RepositorioSesionSupabase
) : ViewModel() {
    private val sesion = repositorioSesion.sesionActual()
    private val email = sesion?.email.orEmpty()
    private val nombreUsuario = sesion?.nombreUsuario.orEmpty()
    private val _uiState = MutableStateFlow(
        EstadoUiCuenta(email = email, nombreUsuario = nombreUsuario)
    )
    val uiState: StateFlow<EstadoUiCuenta> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observarRadarProductosUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            estaCargando = false,
                            mensajeError = error.message ?: "No se pudo cargar el resumen de cuenta."
                        )
                    }
                }
                .collect { radar ->
                    _uiState.update {
                        crearResumenCuenta(
                            email = email,
                            nombreUsuario = nombreUsuario,
                            radar = radar,
                            ahora = Instant.now()
                        )
                    }
                }
        }
    }
}

fun crearResumenCuenta(
    email: String,
    nombreUsuario: String,
    radar: List<ProductoRadarItem>,
    ahora: Instant
): EstadoUiCuenta {
    val ultima = radar
        .mapNotNull { item -> item.ultimaEvaluacion?.let { evaluacion -> item.producto.nombre to evaluacion } }
        .maxByOrNull { (_, evaluacion) -> evaluacion.evaluadoEn }

    return EstadoUiCuenta(
        email = email,
        nombreUsuario = nombreUsuario,
        totalProductos = radar.size,
        saludables = radar.count { it.ultimaEvaluacion?.veredicto == VeredictoComercial.SALUDABLE },
        precaucion = radar.count { it.ultimaEvaluacion?.veredicto == VeredictoComercial.PRECAUCION },
        margenEnRiesgo = radar.count {
            it.ultimaEvaluacion?.veredicto == VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE
        },
        liquidarStock = radar.count { it.ultimaEvaluacion?.veredicto == VeredictoComercial.LIQUIDACION },
        sinDatos = radar.count { item ->
            val evaluacion = item.ultimaEvaluacion
            evaluacion == null ||
                evaluacion.veredicto == null ||
                evaluacion.veredicto == VeredictoComercial.INCONCLUSO ||
                evaluacion.estadoEvaluacion == EstadoEvaluacion.INCONCLUSO ||
                evaluacion.estadoEvaluacion == EstadoEvaluacion.FALLIDO
        },
        desactualizados = radar.count { it.ultimaEvaluacion?.estadoEvaluacion == EstadoEvaluacion.OBSOLETO },
        ultimaLectura = ultima?.let { (producto, evaluacion) ->
            "$producto, ${evaluacion.evaluadoEn.aTextoRelativo(ahora)}"
        } ?: "Sin lecturas todavia",
        estaCargando = false
    )
}

private fun Instant.aTextoRelativo(ahora: Instant): String {
    val segundos = ((ahora.toEpochMilli() - toEpochMilli()) / 1000).coerceAtLeast(0)
    return when {
        segundos < 60 -> "hace menos de un minuto"
        segundos < 3_600 -> "hace ${segundos / 60} min"
        segundos < 86_400 -> "hace ${segundos / 3_600} h"
        else -> "hace ${segundos / 86_400} dias"
    }
}

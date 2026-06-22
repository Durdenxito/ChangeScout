package com.app.changescout.ui.screens.product.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.changescout.domain.model.EstadoEvaluacion
import com.app.changescout.domain.model.EvaluacionComercial
import com.app.changescout.domain.model.VeredictoComercial
import com.app.changescout.ui.screens.components.ChipOperativo
import com.app.changescout.ui.screens.components.EncabezadoSeccion
import com.app.changescout.ui.screens.components.FondoOperativoPremium
import com.app.changescout.ui.screens.components.MetricaResumida
import com.app.changescout.ui.screens.components.TarjetaPremium
import com.app.changescout.ui.theme.ErrorSoft
import com.app.changescout.ui.theme.SignalGold
import com.app.changescout.ui.theme.SuccessContainer
import com.app.changescout.ui.viewmodel.EfectoDetalleProducto
import com.app.changescout.ui.viewmodel.EstadoUiDetalleProducto
import com.app.changescout.ui.viewmodel.EventoDetalleProducto
import com.app.changescout.ui.viewmodel.ViewModelDetalleProducto

@Composable
fun PantallaDetalleProducto(
    onNavegarAtras: () -> Unit,
    detalleViewModel: ViewModelDetalleProducto = hiltViewModel()
) {
    val state by detalleViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(detalleViewModel) {
        detalleViewModel.uiEffect.collect { effect ->
            when (effect) {
                EfectoDetalleProducto.NavegarAtrasDesdeDetalle -> onNavegarAtras()
                is EfectoDetalleProducto.MostrarMensajeDetalle -> {
                    snackbarHostState.showSnackbar(effect.mensaje)
                }
            }
        }
    }

    FondoOperativoPremium {
        ContenidoDetalleProducto(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = detalleViewModel::onEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoDetalleProducto(
    state: EstadoUiDetalleProducto,
    snackbarHostState: SnackbarHostState,
    onEvent: (EventoDetalleProducto) -> Unit
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                title = {
                    Column {
                        Text("Ficha de seguimiento")
                        Text(
                            text = "Detalle operativo del producto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = {
                        onEvent(EventoDetalleProducto.RegresarDesdeDetalleSolicitado)
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.estaCargando) {
                TarjetaPremium {
                    EncabezadoSeccion(
                        icono = Icons.Outlined.Schedule,
                        titulo = "Cargando ficha",
                        subtitulo = "Un momento mientras organizamos la informacion del producto."
                    )
                }
                return@Column
            }

            state.producto?.let { producto ->
                TarjetaPremium {
                    EncabezadoSeccion(
                        icono = Icons.Outlined.Inventory2,
                        titulo = producto.nombre,
                        subtitulo = "Producto cargado para seguimiento"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricaResumida(
                            titulo = "Costo base",
                            valor = "USD ${"%.2f".format(producto.componentesCosto.precioFobUsd)}",
                            icono = Icons.Outlined.AttachMoney,
                            modifier = Modifier.weight(1f)
                        )
                        MetricaResumida(
                            titulo = "Stock",
                            valor = producto.cantidadDisponible.toString(),
                            icono = Icons.Outlined.Inventory2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ChipOperativo(
                        texto = producto.queryCompetencia,
                        icono = Icons.Outlined.Search,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            TarjetaPremium {
                EncabezadoSeccion(
                    icono = Icons.Outlined.QueryStats,
                    titulo = "Lectura de referencia",
                    subtitulo = "Resumen operativo del comportamiento comercial del producto."
                )

                if (state.evaluacion == null) {
                    ChipOperativo(
                        texto = "Sin lectura cargada",
                        icono = Icons.Outlined.Schedule,
                        contenedor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "Cuando exista una lectura disponible, aqui veras margen estimado, nivel de riesgo y estado actual del seguimiento.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.evaluacion.evaluacionResumen()
                }

                if (state.estaEvaluando) {
                    ChipOperativo(
                        texto = "Consultando mercado en vivo",
                        icono = Icons.Outlined.Search,
                        modifier = Modifier.fillMaxWidth(),
                        contenedor = SignalGold.copy(alpha = 0.18f)
                    )
                }

                Button(
                    onClick = {
                        onEvent(EventoDetalleProducto.EvaluarProductoActualSolicitado)
                    },
                    enabled = !state.estaEvaluando && state.producto != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (state.estaEvaluando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Outlined.QueryStats, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (state.estaEvaluando) "Consultando..." else "Solicitar lectura")
                }
            }

            state.mensajeError?.let { mensaje ->
                TarjetaPremium {
                    ChipOperativo(
                        texto = "Revision requerida",
                        icono = Icons.Outlined.WarningAmber,
                        contenedor = ErrorSoft
                    )
                    Text(
                        text = mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EvaluacionComercial.evaluacionResumen() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricaResumida(
            titulo = "Precio ref.",
            valor = precioPromedioRealPen?.let { "S/ ${"%.2f".format(it)}" } ?: "--",
            icono = Icons.Outlined.AttachMoney,
            modifier = Modifier.weight(1f)
        )
        MetricaResumida(
            titulo = "Costo destino",
            valor = costoTotalPen?.let { "S/ ${"%.2f".format(it)}" } ?: "--",
            icono = Icons.Outlined.Inventory2,
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricaResumida(
            titulo = "Margen",
            valor = margenNetoPct?.let { "${"%.1f".format(it)}%" } ?: "--",
            icono = Icons.Outlined.Sell,
            modifier = Modifier.weight(1f)
        )
        MetricaResumida(
            titulo = "Validas",
            valor = competidoresValidos.toString(),
            icono = Icons.Outlined.Shield,
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipOperativo(
            texto = veredicto?.etiqueta() ?: "Sin clasificar",
            icono = Icons.Outlined.Sell,
            contenedor = veredicto.colorContenedor()
        )
        ChipOperativo(
            texto = estadoEvaluacion.aTextoPresentable(),
            icono = Icons.Outlined.Schedule,
            contenedor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    tipoCambioVentaUsdPen?.let { tasa ->
        ChipOperativo(
            texto = "TC venta ${"%.3f".format(tasa)}",
            icono = Icons.Outlined.AttachMoney,
            contenedor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Text(
        text = "Evaluado: $evaluadoEn",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun VeredictoComercial?.etiqueta(): String {
    return when (this) {
        VeredictoComercial.SALUDABLE -> "Saludable"
        VeredictoComercial.PRECAUCION -> "Precaucion"
        VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE -> "Alerta"
        VeredictoComercial.LIQUIDACION -> "Liquidacion"
        VeredictoComercial.INCONCLUSO -> "Inconcluso"
        null -> "Sin lectura"
    }
}

@Composable
private fun VeredictoComercial?.colorContenedor() = when (this) {
    VeredictoComercial.SALUDABLE -> SuccessContainer
    VeredictoComercial.PRECAUCION -> SignalGold.copy(alpha = 0.18f)
    VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE -> ErrorSoft
    VeredictoComercial.LIQUIDACION -> ErrorSoft
    VeredictoComercial.INCONCLUSO -> MaterialTheme.colorScheme.surfaceVariant
    null -> MaterialTheme.colorScheme.surfaceVariant
}

private fun EstadoEvaluacion.aTextoPresentable(): String {
    return when (this) {
        EstadoEvaluacion.VIGENTE -> "Vigente"
        EstadoEvaluacion.OBSOLETO -> "Obsoleto"
        EstadoEvaluacion.INCONCLUSO -> "Inconcluso"
        EstadoEvaluacion.FALLIDO -> "Pendiente"
    }
}

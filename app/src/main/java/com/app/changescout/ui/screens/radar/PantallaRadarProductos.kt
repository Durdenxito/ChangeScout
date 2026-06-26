package com.app.changescout.ui.screens.radar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.changescout.domain.model.EstadoEvaluacion
import com.app.changescout.domain.model.VeredictoComercial
import com.app.changescout.ui.screens.components.BotonSecundario
import com.app.changescout.ui.screens.components.ChipOperativo
import com.app.changescout.ui.screens.components.EncabezadoSeccion
import com.app.changescout.ui.screens.components.FondoOperativo
import com.app.changescout.ui.screens.components.MetricaResumida
import com.app.changescout.ui.screens.components.TarjetaOperativa
import com.app.changescout.ui.theme.ErrorSoft
import com.app.changescout.ui.theme.SignalGold
import com.app.changescout.ui.theme.SignalMint
import com.app.changescout.ui.theme.SuccessContainer
import com.app.changescout.ui.viewmodel.EfectoRadarProductos
import com.app.changescout.ui.viewmodel.EstadoUiRadarProductos
import com.app.changescout.ui.viewmodel.EventoRadarProductos
import com.app.changescout.ui.viewmodel.TarjetaProductoRadarUiModel
import com.app.changescout.ui.viewmodel.ViewModelRadarProductos

@Composable
fun PantallaRadarProductos(
    onNavegarAFormulario: () -> Unit,
    onNavegarADetalle: (Long) -> Unit,
    radarViewModel: ViewModelRadarProductos = hiltViewModel()
) {
    val state by radarViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(radarViewModel) {
        radarViewModel.uiEffect.collect { effect ->
            when (effect) {
                EfectoRadarProductos.NavegarAFormularioProducto -> onNavegarAFormulario()
                is EfectoRadarProductos.NavegarADetalleProducto -> onNavegarADetalle(effect.productoId)
                is EfectoRadarProductos.MostrarMensajeRadar -> {
                    snackbarHostState.showSnackbar(effect.mensaje)
                }
            }
        }
    }

    FondoOperativo {
        ContenidoRadarProductos(
            state = state,
            snackbarHostState = snackbarHostState,
            onNuevoProducto = {
                radarViewModel.onEvent(EventoRadarProductos.AgregarProductoSolicitado)
            },
            onSeleccionarProducto = { productoId ->
                radarViewModel.onEvent(EventoRadarProductos.ProductoSeleccionado(productoId))
            },
            onConsultarContexto = {
                radarViewModel.onEvent(EventoRadarProductos.EvaluacionPendienteConsultada)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoRadarProductos(
    state: EstadoUiRadarProductos,
    snackbarHostState: SnackbarHostState,
    onNuevoProducto: () -> Unit,
    onSeleccionarProducto: (Long) -> Unit,
    onConsultarContexto: () -> Unit
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
                        Text(
                            text = "ChangeScout",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Costo, margen y mercado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoProducto,
                shape = RoundedCornerShape(14.dp),
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nuevo producto") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ResumenRadar(
                    cantidadProductos = state.productos.size,
                    cantidadPendiente = state.productos.count { it.estadoEvaluacion == null },
                    onConsultarContexto = onConsultarContexto
                )
            }

            if (state.estaCargando) {
                item {
                    TarjetaOperativa {
                        EncabezadoSeccion(
                            icono = Icons.Outlined.Schedule,
                            titulo = "Cargando productos",
                            subtitulo = "Leyendo las fichas guardadas en el dispositivo."
                        )
                    }
                }
            } else if (state.productos.isEmpty()) {
                item {
                    TarjetaOperativa {
                        EncabezadoSeccion(
                            icono = Icons.Outlined.Inventory2,
                            titulo = "Sin productos",
                            subtitulo = "Agrega una ficha para calcular costo, margen y mercado comparable."
                        )
                    }
                }
            } else {
                items(state.productos, key = { it.productoId }) { producto ->
                    TarjetaProductoRadar(
                        producto = producto,
                        onClick = { onSeleccionarProducto(producto.productoId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumenRadar(
    cantidadProductos: Int,
    cantidadPendiente: Int,
    onConsultarContexto: () -> Unit
) {
    TarjetaOperativa(
        acento = MaterialTheme.colorScheme.primary
    ) {
        EncabezadoSeccion(
            icono = Icons.Outlined.QueryStats,
            titulo = "Antes de reponer stock",
            subtitulo = "Revisa que productos siguen dando margen y cuales piden atencion."
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricaResumida(
                titulo = "Productos",
                valor = cantidadProductos.toString(),
                icono = Icons.Outlined.Warehouse,
                modifier = Modifier.weight(1f)
            )
            MetricaResumida(
                titulo = "Pendientes",
                valor = cantidadPendiente.toString(),
                icono = Icons.Outlined.Schedule,
                modifier = Modifier.weight(1f)
            )
        }
        BotonSecundario(
            texto = "Ver criterio de lectura",
            icono = Icons.Outlined.Info,
            onClick = onConsultarContexto,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TarjetaProductoRadar(
    producto: TarjetaProductoRadarUiModel,
    onClick: () -> Unit
) {
    TarjetaOperativa(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        acento = producto.veredicto.colorAcento()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stock local y ultima lectura comercial.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            producto.veredicto?.let { veredicto ->
                ChipOperativo(
                    texto = veredicto.etiqueta(),
                    icono = Icons.Outlined.Sell,
                    contenedor = veredicto.colorContenedor()
                )
            } ?: ChipOperativo(
                texto = "Pendiente",
                icono = Icons.Outlined.Schedule,
                contenedor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricaResumida(
                titulo = "Stock",
                valor = producto.cantidadDisponible.toString(),
                icono = Icons.Outlined.Warehouse,
                modifier = Modifier.weight(1f)
            )
            MetricaResumida(
                titulo = "Margen",
                valor = producto.margenNetoPct?.let { "${"%.1f".format(it)}%" } ?: "--",
                icono = Icons.Outlined.QueryStats,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChipOperativo(
                texto = producto.estadoEvaluacion?.aTextoPresentable() ?: "Pendiente",
                icono = Icons.Outlined.Schedule,
                contenedor = MaterialTheme.colorScheme.surfaceVariant
            )
            producto.evaluadoEn?.let {
                ChipOperativo(
                    texto = it,
                    icono = Icons.Outlined.QueryStats,
                    contenedor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                )
            }
        }
    }
}

private fun EstadoEvaluacion.aTextoPresentable(): String {
    return when (this) {
        EstadoEvaluacion.VIGENTE -> "Evaluado recientemente"
        EstadoEvaluacion.OBSOLETO -> "Informacion desactualizada"
        EstadoEvaluacion.INCONCLUSO -> "Sin datos suficientes"
        EstadoEvaluacion.FALLIDO -> "Lectura pendiente"
    }
}

private fun VeredictoComercial.etiqueta(): String {
    return when (this) {
        VeredictoComercial.SALUDABLE -> "Saludable"
        VeredictoComercial.PRECAUCION -> "Precaucion"
        VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE -> "Margen en riesgo"
        VeredictoComercial.LIQUIDACION -> "Liquidar stock"
        VeredictoComercial.INCONCLUSO -> "Sin datos"
    }
}

@Composable
private fun VeredictoComercial.colorContenedor() = when (this) {
    VeredictoComercial.SALUDABLE -> SuccessContainer
    VeredictoComercial.PRECAUCION -> SignalGold.copy(alpha = 0.18f)
    VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE -> ErrorSoft
    VeredictoComercial.LIQUIDACION -> ErrorSoft
    VeredictoComercial.INCONCLUSO -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun VeredictoComercial?.colorAcento() = when (this) {
    VeredictoComercial.SALUDABLE -> SignalMint
    VeredictoComercial.PRECAUCION -> SignalGold
    VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE -> MaterialTheme.colorScheme.error
    VeredictoComercial.LIQUIDACION -> MaterialTheme.colorScheme.error
    VeredictoComercial.INCONCLUSO -> MaterialTheme.colorScheme.primary
    null -> MaterialTheme.colorScheme.primary
}

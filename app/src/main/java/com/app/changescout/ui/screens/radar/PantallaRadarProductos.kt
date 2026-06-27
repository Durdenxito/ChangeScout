package com.app.changescout.ui.screens.radar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.changescout.domain.model.VeredictoComercial
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
    onNavegarACuenta: () -> Unit,
    onNavegarAFormulario: () -> Unit,
    onNavegarADetalle: (Long) -> Unit,
    radarViewModel: ViewModelRadarProductos = hiltViewModel()
) {
    val state by radarViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(radarViewModel) {
        radarViewModel.uiEffect.collect { effect ->
            when (effect) {
                EfectoRadarProductos.NavegarAFormularioProducto -> onNavegarAFormulario()
                is EfectoRadarProductos.NavegarADetalleProducto -> onNavegarADetalle(effect.productoId)
            }
        }
    }

    val onNuevoProductoEstable = remember<() -> Unit> {
        { radarViewModel.onEvent(EventoRadarProductos.AgregarProductoSolicitado) }
    }
    val onSeleccionarProductoEstable = remember<(Long) -> Unit> {
        { productoId -> radarViewModel.onEvent(EventoRadarProductos.ProductoSeleccionado(productoId)) }
    }

    FondoOperativo {
        ContenidoRadarProductos(
            state = state,
            onNavegarACuenta = onNavegarACuenta,
            onNuevoProducto = onNuevoProductoEstable,
            onSeleccionarProducto = onSeleccionarProductoEstable
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoRadarProductos(
    state: EstadoUiRadarProductos,
    onNavegarACuenta: () -> Unit,
    onNuevoProducto: () -> Unit,
    onSeleccionarProducto: (Long) -> Unit
) {
    var filtro by rememberSaveable {
        mutableStateOf(FiltroRadar.TODOS.name)
    }
    val filtroActual = remember(filtro) {
        FiltroRadar.valueOf(filtro)
    }
    val productosFiltrados = remember(state.productos, filtroActual) {
        state.productos.filter { producto -> filtroActual.acepta(producto) }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                title = {
                    Text(
                        text = "ChangeScout",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavegarACuenta) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = "Cuenta")
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
                    cantidadPendiente = state.productos.count { it.estadoEvaluacion == null }
                )
            }

            if (!state.estaCargando && state.productos.isNotEmpty()) {
                item {
                    FiltrosRadar(
                        filtroActual = filtroActual,
                        onFiltroSeleccionado = { filtro = it.name }
                    )
                }
            }

            if (state.estaCargando) {
                item {
                    TarjetaOperativa {
                        EncabezadoSeccion(
                            icono = Icons.Outlined.Schedule,
                            titulo = "Cargando productos"
                        )
                    }
                }
            } else if (state.productos.isEmpty()) {
                item {
                    TarjetaOperativa {
                        EncabezadoSeccion(
                            icono = Icons.Outlined.Inventory2,
                            titulo = "Sin productos"
                        )
                    }
                }
            } else if (productosFiltrados.isEmpty()) {
                item {
                    TarjetaOperativa {
                        EncabezadoSeccion(
                            icono = Icons.Outlined.Inventory2,
                            titulo = "Sin productos en este filtro"
                        )
                    }
                }
            } else {
                items(productosFiltrados, key = { it.productoId }) { producto ->
                    val onClick = remember(producto.productoId) {
                        { onSeleccionarProducto(producto.productoId) }
                    }
                    TarjetaProductoRadar(
                        producto = producto,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltrosRadar(
    filtroActual: FiltroRadar,
    onFiltroSeleccionado: (FiltroRadar) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroRadar.entries.forEach { filtro ->
            FilterChip(
                selected = filtro == filtroActual,
                onClick = { onFiltroSeleccionado(filtro) },
                label = { Text(filtro.etiqueta) }
            )
        }
    }
}

@Composable
private fun ResumenRadar(
    cantidadProductos: Int,
    cantidadPendiente: Int
) {
    TarjetaOperativa(
        acento = MaterialTheme.colorScheme.primary
    ) {
        EncabezadoSeccion(
            icono = Icons.Outlined.QueryStats,
            titulo = "Antes de reponer stock"
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

        producto.evaluadoEn?.let {
            ChipOperativo(
                texto = it,
                icono = Icons.Outlined.QueryStats,
                contenedor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            )
        }
    }
}

private enum class FiltroRadar(
    val etiqueta: String
) {
    TODOS("Todos"),
    SALUDABLES("Saludables"),
    EN_RIESGO("En riesgo"),
    SIN_LECTURA("Sin lectura");

    fun acepta(producto: TarjetaProductoRadarUiModel): Boolean {
        return when (this) {
            TODOS -> true
            SALUDABLES -> producto.veredicto == VeredictoComercial.SALUDABLE
            EN_RIESGO -> producto.veredicto == VeredictoComercial.PRECAUCION ||
                producto.veredicto == VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE ||
                producto.veredicto == VeredictoComercial.LIQUIDACION
            SIN_LECTURA -> producto.veredicto == null ||
                producto.veredicto == VeredictoComercial.INCONCLUSO
        }
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

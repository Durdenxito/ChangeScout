package com.app.changescout.ui.screens.product.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.app.changescout.domain.model.MetricasTendencia
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.VeredictoComercial
import com.app.changescout.ui.screens.components.BotonPrimario
import com.app.changescout.ui.screens.components.ChipOperativo
import com.app.changescout.ui.screens.components.EncabezadoSeccion
import com.app.changescout.ui.screens.components.FondoOperativo
import com.app.changescout.ui.screens.components.MetricaResumida
import com.app.changescout.ui.screens.components.TarjetaOperativa
import com.app.changescout.ui.theme.ErrorSoft
import com.app.changescout.ui.theme.SignalGold
import com.app.changescout.ui.theme.SuccessContainer
import com.app.changescout.ui.viewmodel.EfectoDetalleProducto
import com.app.changescout.ui.viewmodel.EstadoUiDetalleProducto
import com.app.changescout.ui.viewmodel.EventoDetalleProducto
import com.app.changescout.ui.viewmodel.ViewModelDetalleProducto
import kotlin.math.absoluteValue

@Composable
fun PantallaDetalleProducto(
    onNavegarAtras: () -> Unit,
    onNavegarAEditar: (ProductoImportado) -> Unit,
    detalleViewModel: ViewModelDetalleProducto = hiltViewModel()
) {
    val state by detalleViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(detalleViewModel) {
        detalleViewModel.uiEffect.collect { effect ->
            when (effect) {
                EfectoDetalleProducto.NavegarAtrasDesdeDetalle -> onNavegarAtras()
                is EfectoDetalleProducto.NavegarAEditarProducto -> onNavegarAEditar(effect.producto)
                is EfectoDetalleProducto.MostrarMensajeDetalle -> {
                    snackbarHostState.showSnackbar(effect.mensaje)
                }
            }
        }
    }

    FondoOperativo {
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
                    Text("Producto")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onEvent(EventoDetalleProducto.RegresarDesdeDetalleSolicitado)
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(EventoDetalleProducto.EditarProductoSolicitado) },
                        enabled = state.producto != null && !state.estaEliminando
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar producto")
                    }
                    IconButton(
                        onClick = { onEvent(EventoDetalleProducto.EliminarProductoSolicitado) },
                        enabled = state.producto != null && !state.estaEliminando
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar producto")
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
                TarjetaOperativa {
                    EncabezadoSeccion(
                        icono = Icons.Outlined.Schedule,
                        titulo = "Cargando producto"
                    )
                }
                return@Column
            }

            state.producto?.let { producto ->
                TarjetaOperativa {
                    EncabezadoSeccion(
                        icono = Icons.Outlined.Inventory2,
                        titulo = producto.nombre
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
                }
            }

            TarjetaOperativa {
                EncabezadoSeccion(
                    icono = Icons.Outlined.QueryStats,
                    titulo = "Resultado comercial"
                )

                if (state.evaluacion == null) {
                    ChipOperativo(
                        texto = "Sin lectura cargada",
                        icono = Icons.Outlined.Schedule,
                        contenedor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "Actualiza la ficha para comparar costo en destino contra precios reales del mercado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.evaluacion.evaluacionResumen(state.producto)
                }

                if (state.estaEvaluando) {
                    ChipOperativo(
                        texto = "Consultando mercado en vivo",
                        icono = Icons.Outlined.Search,
                        modifier = Modifier.fillMaxWidth(),
                        contenedor = SignalGold.copy(alpha = 0.18f)
                    )
                }

                BotonPrimario(
                    texto = if (state.estaEvaluando) "Consultando..." else "Actualizar lectura",
                    icono = Icons.Outlined.QueryStats,
                    onClick = {
                        onEvent(EventoDetalleProducto.EvaluarProductoActualSolicitado)
                    },
                    enabled = !state.estaEvaluando && state.producto != null,
                    modifier = Modifier.fillMaxWidth(),
                    cargando = state.estaEvaluando
                )
            }

            state.mensajeError?.let { mensaje ->
                TarjetaOperativa {
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

    if (state.mostrarConfirmarEliminacion) {
        AlertDialog(
            onDismissRequest = {
                onEvent(EventoDetalleProducto.CancelarEliminacionSolicitada)
            },
            title = { Text("Eliminar producto") },
            text = {
                Text("Se quitara esta ficha y sus lecturas guardadas del dispositivo.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(EventoDetalleProducto.ConfirmarEliminacionSolicitada)
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onEvent(EventoDetalleProducto.CancelarEliminacionSolicitada)
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EvaluacionComercial.evaluacionResumen(producto: ProductoImportado?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipOperativo(
            texto = veredicto?.etiqueta() ?: "Sin clasificar",
            icono = Icons.Outlined.Sell,
            contenedor = veredicto.colorContenedor()
        )
        Text(
            text = veredicto.descripcionUsuario(motivoEvidenciaInsuficiente),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChipOperativo(
            texto = estadoEvaluacion.aTextoPresentable(),
            icono = Icons.Outlined.Schedule,
            contenedor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

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
            titulo = "Precio sugerido",
            valor = precioVentaSugeridoPen?.let { "S/ ${"%.2f".format(it)}" } ?: "--",
            icono = Icons.Outlined.Sell,
            modifier = Modifier.weight(1f)
        )
        MetricaResumida(
            titulo = "Margen objetivo",
            valor = margenObjetivoPct?.let { "${"%.0f".format(it)}%" } ?: "--",
            icono = Icons.Outlined.QueryStats,
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

    gananciaEstimadaLotePen(producto)?.let { ganancia ->
        MetricaResumida(
            titulo = "Ganancia lote",
            valor = "S/ ${"%.2f".format(ganancia)}",
            icono = Icons.Outlined.AttachMoney,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (precioVentaSugeridoPen != null) {
        Text(
            text = descripcionPrecioSugerido(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (estadoEvaluacion == EstadoEvaluacion.OBSOLETO) {
        Text(
            text = "Esta lectura tiene mas de 12 horas. Actualiza para revisar el mercado de hoy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    tipoCambioVentaUsdPen?.let { tasa ->
        ChipOperativo(
            texto = "TC venta ${"%.3f".format(tasa)}",
            icono = Icons.Outlined.AttachMoney,
            contenedor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    metricasTendencia?.let { metricas ->
        TendenciasComerciales(metricas)
    }

    Text(
        text = "Evaluado ${evaluadoEn.aTextoRelativo()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TendenciasComerciales(metricas: MetricasTendencia) {
    val lecturas = listOfNotNull(
        metricas.erosionPrecioLocalPct?.takeIf { it.esCambioVisible() }?.let { valor ->
            "Los precios comparables ${valor.verboCambioPrecio()} ${valor.formatearAbs()} frente a tus lecturas recientes. ${valor.impactoPrecio()}"
        },
        metricas.variacionCompetidoresPct?.takeIf { it.esCambioVisible() }?.let { valor ->
            "Hay ${valor.formatearAbs()} ${valor.masOMenos()} vendedores similares que antes. ${valor.impactoCompetidores()}"
        },
        metricas.presionCambiariaPct?.takeIf { it.esCambioVisible() }?.let { valor ->
            "El dolar ${valor.verboCambioDolar()} ${valor.formatearAbs()} en la ventana revisada. ${valor.impactoDolar()}"
        }
    )

    if (lecturas.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Lectura del mercado",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        lecturas.forEach { lectura ->
            Text(
                text = lectura,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun VeredictoComercial?.etiqueta(): String {
    return when (this) {
        VeredictoComercial.SALUDABLE -> "Saludable"
        VeredictoComercial.PRECAUCION -> "Precaucion"
        VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE -> "Margen en riesgo"
        VeredictoComercial.LIQUIDACION -> "Conviene liquidar stock"
        VeredictoComercial.INCONCLUSO -> "Sin datos suficientes"
        null -> "Sin lectura"
    }
}

private fun VeredictoComercial?.descripcionUsuario(motivoEvidencia: String?): String {
    return when (this) {
        VeredictoComercial.SALUDABLE -> "El margen sigue por encima del rango de riesgo y las senales de mercado no presionan la ficha."
        VeredictoComercial.PRECAUCION -> "El producto todavia puede ser viable, pero conviene revisar precio, stock y nueva compra."
        VeredictoComercial.ALERTA_TEMPRANA_QUIEBRE -> "El margen esta cerca del punto de equilibrio. Una baja de precio o alza del dolar puede dejarlo sin ganancia."
        VeredictoComercial.LIQUIDACION -> "La lectura sugiere priorizar salida de stock antes de seguir comprando este producto."
        VeredictoComercial.INCONCLUSO -> motivoEvidencia ?: "No hay datos de mercado suficientes para recomendar una accion."
        null -> "Solicita una lectura para estimar margen, competencia y deterioro del mercado."
    }
}

private fun EvaluacionComercial.descripcionPrecioSugerido(): String {
    val margen = margenObjetivoPct?.let { "${"%.0f".format(it)}%" } ?: "objetivo"
    val brecha = brechaPrecioSugeridoMercadoPct ?: return "Para lograr $margen de margen, usa el precio sugerido como referencia antes de reponer stock."

    return if (brecha >= 0.0) {
        "El mercado esta ${brecha.formatearAbs()} por encima del precio sugerido. El margen objetivo parece viable."
    } else {
        "El mercado esta ${brecha.formatearAbs()} por debajo del precio sugerido. El margen objetivo no se sostiene con el precio actual."
    }
}

private fun EvaluacionComercial.gananciaEstimadaLotePen(producto: ProductoImportado?): Double? {
    val precio = precioPromedioRealPen ?: return null
    val costo = costoTotalPen ?: return null
    val cantidad = producto?.cantidadDisponible?.takeIf { it > 0 } ?: return null
    return (precio - costo) * cantidad
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
        EstadoEvaluacion.VIGENTE -> "Evaluado recientemente"
        EstadoEvaluacion.OBSOLETO -> "Informacion desactualizada"
        EstadoEvaluacion.INCONCLUSO -> "Sin datos suficientes"
        EstadoEvaluacion.FALLIDO -> "Lectura pendiente"
    }
}

private fun java.time.Instant.aTextoRelativo(): String {
    val segundos = ((System.currentTimeMillis() - toEpochMilli()) / 1000).coerceAtLeast(0)
    return when {
        segundos < 60 -> "hace menos de un minuto"
        segundos < 3_600 -> "hace ${segundos / 60} min"
        segundos < 86_400 -> "hace ${segundos / 3_600} h"
        else -> "hace ${segundos / 86_400} dias"
    }
}

private fun Double.formatearAbs(): String = "${"%.1f".format(absoluteValue)}%"

private fun Double.esCambioVisible(): Boolean = absoluteValue >= 0.05

private fun Double.verboCambioPrecio(): String = if (this < 0.0) "bajaron" else "subieron"

private fun Double.impactoPrecio(): String {
    return if (this < 0.0) {
        "Tu margen se esta comprimiendo."
    } else {
        "Hay mas espacio para sostener margen."
    }
}

private fun Double.masOMenos(): String = if (this >= 0.0) "mas" else "menos"

private fun Double.impactoCompetidores(): String {
    return if (this >= 0.0) {
        "El mercado se esta saturando."
    } else {
        "La presion competitiva bajo."
    }
}

private fun Double.verboCambioDolar(): String = if (this >= 0.0) "subio" else "bajo"

private fun Double.impactoDolar(): String {
    return if (this >= 0.0) {
        "Tu costo en soles aumento."
    } else {
        "Tu costo en soles se alivio."
    }
}

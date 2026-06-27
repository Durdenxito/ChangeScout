package com.app.changescout.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.app.changescout.ui.viewmodel.EstadoUiCuenta
import com.app.changescout.ui.viewmodel.ViewModelCuenta

@Composable
fun PantallaCuenta(
    onNavegarAtras: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: ViewModelCuenta = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FondoOperativo {
        ContenidoCuenta(
            state = state,
            onNavegarAtras = onNavegarAtras,
            onCerrarSesion = onCerrarSesion
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoCuenta(
    state: EstadoUiCuenta,
    onNavegarAtras: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Text("Cuenta")
                },
                navigationIcon = {
                    IconButton(onClick = onNavegarAtras) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
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
                .padding(PaddingValues(horizontal = 16.dp, vertical = 20.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaOperativa(acento = MaterialTheme.colorScheme.primary) {
                EncabezadoSeccion(
                    icono = Icons.Outlined.AccountCircle,
                    titulo = state.email.ifBlank { "Cuenta activa" }
                )
                ChipOperativo(
                    texto = "Ultima lectura: ${state.ultimaLectura}",
                    icono = Icons.Outlined.Schedule,
                    contenedor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                )
            }

            TarjetaOperativa(acento = state.acentoPrincipal()) {
                EncabezadoSeccion(
                    icono = Icons.Outlined.QueryStats,
                    titulo = "Lectura rapida"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricaResumida(
                        titulo = "Productos",
                        valor = state.totalProductos.toString(),
                        icono = Icons.Outlined.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                    MetricaResumida(
                        titulo = "Saludables",
                        valor = state.saludables.toString(),
                        icono = Icons.Outlined.CheckCircle,
                        acento = SignalMint,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            TarjetaOperativa(acento = SignalGold) {
                EncabezadoSeccion(
                    icono = Icons.Outlined.WarningAmber,
                    titulo = "Atencion antes de reponer"
                )
                FilaMetricas(
                    izquierdaTitulo = "Precaucion",
                    izquierdaValor = state.precaucion.toString(),
                    izquierdaIcono = Icons.Outlined.WarningAmber,
                    izquierdaAcento = SignalGold,
                    derechaTitulo = "Margen en riesgo",
                    derechaValor = state.margenEnRiesgo.toString(),
                    derechaIcono = Icons.AutoMirrored.Outlined.TrendingDown,
                    derechaAcento = MaterialTheme.colorScheme.error
                )
                FilaMetricas(
                    izquierdaTitulo = "Liquidar stock",
                    izquierdaValor = state.liquidarStock.toString(),
                    izquierdaIcono = Icons.Outlined.Sell,
                    izquierdaAcento = MaterialTheme.colorScheme.error,
                    derechaTitulo = "Sin datos",
                    derechaValor = state.sinDatos.toString(),
                    derechaIcono = Icons.AutoMirrored.Outlined.HelpOutline,
                    derechaAcento = MaterialTheme.colorScheme.primary
                )
                ChipOperativo(
                    texto = "${state.desactualizados} lecturas desactualizadas",
                    icono = Icons.Outlined.Schedule,
                    contenedor = if (state.desactualizados > 0) ErrorSoft else SuccessContainer
                )
            }

            BotonSecundario(
                texto = "Cerrar sesion",
                icono = Icons.AutoMirrored.Outlined.Logout,
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FilaMetricas(
    izquierdaTitulo: String,
    izquierdaValor: String,
    izquierdaIcono: androidx.compose.ui.graphics.vector.ImageVector,
    izquierdaAcento: Color,
    derechaTitulo: String,
    derechaValor: String,
    derechaIcono: androidx.compose.ui.graphics.vector.ImageVector,
    derechaAcento: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricaResumida(
            titulo = izquierdaTitulo,
            valor = izquierdaValor,
            icono = izquierdaIcono,
            acento = izquierdaAcento,
            modifier = Modifier.weight(1f)
        )
        MetricaResumida(
            titulo = derechaTitulo,
            valor = derechaValor,
            icono = derechaIcono,
            acento = derechaAcento,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EstadoUiCuenta.acentoPrincipal(): Color {
    return when {
        liquidarStock > 0 || margenEnRiesgo > 0 -> MaterialTheme.colorScheme.error
        precaucion > 0 -> SignalGold
        saludables > 0 -> SignalMint
        else -> MaterialTheme.colorScheme.primary
    }
}

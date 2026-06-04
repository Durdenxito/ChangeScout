package com.app.changescout.ui.screens.product.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.changescout.ui.screens.components.ChipOperativo
import com.app.changescout.ui.screens.components.EncabezadoSeccion
import com.app.changescout.ui.screens.components.FondoOperativoPremium
import com.app.changescout.ui.screens.components.MetricaResumida
import com.app.changescout.ui.screens.components.TarjetaPremium
import com.app.changescout.ui.viewmodel.EfectoFormularioProducto
import com.app.changescout.ui.viewmodel.EstadoUiFormularioProducto
import com.app.changescout.ui.viewmodel.EventoFormularioProducto
import com.app.changescout.ui.viewmodel.ViewModelFormularioProducto

@Composable
fun PantallaFormularioProducto(
    onNavegarAtras: () -> Unit,
    formularioViewModel: ViewModelFormularioProducto = hiltViewModel()
) {
    val state by formularioViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(formularioViewModel) {
        formularioViewModel.uiEffect.collect { effect ->
            when (effect) {
                EfectoFormularioProducto.NavegarAtrasDesdeFormulario -> onNavegarAtras()
                is EfectoFormularioProducto.MostrarMensajeFormulario -> {
                    snackbarHostState.showSnackbar(effect.mensaje)
                }
            }
        }
    }

    FondoOperativoPremium {
        ContenidoFormularioProducto(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = formularioViewModel::onEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContenidoFormularioProducto(
    state: EstadoUiFormularioProducto,
    snackbarHostState: SnackbarHostState,
    onEvent: (EventoFormularioProducto) -> Unit
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
                        Text("Alta de producto")
                        Text(
                            text = "Ficha base para seguimiento financiero",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = {
                        onEvent(EventoFormularioProducto.RegresarDesdeFormularioSolicitado)
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
            TarjetaPremium {
                Text(
                    text = "Abre una nueva ficha y deja lista la base comercial del producto: nombre, costos, stock y una referencia de mercado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricaResumida(
                        titulo = "Modo",
                        valor = "Operativo",
                        icono = Icons.Outlined.Tune,
                        modifier = Modifier.weight(1f)
                    )
                    MetricaResumida(
                        titulo = "Guardado",
                        valor = if (state.estaGuardando) "En curso" else "Manual",
                        icono = Icons.Outlined.Save,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            TarjetaPremium {
                EncabezadoSeccion(
                    icono = Icons.Outlined.Description,
                    titulo = "Ficha principal",
                    subtitulo = "Usa un nombre y una referencia faciles de reconocer en el mercado."
                )
                CampoPremium(
                    value = state.nombre,
                    onValueChange = { onEvent(EventoFormularioProducto.NombreCambiado(it)) },
                    label = "Nombre del producto",
                    leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) }
                )
                CampoPremium(
                    value = state.precioFobUsd,
                    onValueChange = { onEvent(EventoFormularioProducto.PrecioFobUsdCambiado(it)) },
                    label = "Precio del producto en origen (USD)",
                    supportingText = "Monto pagado al proveedor antes del envio.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null) }
                )
                CampoPremium(
                    value = state.fleteUsd,
                    onValueChange = { onEvent(EventoFormularioProducto.FleteUsdCambiado(it)) },
                    label = "Envio internacional (USD)",
                    supportingText = "Costo de transporte hasta destino. Si no aplica, dejalo vacio.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null) }
                )
                CampoPremium(
                    value = state.seguroUsd,
                    onValueChange = { onEvent(EventoFormularioProducto.SeguroUsdCambiado(it)) },
                    label = "Seguro de carga (USD)",
                    supportingText = "Proteccion de la mercaderia. Si no aplica, dejalo vacio.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null) }
                )
                CampoPremium(
                    value = state.arancelesUsd,
                    onValueChange = { onEvent(EventoFormularioProducto.ArancelesUsdCambiado(it)) },
                    label = "Impuestos de importacion (USD)",
                    supportingText = "Tributos, aranceles u otros pagos de ingreso.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null) }
                )
                CampoPremium(
                    value = state.otrosCargosUsd,
                    onValueChange = { onEvent(EventoFormularioProducto.OtrosCargosUsdCambiado(it)) },
                    label = "Gastos adicionales (USD)",
                    supportingText = "Agente, almacenaje, manejo u otros. Si no aplica, dejalo vacio.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null) }
                )
                CampoPremium(
                    value = state.cantidadDisponible,
                    onValueChange = { onEvent(EventoFormularioProducto.CantidadDisponibleCambiada(it)) },
                    label = "Unidades disponibles",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Outlined.Numbers, contentDescription = null) }
                )
                CampoPremium(
                    value = state.queryCompetencia,
                    onValueChange = { onEvent(EventoFormularioProducto.QueryCompetenciaCambiado(it)) },
                    label = "Producto a buscar en el mercado",
                    supportingText = "Ejemplo: consola portatil retro, audifonos bluetooth.",
                    minLines = 3,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                )
                ChipOperativo(
                    texto = "El borrador se conserva mientras sigas dentro de la sesion actual.",
                    icono = Icons.Outlined.Tune
                )
            }

            state.mensajeValidacion?.let { mensaje ->
                TarjetaPremium {
                    Text(
                        text = mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = {
                    onEvent(EventoFormularioProducto.GuardarProductoSolicitado)
                },
                enabled = state.puedeEnviar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(if (state.estaGuardando) "Guardando ficha..." else "Guardar ficha")
            }
        }
    }
}

@Composable
private fun CampoPremium(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = supportingText?.let { texto ->
            { Text(texto) }
        },
        leadingIcon = leadingIcon,
        minLines = minLines,
        singleLine = minLines == 1,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        )
    )
}

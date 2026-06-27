package com.app.changescout.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.app.changescout.ui.screens.components.BotonPrimario
import com.app.changescout.ui.screens.components.BotonSecundario
import com.app.changescout.ui.screens.components.FondoOperativo
import com.app.changescout.ui.theme.BullionGold
import com.app.changescout.ui.theme.OutlineSubtle
import com.app.changescout.ui.viewmodel.EstadoUiSesion
import com.app.changescout.ui.viewmodel.EventoSesion

@Composable
fun PantallaSesion(
    state: EstadoUiSesion,
    onEvent: (EventoSesion) -> Unit
) {
    FondoOperativo {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(PaddingValues(horizontal = 18.dp, vertical = 34.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoSesion()
                Text(
                    text = "Consulta el mercado. Protege tus evaluaciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(48.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CampoSesion(
                            value = state.email,
                            onValueChange = { value -> onEvent(EventoSesion.EmailCambiado(value)) },
                            placeholder = "Correo",
                            leadingIcon = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        CampoSesion(
                            value = state.password,
                            onValueChange = { value -> onEvent(EventoSesion.PasswordCambiado(value)) },
                            placeholder = "Clave",
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation()
                        )

                        state.mensaje?.let { mensaje ->
                            Text(
                                text = mensaje,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        BotonPrimario(
                            texto = "Entrar",
                            icono = Icons.AutoMirrored.Outlined.Login,
                            onClick = { onEvent(EventoSesion.IniciarSesion) },
                            modifier = Modifier.fillMaxWidth(),
                            cargando = state.estaCargando
                        )

                        SeparadorSesion()

                        BotonSecundario(
                            texto = "Crear cuenta",
                            icono = Icons.Outlined.PersonAdd,
                            onClick = { onEvent(EventoSesion.CrearCuenta) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.estaCargando
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoSesion() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Change ",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Sc",
            style = MaterialTheme.typography.headlineLarge,
            color = BullionGold,
            fontWeight = FontWeight.ExtraBold
        )
        Icon(
            imageVector = Icons.Outlined.Visibility,
            contentDescription = null,
            tint = BullionGold
        )
        Text(
            text = "ut",
            style = MaterialTheme.typography.headlineLarge,
            color = BullionGold,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun CampoSesion(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = leadingIcon,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
            unfocusedBorderColor = OutlineSubtle,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun SeparadorSesion() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineSubtle)
        Text(
            text = "o",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineSubtle)
    }
}

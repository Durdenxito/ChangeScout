package com.app.changescout.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.changescout.data.auth.RepositorioSesionSupabase
import com.app.changescout.data.auth.SesionUsuario
import com.app.changescout.domain.model.ResultadoOperacion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class EstadoUiSesion(
    val nombreUsuario: String = "",
    val email: String = "",
    val password: String = "",
    val sesion: SesionUsuario? = null,
    val estaCargando: Boolean = false,
    val mensaje: String? = null
) {
    val estaAutenticado: Boolean = sesion != null
}

sealed interface EventoSesion {
    data class NombreUsuarioCambiado(val valor: String) : EventoSesion
    data class EmailCambiado(val valor: String) : EventoSesion
    data class PasswordCambiado(val valor: String) : EventoSesion
    data object IniciarSesion : EventoSesion
    data object CrearCuenta : EventoSesion
    data object CerrarSesion : EventoSesion
}

@HiltViewModel
class ViewModelSesion @Inject constructor(
    private val repositorioSesion: RepositorioSesionSupabase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        EstadoUiSesion(sesion = repositorioSesion.sesionActual())
    )
    val uiState: StateFlow<EstadoUiSesion> = _uiState.asStateFlow()

    fun onEvent(event: EventoSesion) {
        when (event) {
            is EventoSesion.NombreUsuarioCambiado -> _uiState.update {
                it.copy(nombreUsuario = event.valor, mensaje = null)
            }

            is EventoSesion.EmailCambiado -> _uiState.update {
                it.copy(email = event.valor, mensaje = null)
            }

            is EventoSesion.PasswordCambiado -> _uiState.update {
                it.copy(password = event.valor, mensaje = null)
            }

            EventoSesion.IniciarSesion -> autenticar(crearCuenta = false)
            EventoSesion.CrearCuenta -> autenticar(crearCuenta = true)
            EventoSesion.CerrarSesion -> {
                repositorioSesion.cerrarSesion()
                _uiState.value = EstadoUiSesion()
            }
        }
    }

    private fun autenticar(crearCuenta: Boolean) {
        val estado = _uiState.value
        val nombreUsuario = estado.nombreUsuario.trim()
        val email = estado.email.trim()
        val password = estado.password
        if (!email.contains("@") || password.length < 6) {
            _uiState.update {
                it.copy(
                    password = "",
                    mensaje = "Ingresa un correo valido y una clave de al menos 6 caracteres."
                )
            }
            return
        }
        if (crearCuenta && nombreUsuario.isBlank()) {
            _uiState.update {
                it.copy(
                    password = "",
                    mensaje = "Ingresa un nombre para identificar tu cuenta."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(estaCargando = true, mensaje = null) }
            val resultado = if (crearCuenta) {
                repositorioSesion.crearCuenta(email, password, nombreUsuario)
            } else {
                repositorioSesion.iniciarSesion(email, password, nombreUsuario.takeIf { it.isNotBlank() })
            }
            _uiState.update { estadoActual ->
                when (resultado) {
                    is ResultadoOperacion.Exito -> estadoActual.copy(
                        sesion = resultado.data,
                        estaCargando = false,
                        password = "",
                        mensaje = null
                    )

                    is ResultadoOperacion.Fallo -> estadoActual.copy(
                        estaCargando = false,
                        password = "",
                        mensaje = resultado.error.mensaje
                    )

                    is ResultadoOperacion.DatosObsoletos -> estadoActual.copy(
                        sesion = resultado.data,
                        estaCargando = false,
                        password = "",
                        mensaje = null
                    )
                }
            }
        }
    }
}

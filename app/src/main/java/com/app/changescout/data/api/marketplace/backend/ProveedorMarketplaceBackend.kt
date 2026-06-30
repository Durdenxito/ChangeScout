package com.app.changescout.data.api.marketplace.backend

import com.app.changescout.data.api.backend.BackendProxyConfig
import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.repository.ProveedorMarketplace
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class ProveedorMarketplaceBackend @Inject constructor(
    private val api: BackendMarketplaceApi
) : ProveedorMarketplace {
    override val nombreProveedor: String = BackendProxyConfig.NOMBRE_PROVEEDOR_MARKETPLACE

    override suspend fun buscar(
        query: String,
        limit: Int
    ): ResultadoOperacion<List<PublicacionMercado>> {
        val querySeguro = query.trim()
        if (querySeguro.isBlank()) {
            return ResultadoOperacion.Fallo(
                ErrorOperacion.Validacion("El query de competencia no puede estar vacio.")
            )
        }

        return try {
            val limiteSeguro = limit.coerceIn(1, BackendProxyConfig.LIMITE_MAXIMO_BUSQUEDA)
            val publicaciones = api.buscarPublicaciones(
                query = querySeguro,
                limit = limiteSeguro
            )
                .mapNotNull { publicacion -> publicacion.toDomain(nombreProveedor) }

            ResultadoOperacion.Exito(publicaciones)
        } catch (error: CancellationException) {
            throw error
        } catch (error: SocketTimeoutException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.Timeout(
                    proveedor = nombreProveedor,
                    mensaje = "El marketplace esta demorando por consultas repetidas. Prueba otro producto o intenta de nuevo mas tarde."
                )
            )
        } catch (error: HttpException) {
            val mensaje = if (error.code() == 401) {
                "Tu sesion expiro. Cierra sesion e ingresa nuevamente."
            } else {
                error.mensajeProxy() ?: "El proxy de marketplace respondio con HTTP ${error.code()}."
            }
            ResultadoOperacion.Fallo(
                ErrorOperacion.ProveedorNoDisponible(
                    proveedor = nombreProveedor,
                    mensaje = mensaje
                )
            )
        } catch (error: IOException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.ProveedorNoDisponible(
                    proveedor = nombreProveedor,
                    mensaje = "No se pudo conectar con el proxy de marketplace."
                )
            )
        } catch (error: RuntimeException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.RespuestaInvalida(
                    proveedor = nombreProveedor,
                    mensaje = "La respuesta del proxy de marketplace no tiene el formato esperado."
                )
            )
        }
    }

    private fun HttpException.mensajeProxy(): String? {
        val body = response()?.errorBody()?.string() ?: return null
        return runCatching {
            Gson().fromJson(body, BackendErrorDto::class.java)
                ?.message
                ?.takeIf { mensaje -> mensaje.isNotBlank() }
        }.getOrNull()
    }
}

private data class BackendErrorDto(
    @SerializedName("message")
    val message: String?
)

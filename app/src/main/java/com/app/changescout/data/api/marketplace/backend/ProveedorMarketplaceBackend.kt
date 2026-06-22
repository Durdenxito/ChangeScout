package com.app.changescout.data.api.marketplace.backend

import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.repository.ProveedorMarketplace
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class ProveedorMarketplaceBackend @Inject constructor(
    private val api: BackendMarketplaceApi
) : ProveedorMarketplace {
    override val nombreProveedor: String = BackendMarketplaceConfig.NOMBRE_PROVEEDOR

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
            val limiteSeguro = limit.coerceIn(1, BackendMarketplaceConfig.LIMITE_MAXIMO_BUSQUEDA)
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
                    mensaje = "El proxy de marketplace no respondio dentro del tiempo esperado."
                )
            )
        } catch (error: HttpException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.ProveedorNoDisponible(
                    proveedor = nombreProveedor,
                    mensaje = "El proxy de marketplace respondio con HTTP ${error.code()}."
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
}

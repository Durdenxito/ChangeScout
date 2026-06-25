package com.app.changescout.data.api.nlp.backend

import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ResultadoFiltroNlp
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.repository.ProveedorFiltroNlp
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class ProveedorFiltroNlpBackend @Inject constructor(
    private val api: BackendNlpApi
) : ProveedorFiltroNlp {
    override val nombreProveedor: String = BackendNlpConfig.NOMBRE_PROVEEDOR

    override suspend fun filtrar(
        publicaciones: List<PublicacionMercado>,
        producto: ProductoImportado
    ): ResultadoOperacion<ResultadoFiltroNlp> {
        if (publicaciones.isEmpty()) {
            return ResultadoOperacion.Exito(resultadoVacio())
        }

        return try {
            val response = api.filtrarPublicaciones(
                BackendNlpRequest(
                    producto = producto.toBackendNlpDto(),
                    publicaciones = publicaciones.map { publicacion -> publicacion.toBackendNlpDto() }
                )
            )
            ResultadoOperacion.Exito(response.toDomain())
        } catch (error: CancellationException) {
            throw error
        } catch (error: SocketTimeoutException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.Timeout(
                    proveedor = nombreProveedor,
                    mensaje = "El proxy NLP no respondio dentro del tiempo esperado."
                )
            )
        } catch (error: HttpException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.ProveedorNoDisponible(
                    proveedor = nombreProveedor,
                    mensaje = "El proxy NLP respondio con HTTP ${error.code()}."
                )
            )
        } catch (error: IOException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.ProveedorNoDisponible(
                    proveedor = nombreProveedor,
                    mensaje = "No se pudo conectar con el proxy NLP."
                )
            )
        } catch (error: RuntimeException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.RespuestaInvalida(
                    proveedor = nombreProveedor,
                    mensaje = "La respuesta del proxy NLP no tiene el formato esperado."
                )
            )
        }
    }

    private fun resultadoVacio(): ResultadoFiltroNlp {
        return ResultadoFiltroNlp(
            publicacionesValidas = emptyList(),
            cantidadDescartadas = 0,
            razonesDescarte = emptyList(),
            precioPromedioRealPen = null,
            competidoresValidos = 0,
            puntajeConfianza = 0.0,
            trazaProveedor = "proveedor=$nombreProveedor | total=0 | validas=0"
        )
    }
}

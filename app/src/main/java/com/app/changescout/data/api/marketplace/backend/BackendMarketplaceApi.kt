package com.app.changescout.data.api.marketplace.backend

import com.app.changescout.BuildConfig
import com.app.changescout.domain.model.CondicionPublicacion
import com.app.changescout.domain.model.Moneda
import com.app.changescout.domain.model.PublicacionMercado
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface BackendMarketplaceApi {
    @GET("marketplace/search")
    suspend fun buscarPublicaciones(
        @Query("query") query: String,
        @Query("country") country: String = BackendMarketplaceConfig.PAIS_PERU,
        @Query("limit") limit: Int
    ): List<BackendPublicacionDto>
}

object BackendMarketplaceConfig {
    val BASE_URL: String = BuildConfig.MARKETPLACE_BACKEND_URL
    const val NOMBRE_PROVEEDOR = "ChangeScout Marketplace Proxy"
    const val PAIS_PERU = "PE"
    const val LIMITE_MAXIMO_BUSQUEDA = 5
}

data class BackendPublicacionDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("price")
    val price: Double?,
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("condition")
    val condition: String?,
    @SerializedName("url")
    val url: String?
) {
    fun toDomain(nombreFuente: String): PublicacionMercado? {
        val publicacionId = id?.takeIf { value -> value.isNotBlank() } ?: return null
        val titulo = title?.takeIf { value -> value.isNotBlank() } ?: return null
        val precio = price?.takeIf { value -> value > 0.0 } ?: return null

        return PublicacionMercado(
            publicacionId = publicacionId,
            titulo = titulo,
            precio = precio,
            moneda = currency.toMoneda(),
            condicion = condition.toCondicionPublicacion(),
            permalink = url,
            nombreFuente = nombreFuente
        )
    }
}

private fun String?.toMoneda(): Moneda {
    return when (this?.uppercase()) {
        "PEN" -> Moneda.PEN
        "USD" -> Moneda.USD
        else -> Moneda.DESCONOCIDA
    }
}

private fun String?.toCondicionPublicacion(): CondicionPublicacion {
    return when (this?.lowercase()) {
        "new", "nuevo" -> CondicionPublicacion.NUEVO
        "used", "usado" -> CondicionPublicacion.USADO
        else -> CondicionPublicacion.DESCONOCIDO
    }
}

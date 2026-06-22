package com.app.changescout.backend

import com.google.gson.annotations.SerializedName
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.IOException

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, host = "0.0.0.0", port = port, module = Application::module)
        .start(wait = true)
}

fun Application.module(
    config: ApifyConfig = ApifyConfig.fromEnv()
) {
    install(ServerContentNegotiation) {
        gson()
    }

    val client = HttpClient(CIO) {
        expectSuccess = true
        install(ClientContentNegotiation) {
            gson()
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = config.timeoutMillis
            socketTimeoutMillis = config.timeoutMillis
        }
    }
    val marketplace = ApifyMarketplaceService(client, config)

    monitor.subscribe(ApplicationStopping) {
        client.close()
    }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        get("/marketplace/search") {
            val query = call.request.queryParameters["query"]?.trim().orEmpty()
            if (query.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("El query de competencia no puede estar vacio.")
                )
                return@get
            }

            val limit = call.request.queryParameters["limit"]
                ?.toIntOrNull()
                ?.coerceIn(1, config.maxItems)
                ?: config.maxItems
            val country = call.request.queryParameters["country"]
                ?.trim()
                ?.takeIf { value -> value.isNotBlank() }
                ?: "PE"

            try {
                call.respond(marketplace.buscar(query = query, country = country, limit = limit))
            } catch (error: ApifyConfigException) {
                call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse(error.message))
            } catch (error: HttpRequestTimeoutException) {
                call.respond(HttpStatusCode.GatewayTimeout, ErrorResponse("Apify no respondio a tiempo."))
            } catch (error: ClientRequestException) {
                val detail = error.response.apifyErrorDetail()
                call.respond(
                    HttpStatusCode.BadGateway,
                    ErrorResponse("Apify rechazo la solicitud con HTTP ${error.response.status.value}.$detail")
                )
            } catch (error: ServerResponseException) {
                val detail = error.response.apifyErrorDetail()
                call.respond(
                    HttpStatusCode.BadGateway,
                    ErrorResponse("Apify fallo con HTTP ${error.response.status.value}.$detail")
                )
            } catch (error: IOException) {
                call.respond(HttpStatusCode.BadGateway, ErrorResponse("No se pudo conectar con Apify."))
            }
        }
    }
}

data class ApifyConfig(
    val token: String?,
    val actorId: String,
    val timeoutMillis: Long,
    val maxItems: Int,
    val proxyGroups: List<String>
) {
    val actorIdParaRuta: String = actorId.replace("/", "~")

    companion object {
        fun fromEnv(): ApifyConfig {
            return ApifyConfig(
                token = System.getenv("APIFY_TOKEN"),
                actorId = System.getenv("APIFY_ACTOR_ID")
                    ?: "scrapers_lat/mercadolibre-scraper",
                timeoutMillis = System.getenv("APIFY_TIMEOUT_MS")?.toLongOrNull() ?: 180_000L,
                maxItems = System.getenv("MARKETPLACE_MAX_ITEMS")?.toIntOrNull()?.coerceIn(1, 5)
                    ?: 5,
                proxyGroups = System.getenv("APIFY_PROXY_GROUPS")
                    ?.split(",")
                    ?.map { value -> value.trim() }
                    ?.filter { value -> value.isNotBlank() }
                    ?: listOf("RESIDENTIAL")
            )
        }
    }
}

class ApifyMarketplaceService(
    private val client: HttpClient,
    private val config: ApifyConfig
) {
    suspend fun buscar(
        query: String,
        country: String,
        limit: Int
    ): List<MarketplacePublicacionResponse> {
        val token = config.token?.takeIf { value -> value.isNotBlank() }
            ?: throw ApifyConfigException("Falta APIFY_TOKEN en el backend.")
        val input = ApifySearchInput(
            searchTerm = query,
            country = country.lowercase(),
            maxItems = limit.coerceIn(1, config.maxItems),
            proxyConfiguration = ApifyProxyConfig(
                apifyProxyGroups = config.proxyGroups
            )
        )

        val items = client.post(
            "https://api.apify.com/v2/actors/${config.actorIdParaRuta}/run-sync-get-dataset-items"
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(input)
        }.body<List<ApifyProductoDto>>()

        return items.mapNotNull { item -> item.toPublicacionResponse() }
    }
}

data class ApifySearchInput(
    val searchTerm: String,
    val country: String,
    val maxItems: Int,
    val withDetails: Boolean = false,
    val proxyConfiguration: ApifyProxyConfig
)

data class ApifyProxyConfig(
    val useApifyProxy: Boolean = true,
    val apifyProxyGroups: List<String>
)

data class ApifyProductoDto(
    @SerializedName("listingId")
    val listingId: String?,
    val title: String?,
    val price: Double?,
    val currency: String?,
    val url: String?,
    val error: String?
) {
    fun toPublicacionResponse(): MarketplacePublicacionResponse? {
        if (!error.isNullOrBlank()) return null
        val publicacionId = listingId?.takeIf { value -> value.isNotBlank() } ?: return null
        val titulo = title?.takeIf { value -> value.isNotBlank() } ?: return null
        val precio = price?.takeIf { value -> value > 0.0 } ?: return null
        val moneda = currency?.takeIf { value -> value.isNotBlank() } ?: "PEN"

        return MarketplacePublicacionResponse(
            id = publicacionId,
            title = titulo,
            price = precio,
            currency = moneda,
            condition = null,
            url = url.normalizarUrl()
        )
    }
}

data class MarketplacePublicacionResponse(
    val id: String,
    val title: String,
    val price: Double,
    val currency: String,
    val condition: String?,
    val url: String?
)

data class ErrorResponse(
    val message: String
)

class ApifyConfigException(
    override val message: String
) : RuntimeException(message)

private fun String?.normalizarUrl(): String? {
    val value = this?.takeIf { url -> url.isNotBlank() } ?: return null
    return if (value.startsWith("http://") || value.startsWith("https://")) {
        value
    } else {
        "https://$value"
    }
}

private suspend fun io.ktor.client.statement.HttpResponse.apifyErrorDetail(): String {
    val body = runCatching { bodyAsText() }.getOrNull()
        ?.trim()
        ?.takeIf { value -> value.isNotBlank() }
        ?.take(300)
    return body?.let { value -> " Detalle: $value" }.orEmpty()
}

package com.app.changescout.backend

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

data class ErrorResponse(
    val message: String
)

suspend fun HttpResponse.externalErrorDetail(): String {
    val body = runCatching { bodyAsText() }.getOrNull()
        ?.trim()
        ?.takeIf { value -> value.isNotBlank() }
        ?.take(300)
    return body?.let { value -> " Detalle: $value" }.orEmpty()
}

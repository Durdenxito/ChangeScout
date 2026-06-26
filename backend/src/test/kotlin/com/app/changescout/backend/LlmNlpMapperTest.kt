package com.app.changescout.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LlmNlpMapperTest {
    @Test
    fun outputText_extraePrimerTextoUtil() {
        val response = LlmResponsesResponse(
            output = listOf(
                LlmOutputItem(
                    content = listOf(
                        LlmOutputContent(
                            type = "output_text",
                            text = """{"idsValidos":["1"],"descartes":[],"puntajeConfianza":0.8}""",
                            refusal = null
                        )
                    )
                )
            )
        )

        assertEquals(
            """{"idsValidos":["1"],"descartes":[],"puntajeConfianza":0.8}""",
            response.outputText()
        )
        assertNull(response.refusal())
    }

    @Test
    fun outputText_usaCampoDirectoSiProveedorLoDevuelve() {
        val response = LlmResponsesResponse(
            outputTextDirecto = """{"idsValidos":[],"descartes":[],"puntajeConfianza":0.1}"""
        )

        assertEquals(
            """{"idsValidos":[],"descartes":[],"puntajeConfianza":0.1}""",
            response.outputText()
        )
    }

    @Test
    fun toFiltroResponse_calculaPromedioDesdePublicacionesLocales() {
        val decision = LlmDecisionNlpResponse(
            idsValidos = listOf("1", "2", "999"),
            descartes = listOf(
                LlmDescarteNlpResponse(publicacionId = "3", razon = "titulo contaminado")
            ),
            puntajeConfianza = 1.4
        )
        val publicaciones = listOf(
            publicacion("1", "Audifonos Xiaomi", 39.0),
            publicacion("2", "Audifonos JBL", 169.0),
            publicacion("3", "Funda para audifonos", 19.0),
            publicacion("4", "Audifonos USD", 20.0, currency = "USD")
        )

        val response = decision.toFiltroResponse(
            publicaciones = publicaciones,
            proveedor = "Groq",
            trazaExtra = "modelo=qwen-test | prompt=nlp-filter-v1:abc123"
        )

        assertEquals(2, response.competidoresValidos)
        assertEquals(2, response.cantidadDescartadas)
        assertEquals(104.0, response.precioPromedioRealPen ?: 0.0, 0.0001)
        assertEquals(1.0, response.puntajeConfianza, 0.0)
        assertEquals("audifonos xiaomi", response.publicacionesValidas.first().tituloNormalizado)
        assertEquals(
            "proveedor=Groq | modelo=qwen-test | prompt=nlp-filter-v1:abc123 | total=4 | validas=2",
            response.trazaProveedor
        )
    }

    private fun publicacion(
        id: String,
        title: String,
        price: Double,
        currency: String = "PEN"
    ): NlpPublicacionRequest {
        return NlpPublicacionRequest(
            id = id,
            title = title,
            price = price,
            currency = currency,
            condition = null,
            url = null
        )
    }
}

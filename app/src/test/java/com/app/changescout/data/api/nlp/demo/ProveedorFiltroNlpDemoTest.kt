package com.app.changescout.data.api.nlp.demo

import com.app.changescout.domain.model.ComponentesCostoImportacion
import com.app.changescout.domain.model.CondicionPublicacion
import com.app.changescout.domain.model.Moneda
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ResultadoOperacion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProveedorFiltroNlpDemoTest {
    private val proveedor = ProveedorFiltroNlpDemo()

    @Test
    fun filtrar_descartaUsadosAccesoriosReplicasYCalculaPromedio() = runBlocking {
        val resultado = proveedor.filtrar(
            publicaciones = listOf(
                publicacion("1", "Consola portatil nuevo sellado", 629.0),
                publicacion("2", "Consola portatil usado excelente", 489.0, CondicionPublicacion.USADO),
                publicacion("3", "Consola portatil accesorio funda", 59.0),
                publicacion("4", "Consola portatil replica alternativa", 219.0),
                publicacion("5", "Consola portatil nuevo garantia", 619.0)
            ),
            producto = producto()
        )

        assertTrue(resultado is ResultadoOperacion.Exito)
        val filtro = (resultado as ResultadoOperacion.Exito).data
        assertEquals(2, filtro.competidoresValidos)
        assertEquals(3, filtro.cantidadDescartadas)
        assertEquals(624.0, filtro.precioPromedioRealPen ?: 0.0, 0.0001)
        assertTrue(filtro.razonesDescarte.any { razon ->
            razon.contains("titulo contaminado")
        })
        assertNotNull(filtro.puntajeConfianza)
    }

    @Test
    fun filtrar_descartaMonedaNoPen() = runBlocking {
        val resultado = proveedor.filtrar(
            publicaciones = listOf(
                publicacion("1", "Producto nuevo", 100.0, moneda = Moneda.USD)
            ),
            producto = producto()
        )

        assertTrue(resultado is ResultadoOperacion.Exito)
        val filtro = (resultado as ResultadoOperacion.Exito).data
        assertEquals(0, filtro.competidoresValidos)
        assertEquals(1, filtro.cantidadDescartadas)
        assertEquals(null, filtro.precioPromedioRealPen)
        assertTrue(filtro.razonesDescarte.any { razon ->
            razon.contains("moneda no PEN")
        })
    }

    private fun publicacion(
        id: String,
        titulo: String,
        precio: Double,
        condicion: CondicionPublicacion = CondicionPublicacion.NUEVO,
        moneda: Moneda = Moneda.PEN
    ): PublicacionMercado {
        return PublicacionMercado(
            publicacionId = id,
            titulo = titulo,
            precio = precio,
            moneda = moneda,
            condicion = condicion,
            permalink = null,
            nombreFuente = "test"
        )
    }

    private fun producto(): ProductoImportado {
        return ProductoImportado(
            id = 1L,
            nombre = "Consola portatil",
            queryCompetencia = "consola portatil",
            componentesCosto = ComponentesCostoImportacion(
                precioFobUsd = 100.0,
                fleteUsd = 10.0,
                seguroUsd = 5.0,
                arancelesUsd = 5.0
            ),
            cantidadDisponible = 10
        )
    }
}

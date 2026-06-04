package com.app.changescout.data.api.marketplace.demo

import com.app.changescout.domain.model.CondicionPublicacion
import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.model.ResultadoOperacion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProveedorMarketplaceDemoTest {
    private val proveedor = ProveedorMarketplaceDemo()

    @Test
    fun buscar_respetaLimiteSolicitado() = runBlocking {
        val resultado = proveedor.buscar(
            query = "consola portatil",
            limit = 3
        )

        assertTrue(resultado is ResultadoOperacion.Exito)
        assertEquals(3, (resultado as ResultadoOperacion.Exito).data.size)
    }

    @Test
    fun buscar_conMismoQueryDevuelveResultadoDeterministico() = runBlocking {
        val primeraConsulta = proveedor.buscar(
            query = "consola portatil",
            limit = 5
        )
        val segundaConsulta = proveedor.buscar(
            query = "consola portatil",
            limit = 5
        )

        assertEquals(primeraConsulta, segundaConsulta)
    }

    @Test
    fun buscar_incluyePublicacionesCrudasConRuidoParaFiltroPosterior() = runBlocking {
        val resultado = proveedor.buscar(
            query = "audifonos bluetooth",
            limit = 10
        )

        assertTrue(resultado is ResultadoOperacion.Exito)
        val publicaciones = (resultado as ResultadoOperacion.Exito).data
        assertTrue(publicaciones.any { publicacion ->
            publicacion.condicion == CondicionPublicacion.USADO
        })
        assertTrue(publicaciones.any { publicacion ->
            publicacion.titulo.contains("accesorio", ignoreCase = true)
        })
        assertTrue(publicaciones.any { publicacion ->
            publicacion.titulo.contains("replica", ignoreCase = true)
        })
    }

    @Test
    fun buscar_conQueryVacioRetornaValidacion() = runBlocking {
        val resultado = proveedor.buscar(
            query = " ",
            limit = 5
        )

        assertTrue(resultado is ResultadoOperacion.Fallo)
        assertTrue((resultado as ResultadoOperacion.Fallo).error is ErrorOperacion.Validacion)
    }
}

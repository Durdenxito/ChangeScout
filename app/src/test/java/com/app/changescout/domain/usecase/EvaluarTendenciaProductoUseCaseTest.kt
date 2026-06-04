package com.app.changescout.domain.usecase

import com.app.changescout.domain.model.ComponentesCostoImportacion
import com.app.changescout.domain.model.CondicionPublicacion
import com.app.changescout.domain.model.CotizacionTipoCambio
import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.model.EstadoSnapshot
import com.app.changescout.domain.model.Moneda
import com.app.changescout.domain.model.ProductoImportado
import com.app.changescout.domain.model.PublicacionMercado
import com.app.changescout.domain.model.ResultadoFiltroNlp
import com.app.changescout.domain.model.ResultadoOperacion
import com.app.changescout.domain.model.SnapshotEvaluacionComercial
import com.app.changescout.domain.model.VeredictoComercial
import com.app.changescout.domain.repository.ProveedorFiltroNlp
import com.app.changescout.domain.repository.ProveedorMarketplace
import com.app.changescout.domain.repository.ProveedorTipoCambio
import com.app.changescout.domain.repository.RepositorioEvaluacionComercial
import com.app.changescout.domain.repository.RepositorioProductoImportado
import com.app.changescout.domain.rules.CalculadoraLandedCost
import com.app.changescout.domain.rules.ClasificadorVeredictoComercial
import com.app.changescout.domain.rules.MotorTendenciaComercial
import com.app.changescout.domain.rules.PoliticaEvidencia
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluarTendenciaProductoUseCaseTest {
    private val ahora = Instant.parse("2026-06-04T12:00:00Z")
    private val clock = Clock.fixed(ahora, ZoneOffset.UTC)
    private lateinit var repositorioProducto: FakeRepositorioProductoImportado
    private lateinit var repositorioEvaluacion: FakeRepositorioEvaluacionComercial
    private lateinit var proveedorTipoCambio: FakeProveedorTipoCambio
    private lateinit var proveedorMarketplace: FakeProveedorMarketplace
    private lateinit var proveedorFiltroNlp: FakeProveedorFiltroNlp

    @Before
    fun setUp() {
        repositorioProducto = FakeRepositorioProductoImportado(productoBase())
        repositorioEvaluacion = FakeRepositorioEvaluacionComercial()
        proveedorTipoCambio = FakeProveedorTipoCambio(
            ResultadoOperacion.Exito(cotizacionBase())
        )
        proveedorMarketplace = FakeProveedorMarketplace(
            ResultadoOperacion.Exito(listOf(publicacionBase()))
        )
        proveedorFiltroNlp = FakeProveedorFiltroNlp(
            ResultadoOperacion.Exito(filtroBase())
        )
    }

    @Test
    fun invoke_conEvidenciaSuficiente_guardaSnapshotVigenteConVeredicto() = runBlocking {
        val resultado = crearUseCase().invoke(productoId = 7L)

        assertTrue(resultado is ResultadoOperacion.Exito)
        val snapshot = (resultado as ResultadoOperacion.Exito).data
        assertEquals(1, repositorioEvaluacion.guardados.size)
        assertEquals(snapshot, repositorioEvaluacion.guardados.single())
        assertEquals(EstadoSnapshot.VIGENTE, snapshot.estadoSnapshot)
        assertEquals(VeredictoComercial.SALUDABLE, snapshot.veredicto)
        assertEquals(120.0, snapshot.costoTotalUsd ?: 0.0, 0.0001)
        assertEquals(480.0, snapshot.costoTotalPen ?: 0.0, 0.0001)
        assertEquals(20.0, snapshot.margenNetoPct ?: 0.0, 0.0001)
        assertNotNull(snapshot.metricasTendencia)
        assertEquals(ahora, snapshot.evaluadoEn)
    }

    @Test
    fun invoke_conEvidenciaInsuficiente_guardaSnapshotInconclusoSinMargen() = runBlocking {
        proveedorFiltroNlp.resultado = ResultadoOperacion.Exito(
            filtroBase(competidoresValidos = 2, puntajeConfianza = 0.9)
        )

        val resultado = crearUseCase().invoke(productoId = 7L)

        assertTrue(resultado is ResultadoOperacion.Exito)
        val snapshot = (resultado as ResultadoOperacion.Exito).data
        assertEquals(EstadoSnapshot.INCONCLUSO, snapshot.estadoSnapshot)
        assertEquals(VeredictoComercial.INCONCLUSO, snapshot.veredicto)
        assertNull(snapshot.margenNetoPct)
        assertNull(snapshot.metricasTendencia)
        assertEquals(1, repositorioEvaluacion.guardados.size)
    }

    @Test
    fun invoke_siMarketplaceFalla_retornaFalloYSinGuardarSnapshot() = runBlocking {
        val error = ErrorOperacion.Timeout(
            proveedor = "market-test",
            mensaje = "Tiempo de espera agotado."
        )
        proveedorMarketplace.resultado = ResultadoOperacion.Fallo(error)

        val resultado = crearUseCase().invoke(productoId = 7L)

        assertTrue(resultado is ResultadoOperacion.Fallo)
        assertEquals(error, (resultado as ResultadoOperacion.Fallo).error)
        assertTrue(repositorioEvaluacion.guardados.isEmpty())
    }

    @Test
    fun invoke_conDatosObsoletos_guardaSnapshotObsoletoYPropagaAdvertencia() = runBlocking {
        val causa = ErrorOperacion.ProveedorNoDisponible(
            proveedor = "fx-test",
            mensaje = "Se uso la ultima cotizacion cacheada."
        )
        proveedorTipoCambio.resultado = ResultadoOperacion.DatosObsoletos(
            data = cotizacionBase(),
            causa = causa
        )

        val resultado = crearUseCase().invoke(productoId = 7L)

        assertTrue(resultado is ResultadoOperacion.DatosObsoletos)
        val snapshot = (resultado as ResultadoOperacion.DatosObsoletos).data
        assertEquals(causa, resultado.causa)
        assertEquals(EstadoSnapshot.OBSOLETO, snapshot.estadoSnapshot)
        assertEquals(VeredictoComercial.SALUDABLE, snapshot.veredicto)
        assertEquals(snapshot, repositorioEvaluacion.guardados.single())
    }

    private fun crearUseCase(): EvaluarTendenciaProductoUseCase {
        return EvaluarTendenciaProductoUseCase(
            repositorioProducto = repositorioProducto,
            repositorioEvaluacion = repositorioEvaluacion,
            proveedorTipoCambio = proveedorTipoCambio,
            proveedorMarketplace = proveedorMarketplace,
            proveedorFiltroNlp = proveedorFiltroNlp,
            calculadoraLandedCost = CalculadoraLandedCost(),
            politicaEvidencia = PoliticaEvidencia(),
            motorTendencia = MotorTendenciaComercial(),
            clasificadorVeredicto = ClasificadorVeredictoComercial(),
            clock = clock
        )
    }

    private fun productoBase(): ProductoImportado {
        return ProductoImportado(
            id = 7L,
            nombre = "Consola portatil",
            queryCompetencia = "consola portatil nueva",
            componentesCosto = ComponentesCostoImportacion(
                precioFobUsd = 100.0,
                fleteUsd = 10.0,
                seguroUsd = 5.0,
                arancelesUsd = 5.0
            ),
            cantidadDisponible = 12
        )
    }

    private fun cotizacionBase(): CotizacionTipoCambio {
        return CotizacionTipoCambio(
            tasaVentaUsdPen = 4.0,
            nombreFuente = "fx-test",
            obtenidoEn = ahora
        )
    }

    private fun publicacionBase(): PublicacionMercado {
        return PublicacionMercado(
            publicacionId = "pub-1",
            titulo = "Consola portatil nueva",
            precio = 600.0,
            moneda = Moneda.PEN,
            condicion = CondicionPublicacion.NUEVO,
            permalink = null,
            nombreFuente = "market-test"
        )
    }

    private fun filtroBase(
        competidoresValidos: Int = 3,
        puntajeConfianza: Double = 0.9
    ): ResultadoFiltroNlp {
        return ResultadoFiltroNlp(
            publicacionesValidas = emptyList(),
            cantidadDescartadas = 0,
            razonesDescarte = emptyList(),
            precioPromedioRealPen = 600.0,
            competidoresValidos = competidoresValidos,
            puntajeConfianza = puntajeConfianza,
            trazaProveedor = "modelo=test"
        )
    }

    private class FakeRepositorioProductoImportado(
        private val producto: ProductoImportado?
    ) : RepositorioProductoImportado {
        override fun observarTodos(): Flow<List<ProductoImportado>> {
            return flowOf(listOfNotNull(producto))
        }

        override fun observarPorId(productoId: Long): Flow<ProductoImportado?> {
            return flowOf(producto?.takeIf { it.id == productoId })
        }

        override suspend fun obtenerPorId(productoId: Long): ProductoImportado? {
            return producto?.takeIf { it.id == productoId }
        }

        override suspend fun upsert(producto: ProductoImportado): Long {
            return producto.id
        }
    }

    private class FakeRepositorioEvaluacionComercial : RepositorioEvaluacionComercial {
        val guardados = mutableListOf<SnapshotEvaluacionComercial>()
        var historial = emptyList<SnapshotEvaluacionComercial>()

        override fun observarUltimo(productoId: Long): Flow<SnapshotEvaluacionComercial?> {
            return flowOf(guardados.lastOrNull { it.productoId == productoId })
        }

        override fun observarUltimosDeTodos(): Flow<List<SnapshotEvaluacionComercial>> {
            return flowOf(guardados)
        }

        override suspend fun obtenerHistorial(
            productoId: Long,
            limite: Int
        ): List<SnapshotEvaluacionComercial> {
            return historial
                .filter { it.productoId == productoId }
                .take(limite)
        }

        override suspend fun guardarSnapshot(snapshot: SnapshotEvaluacionComercial) {
            guardados += snapshot
        }
    }

    private class FakeProveedorTipoCambio(
        var resultado: ResultadoOperacion<CotizacionTipoCambio>
    ) : ProveedorTipoCambio {
        override val nombreProveedor: String = "fx-test"

        override suspend fun obtenerTasaVentaUsdPen(): ResultadoOperacion<CotizacionTipoCambio> {
            return resultado
        }
    }

    private class FakeProveedorMarketplace(
        var resultado: ResultadoOperacion<List<PublicacionMercado>>
    ) : ProveedorMarketplace {
        override val nombreProveedor: String = "market-test"

        override suspend fun buscar(
            query: String,
            limit: Int
        ): ResultadoOperacion<List<PublicacionMercado>> {
            return resultado
        }
    }

    private class FakeProveedorFiltroNlp(
        var resultado: ResultadoOperacion<ResultadoFiltroNlp>
    ) : ProveedorFiltroNlp {
        override val nombreProveedor: String = "nlp-test"

        override suspend fun filtrar(
            publicaciones: List<PublicacionMercado>,
            producto: ProductoImportado
        ): ResultadoOperacion<ResultadoFiltroNlp> {
            return resultado
        }
    }
}

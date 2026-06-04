package com.app.changescout.app

import android.content.Context
import androidx.room.Room
import com.app.changescout.data.api.apisnet.ApisNetTipoCambioApi
import com.app.changescout.data.api.apisnet.ApisNetTipoCambioConfig
import com.app.changescout.data.api.apisnet.ProveedorTipoCambioApisNet
import com.app.changescout.data.api.apisnet.TipoCambioCacheMemoria
import com.app.changescout.data.api.marketplace.demo.ProveedorMarketplaceDemo
import com.app.changescout.data.api.nlp.demo.ProveedorFiltroNlpDemo
import com.app.changescout.data.local.ChangeScoutDatabase
import com.app.changescout.data.local.dao.ProductoImportadoDao
import com.app.changescout.data.local.dao.EvaluacionComercialDao
import com.app.changescout.data.repository.RepositorioEvaluacionComercialRoom
import com.app.changescout.data.repository.RepositorioProductoImportadoRoom
import com.app.changescout.domain.repository.ProveedorFiltroNlp
import com.app.changescout.domain.repository.ProveedorMarketplace
import com.app.changescout.domain.repository.ProveedorTipoCambio
import com.app.changescout.domain.repository.RepositorioEvaluacionComercial
import com.app.changescout.domain.repository.RepositorioProductoImportado
import com.app.changescout.domain.rules.ClasificadorVeredictoComercial
import com.app.changescout.domain.rules.PoliticaEvidencia
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object ModulosChangeScout {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideApisNetTipoCambioApi(
        okHttpClient: OkHttpClient
    ): ApisNetTipoCambioApi {
        return Retrofit.Builder()
            .baseUrl(ApisNetTipoCambioConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApisNetTipoCambioApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProveedorTipoCambio(
        api: ApisNetTipoCambioApi,
        cache: TipoCambioCacheMemoria,
        clock: Clock
    ): ProveedorTipoCambio {
        return ProveedorTipoCambioApisNet(api, cache, clock)
    }

    @Provides
    @Singleton
    fun provideProveedorMarketplace(): ProveedorMarketplace {
        return ProveedorMarketplaceDemo()
    }

    @Provides
    @Singleton
    fun provideProveedorFiltroNlp(): ProveedorFiltroNlp {
        return ProveedorFiltroNlpDemo()
    }

    @Provides
    fun providePoliticaEvidencia(): PoliticaEvidencia {
        return PoliticaEvidencia()
    }

    @Provides
    fun provideClasificadorVeredictoComercial(): ClasificadorVeredictoComercial {
        return ClasificadorVeredictoComercial()
    }

    @Provides
    @Singleton
    fun provideChangeScoutDatabase(
        @ApplicationContext context: Context
    ): ChangeScoutDatabase {
        return Room.databaseBuilder(
            context,
            ChangeScoutDatabase::class.java,
            "changescout.db"
        ).build()
    }

    @Provides
    fun provideProductoImportadoDao(
        database: ChangeScoutDatabase
    ): ProductoImportadoDao = database.productoImportadoDao()

    @Provides
    fun provideEvaluacionComercialDao(
        database: ChangeScoutDatabase
    ): EvaluacionComercialDao = database.evaluacionComercialDao()

    @Provides
    @Singleton
    fun provideRepositorioProductoImportado(
        productoDao: ProductoImportadoDao
    ): RepositorioProductoImportado {
        return RepositorioProductoImportadoRoom(productoDao)
    }

    @Provides
    @Singleton
    fun provideRepositorioEvaluacionComercial(
        evaluacionDao: EvaluacionComercialDao
    ): RepositorioEvaluacionComercial {
        return RepositorioEvaluacionComercialRoom(evaluacionDao)
    }
}

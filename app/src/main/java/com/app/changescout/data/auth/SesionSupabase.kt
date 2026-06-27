package com.app.changescout.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.app.changescout.BuildConfig
import com.app.changescout.data.api.backend.BackendProxyConfig
import com.app.changescout.domain.model.ErrorOperacion
import com.app.changescout.domain.model.ResultadoOperacion
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {
    @POST("auth/v1/token")
    suspend fun iniciarSesion(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("grant_type") grantType: String = "password",
        @Body request: SupabaseAuthRequest
    ): SupabaseSessionResponse

    @POST("auth/v1/signup")
    suspend fun crearCuenta(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SupabaseAuthRequest
    ): SupabaseSessionResponse

    @POST("auth/v1/token")
    suspend fun refrescarSesion(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("grant_type") grantType: String = "refresh_token",
        @Body request: SupabaseRefreshRequest
    ): SupabaseSessionResponse
}

data class SupabaseAuthRequest(
    val email: String,
    val password: String
)

data class SupabaseRefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class SupabaseSessionResponse(
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    val user: SupabaseUserDto?
)

data class SupabaseUserDto(
    val id: String?,
    val email: String?
)

data class SesionUsuario(
    val userId: String,
    val email: String
)

@Singleton
class AlmacenSesion @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferencias = context.getSharedPreferences("sesion_usuario", Context.MODE_PRIVATE)

    fun obtenerToken(): String? = leer(KEY_TOKEN)

    fun obtenerSesion(): SesionUsuario? {
        val userId = usuarioIdActual()
        val email = leer(KEY_EMAIL)
        return if (userId != null && email != null && obtenerToken() != null) {
            SesionUsuario(userId = userId, email = email)
        } else {
            null
        }
    }

    fun usuarioIdActual(): String? = leer(KEY_USER_ID)

    fun obtenerRefreshToken(): String? = leer(KEY_REFRESH_TOKEN)

    fun guardar(sesion: SupabaseSessionResponse): SesionUsuario? {
        val token = sesion.accessToken?.takeIf { value -> value.isNotBlank() } ?: return null
        val refreshToken = sesion.refreshToken?.takeIf { value -> value.isNotBlank() }
            ?: obtenerRefreshToken()
            ?: return null
        val userId = sesion.user?.id?.takeIf { value -> value.isNotBlank() } ?: return null
        val email = sesion.user.email?.takeIf { value -> value.isNotBlank() } ?: return null
        preferencias.edit()
            .putString(KEY_TOKEN, token.cifrar())
            .putString(KEY_REFRESH_TOKEN, refreshToken.cifrar())
            .putString(KEY_USER_ID, userId.cifrar())
            .putString(KEY_EMAIL, email.cifrar())
            .apply()
        return SesionUsuario(userId = userId, email = email)
    }

    fun limpiar() {
        preferencias.edit().clear().apply()
    }

    private fun leer(key: String): String? {
        return preferencias.getString(key, null)
            ?.descifrar()
            ?.takeIf { value -> value.isNotBlank() }
    }

    private fun String.cifrar(): String {
        val cipher = Cipher.getInstance(CIFRADO)
        cipher.init(Cipher.ENCRYPT_MODE, llave())
        val cifrado = cipher.iv + cipher.doFinal(toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cifrado, Base64.NO_WRAP)
    }

    private fun String.descifrar(): String? {
        return runCatching {
            val data = Base64.decode(this, Base64.NO_WRAP)
            val iv = data.copyOfRange(0, IV_BYTES)
            val textoCifrado = data.copyOfRange(IV_BYTES, data.size)
            val cipher = Cipher.getInstance(CIFRADO)
            cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(textoCifrado), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun llave(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val KEY_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEYSTORE_ALIAS = "changescout_session_key"
        const val CIFRADO = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

@Singleton
class RepositorioSesionSupabase @Inject constructor(
    private val api: SupabaseAuthApi,
    private val almacenSesion: AlmacenSesion
) {
    private val mutexRefresh = Mutex()

    fun sesionActual(): SesionUsuario? = almacenSesion.obtenerSesion()

    suspend fun iniciarSesion(email: String, password: String): ResultadoOperacion<SesionUsuario> {
        return autenticar(
            email = email,
            password = password,
            mensajeSinSesion = "No se pudo iniciar sesion."
        ) { request ->
            api.iniciarSesion(
                apiKey = SupabaseAuthConfig.ANON_KEY,
                authorization = SupabaseAuthConfig.authorizationHeader(),
                request = request
            )
        }
    }

    suspend fun crearCuenta(email: String, password: String): ResultadoOperacion<SesionUsuario> {
        return autenticar(
            email = email,
            password = password,
            mensajeSinSesion = "Cuenta creada. Revisa tu correo antes de iniciar sesion."
        ) { request ->
            api.crearCuenta(
                apiKey = SupabaseAuthConfig.ANON_KEY,
                authorization = SupabaseAuthConfig.authorizationHeader(),
                request = request
            )
        }
    }

    fun cerrarSesion() {
        almacenSesion.limpiar()
    }

    suspend fun refrescarToken(): String? {
        return mutexRefresh.withLock {
            val refreshToken = almacenSesion.obtenerRefreshToken() ?: return@withLock null
            try {
                val response = api.refrescarSesion(
                    apiKey = SupabaseAuthConfig.ANON_KEY,
                    authorization = SupabaseAuthConfig.authorizationHeader(),
                    request = SupabaseRefreshRequest(refreshToken)
                )
                almacenSesion.guardar(response)
                response.accessToken
            } catch (error: CancellationException) {
                throw error
            } catch (error: RuntimeException) {
                almacenSesion.limpiar()
                null
            } catch (error: IOException) {
                null
            }
        }
    }

    private suspend fun autenticar(
        email: String,
        password: String,
        mensajeSinSesion: String,
        call: suspend (SupabaseAuthRequest) -> SupabaseSessionResponse
    ): ResultadoOperacion<SesionUsuario> {
        if (!SupabaseAuthConfig.estaConfigurado()) {
            return ResultadoOperacion.Fallo(
                ErrorOperacion.Validacion("Configura SUPABASE_URL y SUPABASE_ANON_KEY.")
            )
        }
        return try {
            val response = call(SupabaseAuthRequest(email = email.trim(), password = password))
            val sesion = almacenSesion.guardar(response)
                ?: return ResultadoOperacion.Fallo(
                    ErrorOperacion.Validacion(mensajeSinSesion)
                )
            ResultadoOperacion.Exito(sesion)
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.Validacion("Credenciales no validas o usuario no confirmado.")
            )
        } catch (error: IOException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.ProveedorNoDisponible("Supabase Auth", "No se pudo conectar con Supabase.")
            )
        } catch (error: RuntimeException) {
            ResultadoOperacion.Fallo(
                ErrorOperacion.RespuestaInvalida("Supabase Auth", "Supabase devolvio una respuesta inesperada.")
            )
        }
    }
}

class InterceptorSesionBackend @Inject constructor(
    private val almacenSesion: AlmacenSesion
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = almacenSesion.obtenerToken()
        if (token == null || !request.url.toString().startsWith(BackendProxyConfig.BASE_URL)) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}

class AutenticadorBackend @Inject constructor(
    private val repositorioSesion: RepositorioSesionSupabase
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (
            !response.request.url.toString().startsWith(BackendProxyConfig.BASE_URL) ||
            response.intentosPrevios() >= 2
        ) {
            return null
        }

        val token = runBlocking { repositorioSesion.refrescarToken() } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun Response.intentosPrevios(): Int {
        var actual: Response? = this
        var total = 1
        while (actual?.priorResponse != null) {
            total++
            actual = actual.priorResponse
        }
        return total
    }
}

object SupabaseAuthConfig {
    val BASE_URL: String = BuildConfig.SUPABASE_URL.normalizarBaseUrl()
    val ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    fun estaConfigurado(): Boolean = BuildConfig.SUPABASE_URL.isNotBlank() && ANON_KEY.isNotBlank()

    fun authorizationHeader(): String = "Bearer $ANON_KEY"
}

private fun String.normalizarBaseUrl(): String {
    val valor = trim()
    return when {
        valor.isBlank() -> "https://changescout-auth.invalid/"
        valor.endsWith("/") -> valor
        else -> "$valor/"
    }
}

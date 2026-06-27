# ChangeScout

ChangeScout es una aplicacion Android nativa para micro-importadores. Funciona como radar de tendencia comercial y simulador de costo en destino, no como e-commerce ni POS.

La app permite registrar productos importados, calcular costo total en USD/PEN, consultar tipo de cambio USD/PEN, comparar contra precios de mercado mediante el backend, filtrar resultados con NLP en Groq y guardar evaluaciones comerciales historicas para detectar riesgo de margen, saturacion y presion cambiaria.

## Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- MVVM con UDF
- StateFlow y SharedFlow
- Dagger Hilt
- Room Database
- Retrofit2
- Coroutines

## Arquitectura

El proyecto separa responsabilidades en capas:

- `ui`: pantallas Compose, navegacion y ViewModels.
- `domain`: modelos, contratos de repositorio, reglas y casos de uso.
- `data`: Room, Retrofit, proveedores backend y repositorios concretos.

Los ViewModels no calculan costos, margenes ni tendencias. Esa logica vive en casos de uso y reglas de dominio.

## Estado actual

- Tipo de cambio real con APIS.NET.
- Marketplace via backend Ktor con Apify/MercadoLibre.
- Filtro NLP via backend Ktor con Groq.
- Autenticacion con Supabase Auth y JWT validado por el backend.
- Persistencia local con Room.
- Tabla `productos_importados` para fichas base.
- Tabla `evaluaciones_comerciales` para evaluaciones historicas.
- Migraciones Room `1 -> 4`.

## Requisitos

- Android Studio reciente.
- JDK 21.
- Conexion a internet para resolver dependencias Gradle.

## Ejecutar

Agrega tus valores publicos de Supabase y backend en `local.properties`:

```properties
MARKETPLACE_BACKEND_URL=https://changescout.sytes.net/
SUPABASE_URL=https://PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=tu_anon_key
```

```bash
./gradlew :app:assembleDebug
```

En Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Pruebas

```bash
./gradlew :app:testDebugUnitTest
```

En Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Notas

Los campos de envio internacional, seguro, impuestos de importacion y gastos adicionales son opcionales. Si el usuario los deja vacios, el sistema los interpreta como `0.0` para el calculo del costo total.

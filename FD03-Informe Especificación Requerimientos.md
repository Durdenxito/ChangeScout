# FD03 - Informe de Especificacion de Requerimientos

## Proyecto

**Sistema de Analisis Competitivo y Rentabilidad de Inventario: ChangeScout**

**Curso:** Soluciones Moviles I
**Institucion:** Universidad Privada de Tacna
**Escuela:** Ingenieria de Sistemas
**Integrantes:** Loyola Vilca, Renzo Fernando; Laqui Luyo, Enzo Lionel
**Version:** 2.0
**Fecha:** 2026

## Control de versiones

| Version | Responsable | Fecha | Motivo |
| --- | --- | --- | --- |
| 1.0 | Equipo del proyecto | 18/04/2026 | Version inicial del documento |
| 2.0 | Equipo del proyecto | 04/06/2026 | Alineacion con la implementacion actual de ChangeScout |

## Indice

1. Introduccion
2. Generalidades
3. Visionamiento del producto
4. Alcance del proyecto
5. Estado de implementacion actual
6. Especificacion de requerimientos
7. Reglas de negocio
8. Modelo logico
9. Modelo de persistencia
10. Conclusiones
11. Recomendaciones

# 1. Introduccion

Este documento presenta la Especificacion de Requerimientos de Software
para **ChangeScout**, una aplicacion movil Android nativa desarrollada en
Kotlin.

ChangeScout esta dirigida a micro-importadores que compran productos en
dolares, asumen costos logisticos fragmentados y compiten localmente en
soles. Su objetivo es apoyar decisiones de compra, pausa, repricing o
liquidacion mediante una lectura temprana del riesgo comercial.

La aplicacion no es un e-commerce, no es un POS y no emite comprobantes.
Funciona como un radar de tendencia comercial y simulador de costo en
destino, tambien llamado landed cost.

# 2. Generalidades

## 2.1 Nombre del sistema

ChangeScout.

## 2.2 Tipo de producto

Aplicacion movil B2C nativa para Android.

## 2.3 Publico objetivo

Micro-importadores, vendedores online y pequenos comerciantes que compran
mercaderia en USD y venden en PEN.

## 2.4 Proposito

Automatizar la evaluacion temprana del riesgo comercial de un producto
importado, cruzando:

- costo de adquisicion en USD,
- tipo de cambio USD/PEN,
- precio promedio real del mercado local,
- volumen de competidores validos,
- historial de evaluaciones comerciales.

# 3. Visionamiento del producto

## 3.1 Descripcion del problema

Los micro-importadores suelen calcular su rentabilidad de forma manual.
Esto genera errores cuando los costos estan fragmentados entre precio del
producto, envio, seguro, impuestos y gastos adicionales.

Ademas, el precio local de referencia suele estar contaminado por
publicaciones no comparables: productos usados, replicas, accesorios,
repuestos, fundas o combos que distorsionan el promedio real.

La rentabilidad tampoco depende solo del margen del dia. Puede deteriorarse
por presion cambiaria, caida historica del precio local o saturacion del
mercado. Sin una lectura historica, el usuario puede reaccionar tarde y
liquidar cuando el margen ya se rompio.

## 3.2 Objetivo general

Implementar una aplicacion movil que permita registrar productos
importados, calcular su costo total en destino, consultar datos externos,
filtrar ruido del mercado y emitir un veredicto comercial basado en margen
y tendencia historica.

## 3.3 Objetivos especificos

1. Registrar fichas base de productos importados en una base local.
2. Calcular costo total de adquisicion en USD y PEN.
3. Consultar tipo de cambio USD/PEN mediante un proveedor externo.
4. Obtener publicaciones de mercado mediante un proveedor intercambiable.
5. Filtrar publicaciones no comparables usando un proveedor NLP.
6. Guardar evaluaciones comerciales historicas en Room.
7. Comparar la evaluacion actual contra el historial del producto.
8. Clasificar el estado comercial en saludable, precaucion, alerta,
   liquidacion o inconcluso.
9. Renderizar estados offline-first cuando falle un proveedor externo.

# 4. Alcance del proyecto

## 4.1 Incluye

- Aplicacion Android nativa en Kotlin.
- UI con Jetpack Compose.
- Navegacion con Navigation Compose.
- Arquitectura MVVM orientada a eventos bajo UDF.
- Casos de uso para logica de dominio.
- Inyeccion de dependencias con Dagger Hilt.
- Persistencia local con Room.
- Consumo de red con Retrofit2.
- Corrutinas para asincronismo.
- StateFlow para estado de UI.
- SharedFlow para efectos o eventos de una sola vez.
- Consulta real de tipo de cambio USD/PEN.
- Proveedores demo para marketplace y NLP.
- Evaluaciones comerciales historicas para auditoria.

## 4.2 No incluye

- Carrito de compras.
- Pasarela de pago.
- Emision de comprobantes.
- Gestion de ventas POS.
- Sincronizacion cloud del inventario.
- Backend propio obligatorio en la version demo.
- Persistencia de feeds vivos de mercado o tipo de cambio.

# 5. Estado de implementacion actual

## 5.1 Estado tecnico

- Proyecto Android: ChangeScout.
- Lenguaje: Kotlin.
- UI: Jetpack Compose.
- Navegacion: Navigation Compose.
- Arquitectura: MVVM con UDF.
- DI: Dagger Hilt.
- Persistencia: Room Database.
- Red: Retrofit2.
- Async: Coroutines.
- Estado: StateFlow y SharedFlow.
- Soporte para `java.time` en minSdk 24 mediante core library desugaring.

## 5.2 Proveedores actuales

| Area | Estado actual | Objetivo |
| --- | --- | --- |
| Tipo de cambio | APIS.NET real | Mantener proveedor reemplazable |
| Marketplace | Backend Ktor con Apify/MercadoLibre | Mantener proveedor reemplazable |
| NLP | Backend Ktor con Groq | Mantener proveedor reemplazable |

## 5.3 Persistencia actual

Room usa version 2 y contiene:

- `productos_importados`
- `evaluaciones_comerciales`

Existe una migracion `1 -> 2` para limpiar nombres fisicos heredados de la
nomenclatura anterior basada en snapshots.

# 6. Especificacion de requerimientos

## 6.1 Requerimientos funcionales

| ID | Requerimiento | Descripcion | Prioridad |
| --- | --- | --- | --- |
| RF-01 | Registrar producto importado | El sistema debe permitir guardar una ficha base con ID unico, nombre, query de competencia, cantidad disponible, precio del producto en origen USD, envio internacional USD opcional, seguro USD opcional, impuestos de importacion USD opcionales, gastos adicionales USD opcionales y notas opcionales. | Alta |
| RF-02 | Calcular costo fragmentado | El sistema debe calcular el costo total USD desde los componentes locales del producto. El precio en origen es obligatorio; los demas componentes son opcionales y se interpretan como 0.0 si estan vacios. | Alta |
| RF-03 | Iniciar evaluacion bajo demanda | El usuario debe poder iniciar manualmente una evaluacion comercial de un producto registrado. | Alta |
| RF-04 | Consultar tipo de cambio | Durante la evaluacion, el sistema debe consultar un proveedor externo para obtener tipo de cambio USD/PEN de venta. | Alta |
| RF-05 | Consultar competencia de mercado | El sistema debe consultar un proveedor de marketplace usando el query del producto para obtener publicaciones candidatas con titulo, precio, moneda, condicion, permalink e identificador externo. | Alta |
| RF-06 | Filtrar publicaciones con NLP | El sistema debe filtrar publicaciones candidatas mediante un proveedor NLP, descartando usados, replicas, accesorios, fundas, repuestos, combos no comparables y publicaciones ambiguas. | Alta |
| RF-08 | Calcular landed cost | El sistema debe calcular costo total USD, costo total PEN y margen neto porcentual contra el precio promedio real PEN. | Alta |
| RF-09 | Evaluar tendencia comercial | El sistema debe comparar la evaluacion actual contra evaluaciones comerciales historicas del mismo producto para estimar erosion de precio local, variacion de competidores y presion cambiaria. | Alta |
| RF-10 | Emitir veredicto comercial | El sistema debe emitir un veredicto despues de calcular el landed cost, validar la evidencia disponible y evaluar la tendencia historica del producto. El veredicto puede ser Saludable, Precaucion, Alerta temprana de quiebre, Liquidacion o Inconcluso. Si la evidencia no alcanza el minimo requerido, el veredicto debe ser Inconcluso. | Alta |
| RF-11 | Guardar evaluacion comercial historica | El sistema debe guardar en Room una evaluacion comercial historica e inmutable cuando la evaluacion bajo demanda termine con datos derivados validos, incluso si el resultado es inconcluso por falta de evidencia. Debe persistir producto ID, fecha, costo total USD, costo total PEN, tipo de cambio usado, precio promedio real PEN, competidores validos, margen neto, metricas de tendencia, veredicto, estado de evaluacion, version del algoritmo y traza resumida de proveedores. | Alta |
| RF-13 | Mostrar ultimo estado offline-first | Si falla un proveedor externo, la UI debe mostrar la ultima evaluacion valida si existe e indicar que no fue actualizada o que puede estar obsoleta. Si no existe evaluacion previa, debe mostrar un estado fallido recuperable. | Alta |

## 6.2 Requerimientos no funcionales

| ID | Atributo | Descripcion | Prioridad |
| --- | --- | --- | --- |
| RNF-01 | Arquitectura por capas | La aplicacion debe separar UI, dominio y data. Los ViewModels no deben contener logica financiera ni reglas de tendencia. | Critica |
| RNF-02 | Asincronismo | Las operaciones de red y base de datos deben ejecutarse con Coroutines sin bloquear la UI. | Critica |
| RNF-03 | Estado reactivo | La UI debe exponerse mediante StateFlow y los eventos unicos mediante SharedFlow. | Alta |
| RNF-04 | Seguridad de secretos | Las API keys no deben quedar expuestas en codigo fuente, logs ni UI. | Critica |
| RNF-05 | Tolerancia a fallos | Timeouts, errores HTTP, respuestas invalidas y limites de cuota deben mapearse a errores de dominio controlados. | Alta |
| RNF-06 | JSON estructurado | La integracion real con el proveedor NLP debe validar estructura y tipos antes de usar los datos en dominio. | Alta |
| RNF-07 | Offline-first | La app debe poder renderizar datos locales aun cuando falle la red. | Alta |
| RNF-08 | Eficiencia | Las consultas historicas deben usar ventanas acotadas y no todo el historial innecesariamente. | Media |
| RNF-09 | Trazabilidad | Cada evaluacion debe conservar fecha, proveedor, version de algoritmo y estado de vigencia. | Alta |
| RNF-10 | Reemplazabilidad | Los proveedores externos deben depender de interfaces de dominio para permitir reemplazar demos por APIs reales. | Alta |

# 7. Reglas de negocio

| ID | Regla | Descripcion |
| --- | --- | --- |
| RN-01 | Costo total USD | `CostoTotalUsd = PrecioOrigenUsd + EnvioInternacionalUsd + SeguroUsd + ImpuestosImportacionUsd + GastosAdicionalesUsd`. |
| RN-02 | Costos opcionales | Envio, seguro, impuestos y gastos adicionales se consideran `0.0` si el usuario los deja vacios. |
| RN-03 | Costo total PEN | `CostoTotalPen = CostoTotalUsd * TipoCambioVentaUsdPen`. |
| RN-04 | Margen neto | `MargenNetoPct = ((PrecioPromedioRealPen - CostoTotalPen) / PrecioPromedioRealPen) * 100`. |
| RN-05 | Evaluacion inconclusa | Si el precio promedio real es nulo, cero o no hay evidencia suficiente, la evaluacion debe ser inconclusa. |
| RN-06 | Evidencia minima | Una evaluacion requiere al menos 3 competidores validos y un puntaje de confianza aceptable cuando exista. |
| RN-07 | Erosion de precio local | Mide la variacion porcentual del precio promedio real actual frente a evaluaciones historicas comparables. |
| RN-08 | Saturacion de mercado | Se estima por el incremento de competidores validos frente al historial reciente. |
| RN-09 | Presion cambiaria | Mide el efecto del movimiento USD/PEN sobre el costo en soles entre evaluaciones. |
| RN-10 | Veredicto por tendencia | Un margen positivo no garantiza estado saludable si la tendencia muestra deterioro acelerado. |
| RN-11 | Vigencia | Una evaluacion comercial se considera vigente por una ventana configurable, por defecto 12 horas. |
| RN-12 | Versionado | Toda evaluacion debe guardar la version del algoritmo usado para permitir auditoria posterior. |

# 8. Modelo logico

## 8.1 Actor principal

**Usuario micro-importador**

Persona que registra productos, revisa el radar, abre detalles y solicita
evaluaciones para decidir si compra, pausa, repricing o liquida un
producto.

## 8.2 Objetos de frontera

| Objeto | Responsabilidad |
| --- | --- |
| PantallaRadarProductos | Muestra productos registrados, margen, veredicto y estado de evaluacion. |
| PantallaFormularioProducto | Captura datos base del producto y componentes de costo. |
| PantallaDetalleProducto | Muestra detalle del producto, ultima evaluacion y accion para reevaluar. |
| NavegacionChangeScout | Coordina destinos Compose sin exponer reglas de negocio. |

## 8.3 Objetos de control

| Objeto | Responsabilidad |
| --- | --- |
| ViewModelRadarProductos | Recibe eventos del radar y orquesta casos de uso. |
| ViewModelFormularioProducto | Gestiona estado del formulario y delega guardado al dominio. |
| ViewModelDetalleProducto | Observa producto, ultima evaluacion y dispara reevaluacion. |
| GuardarProductoImportadoUseCase | Valida y guarda fichas base. |
| ObservarRadarProductosUseCase | Combina productos con su ultima evaluacion comercial. |
| ObservarDetalleProductoUseCase | Obtiene el producto seleccionado. |
| ObservarUltimaEvaluacionUseCase | Obtiene la ultima evaluacion historica del producto. |
| EvaluarTendenciaProductoUseCase | Orquesta el pipeline completo de evaluacion. |
| CalculadoraLandedCost | Calcula costo total USD, costo total PEN y margen. |
| MotorTendenciaComercial | Calcula metricas historicas de tendencia. |
| ClasificadorVeredictoComercial | Convierte margen y tendencia en veredicto. |
| PoliticaEvidencia | Determina si existe evidencia suficiente. |
| PoliticaObsolescenciaEvaluacion | Determina vigencia u obsolescencia de una evaluacion. |

## 8.4 Objetos de entidad

| Objeto | Descripcion |
| --- | --- |
| ProductoImportado | Ficha estable de un producto evaluable. |
| ComponentesCostoImportacion | Desglose de costos locales en USD. |
| EvaluacionComercial | Resultado historico resumido e inmutable. |
| MetricasTendencia | Erosion de precio, variacion de competidores, presion cambiaria y ventana historica. |
| CotizacionTipoCambio | Tipo de cambio usado durante una evaluacion. |
| PublicacionMercado | Publicacion cruda transitoria obtenida del proveedor de mercado. |
| PublicacionComparable | Publicacion validada como comparable. |
| ResultadoFiltroNlp | Resultado estructurado del filtro semantico. |

## 8.5 Casos de uso principales

1. Registrar producto importado.
2. Ver radar de productos.
3. Ver detalle de producto.
4. Solicitar evaluacion comercial.
5. Consultar tipo de cambio.
6. Consultar publicaciones de mercado.
7. Filtrar publicaciones con NLP.
8. Calcular landed cost.
9. Clasificar veredicto comercial.
10. Guardar evaluacion comercial historica.
11. Mostrar ultimo resultado cuando falla la red.

# 9. Modelo de persistencia

## 9.1 Tablas Room

| Tabla | Proposito |
| --- | --- |
| productos_importados | Guarda fichas base de productos. |
| evaluaciones_comerciales | Guarda evaluaciones historicas resumidas. |

## 9.2 Datos que se guardan

- ficha del producto importado,
- componentes locales de costo,
- evaluaciones comerciales historicas,
- costo total USD,
- costo total PEN,
- tipo de cambio usado dentro de la evaluacion,
- precio promedio real PEN,
- competidores validos,
- margen neto,
- metricas derivadas,
- veredicto,
- estado de evaluacion,
- version del algoritmo,
- trazas resumidas de proveedores.

## 9.3 Datos que no se guardan

- feed vivo de tipo de cambio,
- listados crudos completos de marketplace,
- prompts enviados al proveedor NLP,
- payloads completos de proveedores,
- respuestas completas del proveedor NLP,
- datos temporales de red como entidades permanentes.

## 9.4 Migracion de base de datos

La base de datos actual usa version 2. La migracion `1 -> 2` renombra el
modelo fisico anterior hacia la tabla `evaluaciones_comerciales` y las
columnas `evaluacionId` y `estadoEvaluacion`.

# 10. Arquitectura del sistema

## 10.1 Capas

| Capa | Contenido |
| --- | --- |
| UI | Pantallas Compose, navegacion y ViewModels. |
| Domain | Modelos, reglas, interfaces de repositorio y casos de uso. |
| Data | Room, Retrofit, proveedores demo y repositorios concretos. |

## 10.2 Flujo de dependencia

Los ViewModels dependen de casos de uso. Los casos de uso dependen de
interfaces de dominio. Las implementaciones concretas viven en data y son
inyectadas con Hilt.

Ejemplo:

```text
ViewModelDetalleProducto
  -> EvaluarTendenciaProductoUseCase
    -> RepositorioProductoImportado
    -> RepositorioEvaluacionComercial
    -> ProveedorTipoCambio
    -> ProveedorMarketplace
    -> ProveedorFiltroNlp
```

Hilt entrega implementaciones concretas:

```text
RepositorioProductoImportado -> RepositorioProductoImportadoRoom
RepositorioEvaluacionComercial -> RepositorioEvaluacionComercialRoom
ProveedorTipoCambio -> ProveedorTipoCambioApisNet
ProveedorMarketplace -> ProveedorMarketplaceBackend
ProveedorFiltroNlp -> ProveedorFiltroNlpBackend
```

# 11. Diagramas

Los diagramas deben representar la nomenclatura actual del proyecto:

- `ProductoImportado`
- `EvaluacionComercial`
- `RepositorioEvaluacionComercial`
- `ObservarUltimaEvaluacionUseCase`
- `PoliticaObsolescenciaEvaluacion`
- `PantallaRadarProductos`
- `PantallaFormularioProducto`
- `PantallaDetalleProducto`
- `NavegacionChangeScout`

No deben usarse nombres antiguos como:

- `SnapshotEvaluacion`
- `ObservarUltimoSnapshotUseCase`
- `PoliticaVigenciaSnapshot`
- `ItemInventario`
- `PantallaListaInventario`

# 12. Conclusiones

ChangeScout propone una solucion movil para micro-importadores que
necesitan evaluar rentabilidad de productos expuestos a variacion
cambiaria y presion de mercado.

La arquitectura definida separa claramente UI, dominio y data, evitando
que los ViewModels contengan calculos financieros o reglas comerciales.
La persistencia local mediante Room permite conservar fichas base y
evaluaciones comerciales historicas sin almacenar datos volatiles como
feeds vivos de tipo de cambio o listados crudos de marketplace.

La version actual permite una primera demo funcional con tipo de cambio
real, proveedores backend para mercado/NLP, evaluacion bajo demanda,
calculo de landed cost y clasificacion de veredicto comercial.

# 13. Recomendaciones

1. Proteger claves de API mediante backend intermedio o configuracion
   segura fuera del codigo fuente.
2. Agregar pruebas de migracion Room para validar la migracion `1 -> 2`.
3. Mejorar la pantalla de detalle para mostrar trazabilidad completa de la
   evaluacion.
4. Mantener los proveedores externos desacoplados para permitir cambiar
   APIs sin tocar casos de uso ni ViewModels.

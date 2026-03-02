# SXT Template

Plantilla de proyecto para pruebas automatizadas de integración con SICARX. Incluye ejemplos completos de pruebas para operaciones de caja, ventas, compras, cotizaciones, órdenes, pagos y más.

## 📋 Contenido del Proyecto

Este proyecto incluye:

- **Tests de Integración Completos**: Pruebas automatizadas para validar operaciones de caja, ventas, compras, cotizaciones, órdenes y pagos
- **Configuración Base**: Clase `RequestInitializer` con inicialización centralizada de requests
- **Ejemplos Prácticos**: Ejemplos de pruebas en `simpleExampleTests.java`
- **DTOs de Prueba**: Modelos de datos para interactuar con toda la API de SICARX
- **Recursos de Prueba**: Archivos JSON y CSV para casos de prueba parametrizados
- **Configuración Gradle**: Sistema de construcción con soporte para JUnit 5 y Spring

## 🚀 Primeros Pasos

### 1. Crear archivo de configuración

Antes de ejecutar el proyecto localmente, debes crear un archivo `environment.json` en la raíz del proyecto con el siguiente contenido:

```json
{
  "user": "user",
  "hash": "hash",
  "environment": "DEVELOPMENT",
  "ownerName": "ownerName"
}
```

**⚠️ Importante**: Asegúrate de actualizar los valores de `user`, `hash` y `ownerName` con tus datos reales según tu entorno.

### 2. Compilar el proyecto

```bash
./gradlew clean build
```

### 3. Ejecutar las pruebas

```bash
./gradlew test
```

## 📁 Estructura del Proyecto

```
sxt-template/
├── src/
│   └── test/
│       ├── java/
│       │   ├── example/
│       │   │   └── simpleExampleTests.java        # Ejemplos de pruebas
│       │   └── setup/
│       │       └── RequestInitializer.java        # Inicializador de requests (base)
│       └── resources/
│           ├── cashTransaction.json               # Datos de transacciones de caja
│           ├── refundCases.csv                    # Casos parametrizados de reembolsos
│           └── json/                              # Datos para documentos
│               ├── RecepcionPago.json
│               ├── clientPaymentMinimum.json
│               ├── okPurchases.json
│               ├── okQuotation.json
│               ├── okSale.json
│               ├── purchaseProduction.json
│               ├── quotationProduction.json
│               ├── simplePurchaseMinimumFields.json
│               ├── simpleSaleMinimumFields.json
│               └── userCreate.json
├── environment.json                         # ⚠️ Requerido (crear localmente)
├── build.gradle.kts                         # Configuración Gradle
└── README.md                                # Este archivo
```

## 🧪 Tipos de Pruebas Disponibles

El proyecto incluye ejemplos para pruebas de:

- **Transacciones de Caja** (`test1()`, `test2()`): Obtención de lista, registro de transacciones
- **Balances de Caja** (`test3()`, `test4()`): Consulta de balance de cajas
- **Documentos**: Ventas, Compras, Cotizaciones, Órdenes, Pagos
- **Cancelaciones y Reembolsos**: Pruebas parametrizadas con datos CSV

## 🏗️ Arquitectura de Tests

### RequestInitializer
Clase base que proporciona:
- Inicialización de todas las requests necesarias
- Acceso a configuraciones de prueba (`TestSettings`, `TestEnvironment`)
- Métodos getter para todos los tipos de requests
- Setup centralizado antes de cada prueba (`@BeforeEach`)

### simpleExampleTests
Ejemplos prácticos que extienden `RequestInitializer` y demuestran:
- Cómo ejecutar requests
- Validación de respuestas HTTP
- Pruebas parametrizadas
- Manejo de DTOs

## 🛠️ Requisitos

- **Java 21** o superior
- **Gradle 7.0** o superior
- **Acceso a API SICARX** (configurado en `environment.json`)

## 📦 Dependencias Principales

- **JUnit 5** (Jupiter) - Framework de testing
- **Spring** - Utilidades HTTP y configuración
- **SICARX Test Requests** - Cliente de pruebas para API SICARX
- **Jackson** - Serialización JSON
- **SLF4J** - Logging

## 🚀 Uso Rápido

1. **Crear el archivo `environment.json`** en la raíz con tus credenciales
2. **Ejecutar todas las pruebas**:
   ```bash
   ./gradlew test
   ```
3. **Ejecutar una prueba específica**:
   ```bash
   ./gradlew test --tests simpleExampleTests.test1
   ```

## 📝 Notas Importantes

- El archivo `environment.json` es **obligatorio** y no debe committearse (incluido en `.gitignore`)
- Todas las pruebas extienden de `RequestInitializer` para acceder a la infraestructura común
- Los archivos JSON en `resources/json/` se usan para cargar datos de prueba
- El archivo CSV `refundCases.csv` se usa para pruebas parametrizadas

## ℹ️ Información Adicional

Este es un proyecto plantilla que demuestra los patrones y mejores prácticas para testing automatizado con SICARX. Utiliza la configuración centralizada en `environment.json` para conectarse a los servicios en modo desarrollo.

Para más información sobre SICARX, consulta la documentación oficial.

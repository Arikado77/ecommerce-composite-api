# E-Commerce Gateway & Dashboard – Composite API

Proyecto académico de microservicios con Java 17 y Spring Boot. Expone catálogo e inventario por separado y agrega sus datos con una tasa USD/MXN actual obtenida de Frankfurter.

## Objetivo y patrón Composite API

El gateway ofrece al consumidor una vista consolidada del producto mediante una única solicitud. En paralelo consulta el catálogo, el inventario y Frankfurter; valida las tres respuestas y compone el JSON final. Así, el cliente evita coordinar servicios y convertir divisas por su cuenta. El gateway no reemplaza ni modifica las APIs de catálogo e inventario.

## Arquitectura

| Servicio | Puerto | Responsabilidad |
|---|---:|---|
| `catalog-service` | 8081 | Productos en H2; `id`, `name`, `description`, `category`, `priceUsd`. |
| `inventory-service` | 8082 | Existencias en H2; `productId`, `availableUnits`, ubicación, entrega y estado. |
| `composite-gateway` | 8083 | Consulta los servicios anteriores y Frankfurter de forma concurrente y devuelve el resumen. |
| Frankfurter (externo) | HTTPS | Tasa de cambio diaria de USD a MXN. |

### Tecnologías

- Java 17 y Spring Boot 4.1.2.
- Maven Wrapper independiente por microservicio.
- Spring Web MVC y `RestClient`.
- `CompletableFuture` y un `ExecutorService` administrado por Spring.
- Spring Data JPA y H2 en los servicios existentes.
- `BigDecimal` con redondeo `HALF_UP` a dos decimales para MXN.

## Endpoints

| Método | URL | Servicio |
|---|---|---|
| `GET` | `http://localhost:8081/microservices/products/{id}` | Consultar producto |
| `POST` | `http://localhost:8081/microservices/products` | Registrar producto |
| `GET` | `http://localhost:8082/microservices/inventory/{productId}` | Consultar inventario |
| `POST` | `http://localhost:8082/microservices/inventory` | Registrar inventario |
| `GET` | `http://localhost:8083/api/v1/product-summary/{id}` | Resumen compuesto |
| `GET` | `https://api.frankfurter.dev/v1/latest?base=USD&symbols=MXN` | Tasa USD/MXN |

## Requisitos

- JDK 17 disponible en `JAVA_HOME` o en `PATH`.
- Acceso a Internet para que el gateway consulte Frankfurter.
- PowerShell o Git Bash en Windows. El Maven Wrapper descarga Maven la primera vez.

Verifica Java desde PowerShell con `java -version` o desde Git Bash con `java -version`.

## Ejecución

Abre tres terminales en la raíz del repositorio. Inicia los servicios en el orden indicado; las terminales deben permanecer abiertas.

### PowerShell

```powershell
.\catalog-service\mvnw.cmd -f .\catalog-service\pom.xml spring-boot:run
.\inventory-service\mvnw.cmd -f .\inventory-service\pom.xml spring-boot:run
.\composite-gateway\mvnw.cmd -f .\composite-gateway\pom.xml spring-boot:run
```

### Git Bash

```bash
bash ./catalog-service/mvnw -f catalog-service/pom.xml spring-boot:run
bash ./inventory-service/mvnw -f inventory-service/pom.xml spring-boot:run
bash ./composite-gateway/mvnw -f composite-gateway/pom.xml spring-boot:run
```

## Datos de demostración

Las bases H2 son en memoria (`jdbc:h2:mem:...`): al reiniciar un servicio, sus datos se pierden. En una instancia limpia, registra primero el producto para que H2 genere el ID `1`, después registra el inventario para ese mismo ID.

### PowerShell

```powershell
$product = Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/microservices/products' -ContentType 'application/json' -Body '{"name":"Laptop Gamer XYZ","description":"Laptop para gaming","category":"Computadoras","priceUsd":1200.00}'
$product | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri 'http://localhost:8082/microservices/inventory' -ContentType 'application/json' -Body '{"productId":1,"availableUnits":15,"warehouseLocation":"Chihuahua","estimatedDeliveryDays":3}'
```

### Git Bash

```bash
curl -sS -X POST http://localhost:8081/microservices/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Laptop Gamer XYZ","description":"Laptop para gaming","category":"Computadoras","priceUsd":1200.00}'
curl -sS -X POST http://localhost:8082/microservices/inventory \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"availableUnits":15,"warehouseLocation":"Chihuahua","estimatedDeliveryDays":3}'
```

No dupliques estos POST si el registro ya existe. Para un entorno no limpio, consulta primero los endpoints GET y usa el ID que devuelva catálogo también al registrar inventario.

## Conversión USD a MXN

El gateway solicita a Frankfurter `base=USD&symbols=MXN`, extrae `rates.MXN` y calcula `price_usd × exchange_rate` con `BigDecimal`, escala 2 y `RoundingMode.HALF_UP`. El valor nunca está codificado como cero ni como una tasa fija. La cotización puede cambiar entre consultas. El servicio usa timeouts configurables en `composite-gateway/src/main/resources/application.properties`.

## Ejemplo de respuestas

Catálogo:

```json
{
  "id": 1,
  "name": "Laptop Gamer XYZ",
  "description": "Laptop para gaming",
  "category": "Computadoras",
  "priceUsd": 1200.00
}
```

Inventario:

```json
{
  "productId": 1,
  "availableUnits": 15,
  "warehouseLocation": "Chihuahua",
  "estimatedDeliveryDays": 3,
  "stockStatus": "IN_STOCK"
}
```

Frankfurter:

```json
{
  "amount": 1.0,
  "base": "USD",
  "date": "2026-10-29",
  "rates": {
    "MXN": 18.7475
  }
}
```

La respuesta real de Frankfurter depende del día y la disponibilidad de la API. El ejemplo de estructura de gateway es:

```json
{
  "product_id": 1,
  "title": "Laptop Gamer XYZ",
  "price_usd": 1200.00,
  "price_mxn": 22497.00,
  "stock_status": "IN_STOCK",
  "available_units": 15,
  "currency_conversion_source": "Frankfurter External API",
  "exchange_rate": 18.7475,
  "status": "SUCCESS"
}
```

El valor de MXN anterior ilustra la multiplicación con la tasa mostrada (consulta realizada el 29 de octubre de 2026); no es un resultado persistente ni garantizado para futuras consultas.

## Manejo de errores

El gateway devuelve JSON con `status`, `message` y `service`; no expone stack traces:

- `404`: producto o inventario no encontrado.
- `503`: servicio dependiente no disponible.
- `502`: error del servicio dependiente, JSON inválido o respuesta incompleta.
- `504`: timeout de conexión/lectura.
- `400`: identificador no numérico.
- `500`: error interno no previsto.

Las llamadas a catálogo, inventario y Frankfurter se inician con `CompletableFuture.supplyAsync()` en un executor de tres hilos; `CompletableFuture.allOf()` espera a que terminen antes de combinar sus respuestas. Los timeouts de conexión y lectura son 3 y 5 segundos.

## Evidencias

Consulta [EVIDENCIAS.md](./EVIDENCIAS.md) para los comandos de PowerShell y Git Bash que reproducen las cuatro respuestas y para la comprobación de error. Las capturas deben ser reales y mostrar la salida de las consultas; este repositorio no incluye capturas simuladas.

## Estructura del repositorio

```text
catalog-service/       API de catálogo existente
inventory-service/     API de inventario existente
composite-gateway/     API compuesta nueva
README.md              Arquitectura, instalación y uso
EVIDENCIAS.md           Comandos reproducibles para las evidencias
```

## Git y ramas

La implementación se trabaja en `feature/composite-gateway`; los servicios existentes no se reescriben. Revisa los cambios antes de prepararlos:

```bash
git status --short --branch
git diff --check
git diff
git status --ignored --short
```

Los directorios `target/` y artefactos de Maven están excluidos por `.gitignore` de cada servicio. No se deben agregar `target/`, secretos ni archivos temporales. Antes de publicar, confirma que nadie haya actualizado `origin` y revisa el diff; no se ha hecho push ni merge.

```bash
git add README.md EVIDENCIAS.md composite-gateway
git diff --cached --check
git diff --cached
```

Tras revisar y confirmar el contenido, crea el commit local. Publica la rama y abre el Pull Request únicamente después de confirmar esa publicación:

```bash
git commit -m "feat: add composite product summary gateway"
git push -u origin feature/composite-gateway
```

No ejecutes `git clean`, `git reset --hard` ni merges como parte de este flujo.

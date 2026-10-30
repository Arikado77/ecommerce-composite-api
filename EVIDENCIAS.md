# Evidencias reproducibles

Las cuatro evidencias se capturan mostrando una terminal con el comando y su respuesta JSON. No se incluyen capturas simuladas ni resultados de ejecuciones que no se hayan realizado.

## Preparación

Abre tres terminales y ejecuta los servicios desde la raíz del repositorio. Usa las instrucciones completas del `README.md`. Con H2 en memoria, los registros se pierden al reiniciar cada servicio.

En una cuarta terminal, con catálogo e inventario ya iniciados, crea los datos de demostración en una base H2 recién iniciada. El identificador generado por el primer POST de catálogo será `1`:

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

Si ya hay datos en una instancia, consulta primero `GET /microservices/products/1` y `GET /microservices/inventory/1`; no repitas los POST si los registros ya existen. El inventario calcula `stockStatus` a partir de `availableUnits`.

## Evidencia 1: catálogo

### PowerShell

```powershell
Invoke-RestMethod -Uri 'http://localhost:8081/microservices/products/1' | ConvertTo-Json -Depth 5
```

### Git Bash

```bash
curl -sS http://localhost:8081/microservices/products/1
```

## Evidencia 2: inventario

### PowerShell

```powershell
Invoke-RestMethod -Uri 'http://localhost:8082/microservices/inventory/1' | ConvertTo-Json -Depth 5
```

### Git Bash

```bash
curl -sS http://localhost:8082/microservices/inventory/1
```

## Evidencia 3: tipo de cambio de Frankfurter

### PowerShell

```powershell
Invoke-RestMethod -Uri 'https://api.frankfurter.dev/v1/latest?base=USD&symbols=MXN' | ConvertTo-Json -Depth 5
```

### Git Bash

```bash
curl -sS 'https://api.frankfurter.dev/v1/latest?base=USD&symbols=MXN'
```

## Evidencia 4: respuesta consolidada

### PowerShell

```powershell
Invoke-RestMethod -Uri 'http://localhost:8083/api/v1/product-summary/1' | ConvertTo-Json -Depth 5
```

### Git Bash

```bash
curl -sS http://localhost:8083/api/v1/product-summary/1
```

La tasa y `price_mxn` son dinámicos: una captura posterior puede tener una tasa distinta. Captura cada una de las cuatro respuestas completas y conserva visible el comando y el origen de la respuesta en la terminal.

## Error controlado

Para comprobar un producto inexistente:

```powershell
try {
    Invoke-RestMethod -Uri 'http://localhost:8083/api/v1/product-summary/999999' -ErrorAction Stop
} catch {
    $_.ErrorDetails.Message
}
```

Detener temporalmente catálogo o inventario en su terminal y repetir el GET consolidado permite comprobar la respuesta JSON de indisponibilidad. No detengas procesos compartidos sin confirmar que no los está usando otra persona.

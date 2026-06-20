## To check status
``` bash
curl -v "http://localhost:8080/api/health"

```
## To Generate token

``` bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"antonio","password":"admin123","role":"ADMIN"}'
```

## Springboot

``` bash
- To run proyect
 ./mvnw spring-boot:run

- To build proyect
./mvnw clean install

```

## Para ingresar a la base de datos postgres en docker:
``` bash
docker exec -it nenesport_db psql -U nenestore -d nenesport_db
```


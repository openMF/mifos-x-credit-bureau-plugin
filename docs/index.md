# Index

## Configuration

- Java & SpringBoot

  - Java version `21`
  - SpringBoot framework `3.5.5`
- Jarkarta REST / Jersey (JAX-RS)

- BouncyCastle

- ApacheCommons

- MariaDB & Liquibase

- MapStruct

- Lombok

- H2 database and Junit testing

## APIs
`http://localhost:8080/api`
### Authentication
```
#application.properties

spring.security.user.name=tester
spring.security.user.password=tempPassword123

```
#### Option 1
Use curl
```
curl -X POST -H "Content-Type: application/json" -d '{"username": "tester", "password": "tempPassword123"}' http://localhost:8080/api/authenticate
```
#### Option 2
Access APIs after registering 

### Credit Bureau Registration

| Method | Endpoint | Description |
|---|---|---|
|`GET` |`/credit-bureaus` | Get all registered credit bureaus |
|`POST` | `/credit-bureaus`| Post a new credit bureau
|`GET` |`/credit-bureaus/{id}/configuration-keys` | Get the configuration keys for a credit bureau |

### Consuming Fineract Client

| Method | Endpoint | Description |
|---|---|---|
|`GET` | `/client/{clientId}/`| Get Client data from Fineract's API|
|`GET` |`/client/{clientId}/cdc-request`| Get the parsed request body for Circulo de Credit |

### Sending Requests to Circulo De Credito 

| Method | Endpoint | Description |
|---|---|---|
|`POST` | `/circulo-de-credito/security-test/{creditBureauId}`
|`POST` |

## Modules

| Module   | Description                   |
|----------|-------------------------------|
| `api`    | controller classes            
| `config` | configuration classes         
| `data`   | DTOs                          |
|          | `creditbureaus`               |
|          | `models`                      |
|| `registration`                |
|`domain` | entity classes                
|`exception` | exception handling classes    
|`mappers` | dto to entity mapping classes 
|`service` | business action classes       



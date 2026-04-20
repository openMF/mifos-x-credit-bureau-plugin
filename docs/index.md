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
### Base URL
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
|`POST` | `/circulo-de-credito/security-test/{creditBureauId}`| Sends a request to Circulo De Credit to verify if the keys signage method is working. (Circulo De Credit can recognize APIs signed through the SignatureService) |
|`POST` | `/circulo-de-credito/rcc-test/{creditBureauId}`| Uses a hard coded request body to send a request to Circulo De Credito to check the credit report returned by the API.|

## Modules

| Module               | Description                                                                    |
|----------------------|--------------------------------------------------------------------------------|
| `api`                | Currently, this module has 4 controller classes                                |    
| `config`             | Configurations                                                                 |   
| `data`               | Data Objects (DTOs) of the data used in the service.                           |
| `data/creditbureaus` | Data objects related to credit bureaus including request and response formats. |
| `data/fineract`      | Data objects related to fineract connection.                                   |
| `data/registration`  | Data objects related to registering a credit bureau.                           |
| `domain`             | Entities of Credit Bureaus                                                                
| `exception`          | Exception Handling                                                    
| `mappers`            | Mapping Data Transfer Objects.                                                
| `service`            | Business Logic                                                      

## Workflow

 <img width="1221" height="577" alt="Screenshot 2025-11-19 at 6 36 21 PM" src="https://gist.github.com/user-attachments/assets/9df91186-1ae8-46c8-b918-f3579da51f33" />

- The user/bank officers externally register with the Credit Bureau to use their APIs (1, 2, 3)
- The user/bank officers enter the credentials (e.g. api keys) they used to register with the Credit Bureau in the Registration Portal. (4)
- The service saves the api keys (encrypted) in the database. (5, 6)
- The bank officer loads the FICO score result on the client page.
- The service decrypts the api keys, parses a request body with client info, signs the request with the keys and sends it to the external credit bureau. (7, 8, 9, 10, 11, 12, 13)
- The service retrieves the response from the external credit bureau and parses the information into a generalized FICO score. (14)
- The user/bank officer views the FICO score on the client page.

## Postman Collection
1. Setup data in MifosX sandbox to test Credit Bureau APIs: https://documenter.getpostman.com/view/19472254/2sBXigLtC6
2. Test Credit Bureau APIs: https://documenter.getpostman.com/view/19472254/2sB3QCRskT
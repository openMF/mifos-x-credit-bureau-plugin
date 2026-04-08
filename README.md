# Mifos X Credit Bureau Plugin

## Overview
This microservice provides integration between Mifos X and credit bureau systems. It allows financial institutions using Mifos X to fetch credit reports for customers and submit credit data to credit bureaus.

## Features
- Register an external credit bureau's secret key(s) to MifosX.
- Fetch client data from MifosX/Fineract.
- Send client data to a Credit Bureau.
- Fetch credit report from a Credit Bureau.

## Prerequisites
- Java 21 or higher
- MariaDB 11.2 or higher
- Gradle 8.x

## Local Setup 

### 1. Application Configuration
The main configuration is in `src/main/resources/application.properties`. Key properties to review are:

- **Database Connection:**
```
spring.datasource.url=jdbc:mariadb://localhost:3306/creditbureau
spring.datasource.username=root
spring.datasource.password=mysql
```
View the database using the following commands:
```
docker exec -it creditbureau_mariadb mariadb -u root -pmysql

SHOW DATABASES;
USE creditbureau;
SHOW TABLES;
SELECT * FROM my_table;
```

- **Fineract API Details:**
(Points to the Fineract instance from which to pull client data)
```
mifos.fineract.api.base-url.client=https://sandbox.mifos.community/fineract-provider/api/v1/clients/
mifos.fineract.api.base-url.address=https://sandbox.mifos.community/fineract-provider/api/v1/client/
mifos.fineract.api.username=mifos
mifos.fineract.api.password=password
```
- **Credit Bureau API:**
Points to the Credit Bureau API. In this case, circulodecredito.com.mx
```
mifos.circulodecredito.base.url=https://services.circulodecredito.com.mx/
```
### 2. Environment Setup
- Generate a random encryption key on your command line.
```
openssl rand -base64 32
```
- Save the key in a `.env` file OR export the required encryption key in the command line before running the application. For example:
```
export MIFOS_SECURITY_ENCRYPTION_KEY="your-encryption-key"`
```

### 3. Build & Run
#### OPTION 1
```bash
# Build the application
./gradlew build
# Start MariaDB
docker compose -f docker-compose-mariadb.yml up
# export the encryption key
export MIFOS_SECURITY_ENCRYPTION_KEY="your-encryption-key"
# Run the application
./gradlew bootRun
```
#### OPTION 2
```bash
#export the encryption key
export MIFOS_SECURITY_ENCRYPTION_KEY="your-encryption-key"
# Run the application
docker compose -f docker-compose.yml up --build
```

## Production
```bash

docker build -t creditbureau-plugin:latest
docker run -d \
  -e MIFOS_SECURITY_ENCRYPTION_KEY=supersecretkey123 \
  -p 8080:8080 \
  --name creditbureau-prod \
  creditbureau-plugin:latest

```

## API Documentation
To test the APIs, please take a look at the Postman collection below:
- Setup new Client and Credit Bureau in MifosX
  https://documenter.getpostman.com/view/19472254/2sBXigLtC6
- Test Credit Bureau Service APIs
  https://documenter.getpostman.com/view/19472254/2sB3QCRskT

## Running Tests

```bash
./gradlew test
```

## License
This project is licensed under the Mozilla Public License Version 2.0 - see the LICENSE file for details.

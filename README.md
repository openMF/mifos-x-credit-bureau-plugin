# Mifos X Credit Bureau Plugin

## Overview
This plugin provides integration between Mifos X and credit bureau systems. It allows financial institutions using Mifos X to fetch credit reports for customers and submit credit data to credit bureaus.

## Features
- Fetch credit reports for customers from Credit Unions/Bureaus.
- Fetch clients from Fineract api.
- RESTful API for integration with Mifos X

## Prerequisites
- Java 21 or higher
- MariaDB 11.2 or higher
- Gradle 8.x

## Setup

### 1. Environment Setup
Export the required encryption key. For example:
```
export MIFOS_SECURITY_ENCRYPTION_KEY="your-encryption-key"`
```
### 2. Database Setup
The project uses MariaDB. A `docker-compose.yml` file is provided to easily start a MariaDB instance.
docker compose up -d db

### 3. Application Configuration
The main configuration is in `src/main/resources/application.properties`. Key properties to review are:

- **Database Connection:**
```
spring.datasource.url=jdbc:mariadb://localhost:3306/creditbureau
spring.datasource.username=root
spring.datasource.password=mysql
```

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
```
mifos.circulodecredito.base.url=https://services.circulodecredito.com.mx/
```

## Build
```bash
./gradlew build
```

### Run
```bash
docker compose up -d
./gradlew bootRun
```

## API Documentation

## Development

### Running Tests

```bash
./gradlew test
```

## License
This project is licensed under the Mozilla Public License Version 2.0 - see the LICENSE file for details.

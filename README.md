# Mifos X Credit Bureau Plugin

## Overview
This plugin provides integration between Mifos X and credit bureau systems. It allows financial institutions using Mifos X to fetch credit reports for customers and submit credit data to credit bureaus.

## Features
- Fetch credit reports for customers
- Submit credit data to credit bureaus
- RESTful API for integration with Mifos X

## Prerequisites
- Java 21 or higher
- MariaDB 11.2 or higher
- Gradle 8.x

## Setup

### Database Setup

#### Option 1: Using Docker Compose (Recommended)
1. Make sure you have Docker and Docker Compose installed
2. Run the following command to start the MariaDB container:
   ```bash
   docker-compose up -d
   ```
   This will start a MariaDB instance with the required database already created.

#### Option 2: Manual Setup
1. Install MariaDB 11.2 or higher
2. Create a MariaDB database named `creditbureau`:
   ```sql
   CREATE DATABASE creditbureau;
   ```
3. Configure database connection in `src/main/resources/application.properties` if needed.

### Building the Application
```bash
./gradlew build
```

### Running the Application
```bash
./gradlew bootRun
```

The application will start on port 8080 by default.

## API Endpoints

### Get Credit Report
```
GET /api/creditbureau/report/{customerId}
```

### Submit Credit Data
```
POST /api/creditbureau/submit/{customerId}
Content-Type: application/json

{
  "credit data in JSON format"
}
```

## Development

### Running Tests
```bash
./gradlew test
```

### Adding New Credit Bureau Integrations
To add support for a new credit bureau:

1. Create a new implementation of the `CreditBureauService` interface
2. Configure the new implementation as a Spring bean

## License
This project is licensed under the Mozilla Public License Version 2.0 - see the LICENSE file for details.

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

### Building the Application
```bash
./gradlew build
```

### Running the Application
```bash
docker compose up -d
./gradlew bootRun
```

The application will start on port 8080 by default.

## API Endpoints

## Development

### Running Tests
```bash
./gradlew test
```

### Adding New Credit Bureau Integrations

## License
This project is licensed under the Mozilla Public License Version 2.0 - see the LICENSE file for details.

# Contributing to mifos-x-credit-bureau-plugin

> 📖 For general project overview, architecture, and API reference,
> see the [README](README.md) and [docs](docs/) folder.
> This file focuses on local development setup only.

Thank you for your interest in contributing to the Mifos Credit Bureau
Plugin! This guide will help you get up and running locally.

---

## Prerequisites

Make sure you have the following installed:

| Tool | Version | Download |
|------|---------|----------|
| Java | 21+ | https://adoptium.net |
| Docker Desktop | Latest | https://docker.com |
| Git | Latest | https://git-scm.com |

---

## Local Setup — Step by Step

### Step 1 — Fork and Clone
```bash
git clone https://github.com/openMF/mifos-x-credit-bureau-plugin
cd mifos-x-credit-bureau-plugin
```

### Step 2 — Generate and Set Required Environment Variables

The application requires a Base64-encoded AES-256 encryption key to start.

**Generate a secure key:**

On Mac/Linux:
```bash
openssl rand -base64 32
```

On Windows PowerShell:
```powershell
[Convert]::ToBase64String(
  [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
)
```

**Set the generated key as environment variable:**

On Mac/Linux:
```bash
export MIFOS_SECURITY_ENCRYPTION_KEY=<your-generated-key>
```

On Windows PowerShell:
```powershell
$env:MIFOS_SECURITY_ENCRYPTION_KEY="<your-generated-key>"
```

> ⚠️ Never commit this key to version control.
> Never share it with anyone.
> Generate a new one for each environment.

### Step 3 — Start MariaDB with Docker
```bash
docker compose -f docker-compose-mariadb.yml up
```

Wait 30 seconds then verify it is running:
```bash
docker ps
```

You should see `creditbureau_mariadb` with status `Up`.

> ⚠️ **Port Conflict:** If you have MySQL installed locally,
> port 3306 may already be in use. Fix this by changing
> docker-compose.yml:
> ```yaml
> ports:
>   - "3307:3306"  # change from 3306:3306
> ```
> Then set `DB_PORT=3307` environment variable.

### Step 4 — Run the Application
```bash
./gradlew bootRun
```

Wait until you see:
```
Started CreditBureauApplication in XX seconds
Tomcat started on port 8080
```

This may take 5-10 minutes on first run while Gradle
downloads dependencies.

### Step 5 — Verify the App is Running

Open your browser and go to:
```
http://localhost:8080/api/credit-bureaus
```

Enter credentials when prompted:
```
username: tester
password: tempPassword123
```

You should see:
```json
[]
```

---

## Testing Endpoints

### Create a Credit Bureau

On Mac/Linux:
```bash
curl -u tester:tempPassword123 \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"creditBureauName":"Circulo de Credito","country":"Mexico","active":true,"registrationParamKeys":["apiKey","secretKey"]}' \
  http://localhost:8080/api/credit-bureaus
```

On Windows PowerShell:
```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/api/credit-bureaus" `
  -Method POST `
  -Headers @{
    Authorization = "Basic " + [Convert]::ToBase64String(
      [Text.Encoding]::ASCII.GetBytes("tester:tempPassword123")
    )
    "Content-Type" = "application/json"
  } `
  -Body '{"creditBureauName":"Circulo de Credito","country":"Mexico","active":true,"registrationParamKeys":["apiKey","secretKey"]}' `
  -UseBasicParsing
```

### Get Client CDC Request
```
GET http://localhost:8080/api/client/1/cdc-request
```

### Test CDC Security Endpoint
```
POST http://localhost:8080/api/circulo-de-credito/security-test/1
```

---

## Testing Without Real CDC Credentials

To test the CDC security endpoint locally without real
Circulo de Credito API credentials, enable mock mode:

On Windows:
```powershell
$env:CDC_MOCK_ENABLED="true"
```

On Mac/Linux:
```bash
export CDC_MOCK_ENABLED=true
```

Then restart the app. The security test endpoint will
return a mock success response.

---

## Running Tests
```bash
./gradlew test
```

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `MIFOS_SECURITY_ENCRYPTION_KEY` | ✅ Yes | None | AES-256 Base64 key for encrypting bureau credentials |
| `DB_HOST` | No | localhost | Database host |
| `DB_PORT` | No | 3306 | Database port |
| `DB_NAME` | No | creditbureau | Database name |
| `DB_USERNAME` | No | root | Database username |
| `DB_PASSWORD` | No | mysql | Database password |
| `DB_ALLOW_PUBLIC_KEY` | No | true | Allow public key retrieval |
| `DB_USE_SSL` | No | false | Use SSL for DB connection |
| `CDC_MOCK_ENABLED` | No | false | Enable mock CDC responses |
| `SERVER_PORT` | No | 8080 | Application port |

---

## Common Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| `Illegal base64 character` | Missing or invalid encryption key | Generate and set `MIFOS_SECURITY_ENCRYPTION_KEY` |
| `Port 3306 already in use` | Local MySQL running | Change docker-compose port to 3307 |
| `500 on POST /credit-bureaus` | Missing `registrationParamKeys` field | Include field in request body |
| `500 on security-test` | No CDC credentials | Enable `CDC_MOCK_ENABLED=true` |
| App hangs on startup | Gradle downloading deps | Wait 10 minutes on first run |

---

## Making a Pull Request

1. Fork the repository
2. Create a branch: `git checkout -b <new-branch-name>`
3. Make your changes
4. Run tests: `./gradlew test`
5. Commit: `git commit -m "MX-[TICKET-NUMBER] - TICKET TITLE"`
6. Push: `git push origin <new-branch-name>`
7. Open a Pull Request on GitHub

---

## Getting Help

- Join the Mifos community on Slack
- Post in **#mifos-x-dev** for technical questions

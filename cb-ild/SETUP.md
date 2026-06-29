# CB-ILD Complete Setup Guide

## Section 1 — What You Are Setting Up

This guide walks you through running the complete CB-ILD local stack: the **CB-ILD** service (Spring Boot, port 8084) backed by **cbild_db** (MariaDB, port 3307), calling the **mifos-x-credit-bureau-plugin** (port 8081) backed by **creditbureau** (MariaDB, port 3306), which routes real CDC requests through a VPN tunnel to Mexico's Círculo de Crédito sandbox API. A VPN connection is required before starting the plugin because CDC's Cloudflare WAF blocks all non-Mexican IP addresses. The default `mock.enabled=true` skips CDC entirely for local development; this guide covers both modes.

> 📝 **No VPN access?** Run CB-ILD in mock mode — no VPN, no plugin, no CDC credentials needed.
>
> ```bash
> docker start mariadb-cb
> export CB_ENC_KEY="dev-test-key-32-bytes-long-aes!!"
> ./gradlew bootRun
> # Open http://localhost:8084/swagger-ui/index.html
> ```
>
> Mock mode returns realistic fake data for all endpoints.
> Switch to real mode only when you have VPN access from your mentor.
> See Section 12 for full mock vs real comparison.

---

## Section 1b — What Is Safe to Share vs What Is Not

This guide intentionally includes some credentials and data for developer convenience.
Here is what is safe and what must never be committed:

| Item | In this guide | Safe to share? | Why |
|---|---|---|---|
| `kyc_officer / password` | ✅ Yes | ✅ Yes | Sandbox RBAC demo credentials |
| `credit_analyst / password` | ✅ Yes | ✅ Yes | Sandbox RBAC demo credentials |
| `compliance / password` | ✅ Yes | ✅ Yes | Sandbox RBAC demo credentials |
| `tester / tempPassword123` | ✅ Yes | ✅ Yes | Plugin sandbox auth — already public in openMF repo |
| `tempPassword123` | ✅ Yes | ✅ Yes | Already in openMF/mifos-x-credit-bureau-plugin public repo |
| Example IP `187.190.27.223` | ✅ Yes | ✅ Yes | Example only — changes every VPN session |
| CDC reference `386636538` | ✅ Yes | ✅ Yes | Example sandbox response value |
| `pri_key.pem` file path | ✅ Yes | ✅ Yes | Just the path — actual key content NOT included |
| `certificate.pem` file path | ✅ Yes | ✅ Yes | Just the path — actual cert content NOT included |
| VPN server / credentials | ❌ Removed | ❌ Never | Personal mentor credentials — request from Victor Romero |
| Real x-api-key value | ❌ Removed | ❌ Never | CDC API credential — obtain from mentor |
| Real `CB_ENC_KEY` value | ❌ Removed | ❌ Never | AES-256 encryption key |
| Real `MIFOS_SECURITY_ENCRYPTION_KEY` | ❌ Removed | ❌ Never | Plugin AES encryption key |
| Actual `pri_key.pem` content | ❌ Never | ❌ Never | Your ECDSA private key — never commit |
| Actual `certificate.pem` content | ❌ Never | ❌ Never | Your CDC certificate — never commit |

> 📝 **For new contributors:** Contact Victor Romero ([@IOhacker](https://github.com/IOhacker))
> to obtain: VPN credentials, CDC x-api-key, and ECDSA certificate approval.

---

## Section 2 — Prerequisites (~2 minutes)

| Tool | Version | Check command | Install |
|------|---------|---------------|---------|
| Java | 21 | `java -version` | [Adoptium](https://adoptium.net) |
| Docker Desktop | 4.x+ | `docker --version` | [Docker](https://docs.docker.com/get-docker/) |
| OpenVPN Connect | 3.x+ | `openvpn --version` | [OpenVPN](https://openvpn.net/client/) |
| Git | 2.x+ | `git --version` | [Git](https://git-scm.com) |
| Gradle | (bundled) | `./gradlew --version` | Included in repo |

---

## Section 3 — Port Reference

| Service | Port | Container / Process | Health check |
|---------|------|---------------------|--------------|
| CB-ILD | 8084 | `./gradlew bootRun` | `curl -s http://localhost:8084/actuator/health` |
| Plugin | 8081 | `creditbureau-plugin` (Docker) | `curl -s -u tester:tempPassword123 http://localhost:8081/credit-bureaus` |
| cbild_db | 3307 | `mariadb-cb` (Docker) | `docker exec mariadb-cb healthcheck.sh --connect` |
| creditbureau db | 3306 | `creditbureau_mariadb` (Docker) | `docker exec creditbureau_mariadb healthcheck.sh --connect` |
| CDC sandbox | 443 | `services.circulodecredito.com.mx` | Requires VPN |

---

## Section 4 — VPN Setup (~5 minutes) ⚠️ CRITICAL — DO THIS FIRST

CDC's sandbox is hosted in Mexico. Cloudflare blocks all non-Mexican IPs.

> ⚠️ **WARNING**: Start the plugin ONLY after the VPN is connected. The plugin registers CDC credentials on startup; a failed connection means the credit bureau is never configured and bureau ID will be wrong.

**VPN profile settings:**

```
proto:  tcp-client
remote: ${VPN_SERVER}       # provided by your mentor
username: ${VPN_USERNAME}   # provided by your mentor
password: ${VPN_PASSWORD}   # provided by your mentor
```

**Steps:**
1. Open OpenVPN Connect → Import profile → enter the settings above
2. Click Connect
3. Verify Mexican IP:

```bash
curl -s https://api.ipify.org
# Expected: Mexican IP (e.g. 187.190.27.223)
```

If the returned IP is not in Mexico, the CDC call will silently fail with `CDC_TIMEOUT`.

---

## Section 5 — CB-ILD Database (~1 minute)

**Scenario A — Container exists from a previous session:**
```bash
docker start mariadb-cb
```

**Scenario B — First time (or after `docker compose down`):**
```bash
cd ~/path/to/cb-ild
docker compose up -d
```

**Verify:**
```bash
docker ps | grep mariadb-cb
# Expected: mariadb-cb ... Up ... 0.0.0.0:3307->3306/tcp
```

> 📝 NOTE: `docker compose up -d` only starts `mariadb-cb`. CB-ILD itself is run with `./gradlew bootRun`.

---

## Section 6 — Plugin Setup (~3 minutes)

> ⚠️ **WARNING**: VPN must be connected before this step.

```bash
cd ~/path/to/mifos-x-credit-bureau-plugin
export MIFOS_SECURITY_ENCRYPTION_KEY="${YOUR_MIFOS_ENC_KEY}"
docker compose up -d
```

Wait approximately 30 seconds for the plugin to finish starting, then verify:

```bash
curl -s -u tester:tempPassword123 http://localhost:8081/credit-bureaus | python3 -m json.tool
```

Expected (empty list = first time):
```json
[]
```
Expected (after credential registration):
```json
[{"id":13,"name":"Circulo De Credito","country":"Mexico","isActive":true}]
```

---

## Section 7 — CDC Credential Registration (~5 minutes, first time only)

> 📝 NOTE: This step is required the **first time** OR after `docker compose down` (because the plugin database is wiped and the bureau ID increments). The current known bureau ID is **13** as of the June 2026 sandbox session.

```bash
# Step 1 — Create the credit bureau record
BUREAU_RESPONSE=$(curl -s -u tester:tempPassword123 -X POST http://localhost:8081/credit-bureaus \
  -H "Content-Type: application/json" \
  -d '{"creditBureauName":"Circulo De Credito","country":"Mexico","isActive":true,"isAvailable":true,"registrationParamKeys":["x-api-key","private_key"]}')
echo "Bureau created: $BUREAU_RESPONSE"

# Step 2 — Extract the bureau ID (increment from previous runs)
BUREAU_ID=$(echo "$BUREAU_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['resourceId'])")
echo "Bureau ID: $BUREAU_ID"

# Step 3 — Extract private key as hex from PEM
PRIVATE_KEY_HEX=$(openssl ec -in ~/Downloads/CB/pri_key.pem -noout -text 2>&1 \
  | awk '/priv:/{p=1;next} /pub:/{p=0} p{printf $0}' \
  | tr -d ' \n:' | tr 'A-F' 'a-f')

# Step 4 — Register x-api-key and private_key with the plugin
curl -s -u tester:tempPassword123 -X PUT \
  "http://localhost:8081/credit-bureaus/${BUREAU_ID}/configuration" \
  -H "Content-Type: application/json" \
  -d "{\"registrationParams\":{\"x-api-key\":\"${CDC_X_API_KEY}\",\"private_key\":\"$PRIVATE_KEY_HEX\"}}"

# Step 5 — Update application.properties with the new bureau ID
sed -i '' "s/cbild.plugin.credit-bureau-id=.*/cbild.plugin.credit-bureau-id=${BUREAU_ID}/" \
  src/main/resources/application.properties

echo "Registration complete. Bureau ID ${BUREAU_ID} set in application.properties"
```

> ⚠️ **WARNING**: `${CDC_X_API_KEY}` must be set in your shell environment before running this script. Do not commit the real value. Obtain it from mentors.

---

## Section 8 — CB-ILD Startup (~2 minutes)

```bash
cd ~/path/to/cb-ild
export CB_ENC_KEY="${YOUR_CB_ENC_KEY}"
./gradlew bootRun
```

**What to look for in the startup log:**

| Log line | Meaning |
|----------|---------|
| `Started CbIldApplication` | CB-ILD is ready on port 8084 |
| `mockEnabled: true` | Mock mode — no VPN or plugin needed |
| `mockEnabled: false` | Real CDC mode — VPN + plugin required |
| `Running Changeset` | Liquibase migration applying (normal on first run) |
| `CB_ENC_KEY warning` | Key is not 32 bytes — AES-256 will fail in Phase 2 |

Startup takes approximately 2 seconds after Liquibase finishes.

---

## Section 9 — Swagger UI (~2 minutes)

Open: **http://localhost:8084/swagger-ui/index.html**

**How to authenticate:**
1. Click **Authorize** (top right, lock icon)
2. Enter username and password from the table below
3. Click **Authorize** → click **Close**
4. Lock icon closed = authenticated

| Role | Username | Password |
|------|----------|----------|
| KYC Officer | `kyc_officer` | `password` |
| Credit Analyst | `credit_analyst` | `password` |
| Compliance | `compliance` | `password` |

**Swagger sections:**
1. **1. Bureau Readiness** — 3 endpoints (bureau-readiness, bureau-response, audit-trail)
2. **2. Submission Pipeline** — 5 endpoints (run, history, schedule, report-screening, report-approval)
3. **3. Dispute Management** — 3 endpoints (POST, PUT status, GET by ID)

---

## Section 9b — Endpoint Testing Guide (Swagger UI)

Test all 11 endpoints in this exact order. Each step tells you which role to use,
what values to enter, and what response to expect.

> 📝 **Before starting:** Make sure CB-ILD is running and you have authorized in Swagger.
> If Fineract (mifos-bank-1) is down, bureau-readiness will return FINERACT_SERVER_ERROR —
> skip to step 4 and test the DB-only endpoints instead.

---

### AUTHORIZE AS: `kyc_officer / password`

**Step 1 — GET /api/clients/{id}/bureau-readiness**
- Expand: 1. Bureau Readiness → GET /api/clients/{id}/bureau-readiness
- Click: Try it out
- Enter: `id = 5`
- Click: Execute
- Expected 200:
```json
{
  "clientId": 5,
  "score": 80,
  "ready": true,
  "missingFields": ["address", "phoneNumber"],
  "riskBand": "LOW",
  "scoreDropAlert": false,
  "pulledAt": "2026-06-28T02:06:10Z"
}
```
- Expected error (Fineract down): `{"code": "FINERACT_SERVER_ERROR", ...}` HTTP 503
- Expected error (wrong role): HTTP 403 (credit_analyst cannot call this)

---

**Step 2 — POST /api/submissions/report-screening (Trigger 3)**
- Expand: 2. Submission Pipeline → POST /api/submissions/report-screening
- Click: Try it out
- Enter request body:
```json
{
  "clientId": 5,
  "loanId": null,
  "inquiryType": "HARD"
}
```
- Click: Execute
- Expected 201:
```json
{
  "id": 1,
  "clientId": 5,
  "triggerType": "SCREENING_EVENT",
  "status": "ACCEPTED",
  "inquiryType": "HARD",
  "cdcReferenceId": "386636538"
}
```
- Test bad input: change `inquiryType` to `"INVALID"` → Expected 400

---

**Step 3 — POST /api/disputes**
- Expand: 3. Dispute Management → POST /api/disputes
- Click: Try it out
- Enter request body:
```json
{
  "submissionRecordId": 1,
  "disputeDetails": "CDC balance mismatch — loan was fully repaid",
  "raisedBy": "kyc_officer"
}
```
- Click: Execute
- Expected 201:
```json
{
  "id": 1,
  "status": "OPEN",
  "expiryDate": "2032-06-29"
}
```
- **Save the dispute id** — needed for steps 10 and 11

---

**Step 4 — Test 403 (proves RBAC works)**
- Try: GET /api/clients/5/bureau-response as kyc_officer
- Expected: HTTP 403
- This proves KYC_OFFICER cannot access Credit Analyst endpoints

---

### RE-AUTHORIZE AS: `credit_analyst / password`

**Step 5 — GET /api/clients/{id}/bureau-response**
- Enter: `id = 5`
- Expected 200:
```json
{
  "clientId": 5,
  "bureauType": "CIRCULO_DE_CREDITO",
  "riskBand": "LOW",
  "hasDelinquencies": false,
  "scoreDropAlert": false,
  "expiryDate": "2032-06-28",
  "tradelines": "[{...}]"
}
```
- Key field: `expiryDate = 2032-06-28` proves LRSIC 72-month rule working

---

**Step 6 — POST /api/submissions/run**
- Enter request body:
```json
{
  "clientIds": [5]
}
```
- Expected 202:
```json
{
  "message": "Batch submission started",
  "clientCount": 1
}
```
- Returns immediately — job runs in background

---

**Step 7 — GET /api/submissions/history**
- Enter query params: `clientId = 5`, `page = 0`, `size = 20`
- Expected 200: HATEOAS PagedModel with submission records
- Key fields: `status`, `triggerType`, `cdcReferenceId`, `expiryDate`

---

**Step 8 — GET /api/submissions/schedule**
- No params needed
- Expected 200: `[]` (empty = healthy, no pending retries)

---

**Step 9 — POST /api/submissions/report-approval (Trigger 2)**
- Enter request body:
```json
{
  "clientId": 5,
  "loanId": 1
}
```
- Expected 201 (CDC accepted) or 200 (CDC failed but loan NOT blocked)

---

**Step 10 — PUT /api/disputes/{id}/status**
- Enter: `id = 1` (from Step 3)
- Enter request body:
```json
{
  "newStatus": "UNDER_REVIEW",
  "resolutionNotes": "Investigating with CDC team"
}
```
- Expected 200 with `status: "UNDER_REVIEW"`
- Then advance to RESOLVED:
```json
{
  "newStatus": "RESOLVED",
  "resolutionNotes": "Verified — data corrected per LRSIC Article 23"
}
```

---

**Step 11 — GET /api/disputes/{id}**
- Enter: `id = 1`
- Expected 200: full dispute with `status: "RESOLVED"`

---

**Step 12 — Test 403 (proves RBAC works)**
- Try: GET /api/clients/5/bureau-readiness as credit_analyst
- Expected: HTTP 403
- Try: GET /api/clients/5/audit-trail as credit_analyst
- Expected: HTTP 403

---

### RE-AUTHORIZE AS: `compliance / password`

**Step 13 — GET /api/clients/{id}/audit-trail (COMPLIANCE EXCLUSIVE)**
- Enter: `id = 5`, `page = 0`, `size = 20`
- Expected 200: HATEOAS PagedModel with audit entries
- Key fields: `action`, `performedBy`, `result`, `durationMs`, `requestId`
- Every action from steps 1-12 should appear here
- This is the ONLY endpoint exclusive to COMPLIANCE — KYC_OFFICER and CREDIT_ANALYST always get 403

---

**Step 14 — Test separation of duties (proves compliance design)**
- Try: POST /api/submissions/report-screening as compliance
- Expected: HTTP 403
- Try: POST /api/submissions/report-approval as compliance
- Expected: HTTP 403
- Say: "Compliance can AUDIT these actions but cannot CREATE them — separation of duties"

---

## Section 10 — Full Stack Health Check

```bash
#!/bin/bash
echo "=== CB-ILD Full Stack Health Check ==="

echo "1. VPN — checking Mexican IP..."
IP=$(curl -s https://api.ipify.org)
echo "   Current IP: $IP"

echo "2. Plugin — checking /credit-bureaus..."
curl -s -u tester:tempPassword123 http://localhost:8081/credit-bureaus | python3 -m json.tool

echo "3. CB-ILD — Swagger reachable (HTTP 200)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/swagger-ui/index.html)
echo "   Swagger HTTP: $STATUS"

echo "4. Bureau Readiness — calling with client ID 5 (kyc_officer)..."
curl -s -u kyc_officer:password http://localhost:8084/api/clients/5/bureau-readiness | python3 -m json.tool

echo "=== Done ==="
```

**Expected healthy output:**
- Step 1: Mexican IP (e.g. `187.190.27.223`)
- Step 2: Array with bureau ID 13 entry
- Step 3: `200`
- Step 4: `{"score":..., "ready":true, "riskBand":"LOW", "ficoScore":750}` (mock mode)

> 📝 NOTE: If bureau-readiness returns `FINERACT_SERVER_ERROR`, it means `mifos-bank-1.mifos.community` (the Mifos community Fineract server) is down. This is an external server — not your local stack. Try again later.

---

## Section 11 — Environment Variables

**CB-ILD variables:**

| Variable | Purpose | Dev default | Production |
|----------|---------|-------------|------------|
| `CB_ENC_KEY` | AES-256-GCM key for CDC credential encryption | `dev-test-key-32-bytes-long-aes!!` | **CHANGE IN PRODUCTION** — use `openssl rand -base64 32` |
| `FINERACT_BASE_URL` | Fineract API base URL | `https://mifos-bank-1.mifos.community/fineract-provider/api/v1` | OK to use as-is for sandbox |
| `FINERACT_PASSWORD` | Fineract Basic auth password | `password` | OK for sandbox |
| `CBILD_DB_USER` | MariaDB username | `root` | **CHANGE IN PRODUCTION** |
| `CBILD_DB_PASS` | MariaDB password | `root` | **CHANGE IN PRODUCTION** |

**Plugin variables:**

| Variable | Purpose | Dev default | Production |
|----------|---------|-------------|------------|
| `MIFOS_SECURITY_ENCRYPTION_KEY` | AES key for CDC credentials in plugin DB | (generate with openssl) | **CHANGE IN PRODUCTION** |
| `SPRING_SECURITY_USER_NAME` | Plugin Basic auth username | `tester` | OK for sandbox |
| `SPRING_SECURITY_USER_PASSWORD` | Plugin Basic auth password | `tempPassword123` | **CHANGE IN PRODUCTION** |

---

## Section 12 — Mock vs Real Mode

| Setting | `mock.enabled=true` (default) | `mock.enabled=false` |
|---------|-------------------------------|----------------------|
| VPN required | No | Yes |
| Plugin required | No | Yes |
| CDC called | No | Yes |
| `cdcReferenceId` | `MOCK-{uuid}` | Real CDC reference (e.g. `386636538`) |
| `ficoScore` | `750` (hardcoded) | `null` (CDC RCC basic does not return FICO) |
| `riskBand` | `LOW` | Derived from `peorAtraso` (worst delinquency months) |
| Use for | Development, CI | Demo, production |

**How to switch:**
```properties
# src/main/resources/application.properties
mifos.cdc.mock.enabled=false
```

> ⚠️ **WARNING**: Switching to `false` without VPN connected causes `CDC_TIMEOUT` errors on every bureau-readiness and submission call.

---

## Section 13 — CDC Credentials Reference

| Item | Value |
|------|-------|
| Consumer key (x-api-key) | `${CDC_X_API_KEY}` — obtain from Victor Romero |
| Private key path | `~/Downloads/CB/pri_key.pem` (ECDSA secp384r1) |
| Certificate path | `~/Downloads/CB/certificate.pem` |
| CDC sandbox URL | `https://services.circulodecredito.com.mx/sandbox/v1` |
| CDC Developer Portal | `https://developer.circulodecredito.com.mx` |

> ⚠️ **WARNING**: Do not commit `pri_key.pem` or the real `x-api-key` to git. Both are in `.gitignore`.
> Real credentials are provided by Victor Romero (IOhacker). The `certificate.pem` must be uploaded to the CDC developer portal (one-time setup per key pair).

---

## Section 14 — Test Client (Fineract Sandbox)

Always use client ID **5** for demos — this client has been verified end-to-end with real CDC.

| Field | Value |
|-------|-------|
| Client ID | `5` |
| Name | GRANVILLE MARIA DONNELLY |
| RFC | `${TEST_CLIENT_RFC}` — provided by mentor |
| Date of Birth | 07 January 1980 |
| External ID | `${TEST_CLIENT_EXTERNAL_ID}` — provided by mentor |
| Address | CDMX (`stateProvinceId=304`), Mexico (`countryId=167`) |
| Identifier type | Any Other Id Type (`id=4`) |
| Fineract instance | `mifos-bank-1.mifos.community` |
| Tenant | `default` |

---

## Section 15 — Key Generation

**A) CB_ENC_KEY (AES-256, must be 32 bytes):**
```bash
openssl rand -base64 32
export CB_ENC_KEY="<generated-value>"
```

**B) MIFOS_SECURITY_ENCRYPTION_KEY (plugin):**
```bash
openssl rand -base64 32
export MIFOS_SECURITY_ENCRYPTION_KEY="<generated-value>"
```

**C) ECDSA Key Pair (secp384r1) — for CDC request signing:**
```bash
mkdir -p ~/Downloads/CB && cd ~/Downloads/CB
openssl ecparam -name secp384r1 -genkey -out pri_key.pem
openssl req -new -x509 -days 365 -key pri_key.pem -out certificate.pem \
  -subj "/C=MX/CN=CB-ILD"
```

Upload `certificate.pem` to the CDC Developer Portal under your application's credentials. Keep `pri_key.pem` local — never commit it.

---

## Section 16 — Database Reference

| Database | Port | Container | Schema |
|----------|------|-----------|--------|
| cbild_db | 3307 | `mariadb-cb` | `cbild_db` |
| creditbureau | 3306 | `creditbureau_mariadb` | `creditbureau` |

**Liquibase migrations (run automatically on CB-ILD startup):**

| Version | File | What it adds |
|---------|------|-------------|
| V1 | `V1__bureau_response.xml` | `bureau_response` table (10 columns) + index on `client_id` |
| V1b | `V1b__bureau_response_additions.xml` | `risk_band`, `has_delinquencies`, `date_of_first_delinquency`, `raw_response_hash`, `score_drop_alert` |
| V2 | `V2__submission_record.xml` | `submission_record` table (11 columns) + status + client_id indexes |
| V3 | `V3__dispute_case.xml` | `dispute_case` table (9 columns) |
| V4 | `V4__audit_entry.xml` | `audit_entry` table (9 columns) + record_id index |
| V5 | `V5__archive_tables.xml` | `bureau_response_archive` + `submission_record_archive` tables |
| V6 | `V6__audit_entry_additions.xml` | `duration_ms`, `result`, `error_message` on `audit_entry` |
| V7 | `V7__archive_tables_autoincrement.xml` | Fix: `AUTO_INCREMENT` on archive table `id` columns |
| V8 | `V8__expiry_date_to_date.xml` | Fix: `expiry_date` TIMESTAMP → DATE (LRSIC timezone bug) |
| V9 | `V9__submission_record_additions.xml` | `created_at`, `cdc_reference_id`, `inquiry_type`, `expiry_date` + composite retry index |
| V9b | `V9b__dispute_case_additions.xml` | `raised_by`, `resolution_notes`, `expiry_date` on `dispute_case` + submission index |
| V10 | `V10__audit_entry_client_id.xml` | `client_id` column + index on `audit_entry` |

---

## Section 17 — RBAC Access Matrix

✅ = Allowed | ❌ = HTTP 403 Forbidden

| Endpoint | @PreAuthorize | KYC_OFFICER | CREDIT_ANALYST | COMPLIANCE |
|----------|--------------|-------------|----------------|------------|
| `GET /api/clients/{id}/bureau-readiness` | `hasAnyRole('KYC_OFFICER','COMPLIANCE')` | ✅ | ❌ | ✅ |
| `GET /api/clients/{id}/bureau-response` | `hasAnyRole('CREDIT_ANALYST','COMPLIANCE')` | ❌ | ✅ | ✅ |
| `GET /api/clients/{id}/audit-trail` | `hasRole('COMPLIANCE')` | ❌ | ❌ | ✅ |
| `POST /api/submissions/run` | `hasAnyRole('CREDIT_ANALYST','COMPLIANCE')` | ❌ | ✅ | ✅ |
| `GET /api/submissions/history` | `hasAnyRole('CREDIT_ANALYST','COMPLIANCE')` | ❌ | ✅ | ✅ |
| `GET /api/submissions/schedule` | `hasAnyRole('CREDIT_ANALYST','COMPLIANCE')` | ❌ | ✅ | ✅ |
| `POST /api/submissions/report-screening` | `hasRole('KYC_OFFICER')` | ✅ | ❌ | ❌ |
| `POST /api/submissions/report-approval` | `hasAnyRole('CREDIT_ANALYST','COMPLIANCE')` | ❌ | ✅ | ✅ |
| `POST /api/disputes` | `hasAnyRole('KYC_OFFICER','CREDIT_ANALYST','COMPLIANCE')` | ✅ | ✅ | ✅ |
| `PUT /api/disputes/{id}/status` | `hasAnyRole('KYC_OFFICER','CREDIT_ANALYST','COMPLIANCE')` | ✅ | ✅ | ✅ |
| `GET /api/disputes/{id}` | `hasAnyRole('KYC_OFFICER','CREDIT_ANALYST','COMPLIANCE')` | ✅ | ✅ | ✅ |

---

## Section 18 — Troubleshooting

| Error | HTTP Code | Cause | Fix |
|-------|-----------|-------|-----|
| `Connection refused: 3307` | (startup fails) | `mariadb-cb` container not running | `docker start mariadb-cb` or `docker compose up -d` |
| `Connection refused: 8081` | `CDC_NOT_CONFIGURED` 503 | Plugin container not running | `cd plugin && docker compose up -d` |
| `FINERACT_SERVER_ERROR` | 503 | `mifos-bank-1.mifos.community` is down | Community server — wait and retry. Not your problem. |
| `CDC_TIMEOUT` | 503 | VPN not connected or disconnected | Connect VPN, verify `curl -s https://api.ipify.org` returns Mexican IP |
| HTTP 403 Forbidden | 403 | Wrong role for endpoint | Check RBAC matrix §17. Use `compliance` user for any endpoint. |
| HTTP 401 Unauthorized | 401 | Not authenticated in Swagger | Click Authorize in Swagger UI, enter credentials, click Authorize then Close |
| `mockEnabled: true` in logs | (info only) | `mifos.cdc.mock.enabled=true` (default) | Normal for development. Switch to `false` for real CDC. |
| Credit bureau ID mismatch | 400 from plugin | `docker compose down` reset the plugin DB; bureau ID incremented | Run Section 7 again to re-register and update `application.properties` |
| `Port 8084 already in use` | (BUILD FAILED) | Previous CB-ILD process still running | `lsof -ti:8084 | xargs kill` |
| `CB_ENC_KEY not 32 bytes` | (warning in log) | `CB_ENC_KEY` env var is wrong length | Generate correct key: `openssl rand -base64 32` |
| `score:0, ready:false` | 200 | Client RFC is null in Fineract | Add identifier with `documentType=NATIONAL_ID` for client in Fineract UI |
| `Liquibase checksum mismatch` | (startup fails) | Migration XML file was edited after first run | Run `./gradlew bootRun` on a fresh DB, or clear Liquibase locks: `docker exec mariadb-cb mysql -uroot -proot cbild_db -e "DELETE FROM DATABASECHANGELOGLOCK"` |
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-green)
![Java](https://img.shields.io/badge/Java-21-blue)
![License](https://img.shields.io/badge/License-MPL%202.0-orange)
![MSOC](https://img.shields.io/badge/MSOC-2026-purple)
![Tests](https://img.shields.io/badge/Tests-146%20passing-brightgreen)
![Coverage](https://img.shields.io/badge/Coverage-80%25+-brightgreen)

# CB-ILD — Credit Bureau Information Lifecycle Dashboard

> CB-ILD is the missing middleware that connects Apache Fineract microfinance institutions in Mexico to Circulo de Credito (CDC) — automating KYC validation, loan reporting, and LRSIC compliance so loan officers never have to touch a credit bureau manually.

---

## What Problem Does This Solve?

In Mexico, microfinance institutions (MFIs) using Apache Fineract are legally required to:
1. Report every loan to Circulo de Credito (CDC) — Mexico's largest credit bureau
2. Log every credit inquiry (HARD/SOFT) per LRSIC law
3. Delete negative credit data after 72 months (CONDUSEF requirement)
4. Maintain a full audit trail for regulatory inspection

**Before CB-ILD — everything was manual or broken:**

| Problem | Real Impact |
|---|---|
| RFC (Mexican tax ID) always null in plugin | CDC rejected 100% of submissions — CDC-001 error every time |
| No KYC check before CDC submission | Bad client data sent to CDC — wasted API calls |
| No retry on failed submissions | Failed CDC reports silently lost — institution out of compliance |
| No audit trail | CONDUSEF audit = institution has nothing to show |
| No LRSIC 72-month expiry tracking | Negative data kept forever — illegal under Mexican law |
| Manual CDC reporting by loan officers | Human error, delays, missed reports |

**After CB-ILD — everything is automatic:**

| Problem | How CB-ILD Fixes It |
|---|---|
| RFC always null | FineractApiClient calls /identifiers — RFC always fetched before CDC |
| No KYC check | KycCompletenessScorer — weighted 0-100 score, RFC hard veto at 0 |
| No retry | SubmissionRetryScheduler — every 6h, exponential backoff, 3 attempts max |
| No audit trail | @Auditable AOP — every action auto-logged in REQUIRES_NEW transaction |
| No LRSIC expiry | expiryDate = dateOfFirstDelinquency + 72 months on every record |
| Manual reporting | 3 automatic triggers fire at exactly the right moments |

---

## Who Uses CB-ILD — 3 Roles

| Role | Real Person | Their Job | Endpoints They Use |
|---|---|---|---|
| KYC Officer | Checks client data before loans | Validates client is CDC-ready, logs credit inquiries | bureau-readiness, report-screening, disputes |
| Credit Analyst | Reviews loans, manages CDC pipeline | Triggers batch submissions, checks history | bureau-response, all submissions, disputes |
| Compliance | Audits everything for CONDUSEF | Views full audit trail, monitors LRSIC expiry | audit-trail + all read endpoints |

Separation of duties: Compliance can READ everything but cannot TRIGGER any CDC operation.
The auditor is never the operator — enforced at the API level via @PreAuthorize.

---

## The 3 CDC Trigger Points

These are the 3 moments where CB-ILD talks to CDC. Defined by Victor Romero (MSOC mentor).

```
TRIGGER 1 — Pre-Approval KYC Check
WHO:      KYC Officer
WHEN:     Before any loan decision is made
WHAT:     Check if client data is complete enough for CDC reporting
HOW:      Score client 0-100, if score >= 70 pull CDC credit report
RFC:      RFC missing = score 0, CDC never called (hard veto)
RESULT:   KYC Officer sees score, riskBand, missing fields
ENDPOINT: GET /api/clients/{id}/bureau-readiness

TRIGGER 2 — Post-Approval Loan Reporting
WHO:      System (fires automatically when loan approved in Mifos)
WHEN:     Immediately after loan officer approves loan
WHAT:     Report the loan approval to CDC in background
CRITICAL: NEVER blocks the loan officer — even if CDC is down, loan proceeds
          Failed report saved as PENDING_RETRY, retried automatically every 6h
ENDPOINT: POST /api/submissions/report-approval

TRIGGER 3 — Screening Event Logging (LRSIC Required)
WHO:      KYC Officer
WHEN:     Every time a credit check is run
WHAT:     Log the inquiry to CDC as required by LRSIC law
TYPE:     HARD = formal credit check, SOFT = informal pre-qualification
ENDPOINT: POST /api/submissions/report-screening
```

---

## Architecture — 3 Layers + 2 External Services

```
LAYER 1 — ANGULAR FRONTEND (openMF/web-app)
+-------------------------------------------------------------+
|  Tab 1: Bureau Readiness  |  Tab 2: Submission Dashboard   |
|  Tab 3: Credit Profile    |  Tab 4: Disputes and History   |
|                           |  Tab 5: Bureau Feedback        |
+---------------------------+--------------------------------+
                            |
                            |  HTTP Basic Auth
                            |  kyc_officer / credit_analyst / compliance
                            v
LAYER 2 — CB-ILD MICROSERVICE (port 8084)
+-------------------------------------------------------------+
|                                                             |
|  3 CONTROLLERS — 11 ENDPOINTS                               |
|  +-----------------+ +------------------+ +-------------+   |
|  |BureauReadiness  | |SubmissionCtrl    | |DisputeCtrl  |   |
|  |• readiness      | |• run (batch)     | |• POST open  |   |
|  |• bureau-response| |• history         | |• PUT status |   |
|  |• audit-trail    | |• schedule        | |• GET by id  |   |
|  +--------+--------+ |• report-screening| +------+------+   |
|           |          |• report-approval |        |          |
|           +----------+--------+---------+--------+          |
|                               |                             |
|  SERVICES                     |                             |
|  KycCompletenessScorer        weighted 0-100 KYC scoring    |
|  BureauReadinessService       Trigger 1 orchestration       |
|  SubmissionService            CDC submission pipeline       |
|  SubmissionRetryScheduler     @Scheduled every 6h retry     |
|  RetentionService             @Scheduled 2am LRSIC archive  |
|  DisputeService               OPEN -> REVIEW -> RESOLVED    |
|                                                             |
|  CROSS-CUTTING                                              |
|  @Auditable AOP    auto-saves audit_entry (REQUIRES_NEW)    |
|  @PreAuthorize     RBAC enforcement per endpoint            |
|  @SQLRestriction   soft_deleted=false on all queries        |
|                                                             |
|  cbild_db (MariaDB port 3307)                               |
|  bureau_response, submission_record, audit_entry            |
|  dispute_case, *_archive tables                             |
+------------------------------+------------------------------+
                               |
                               |  HTTP Basic Auth
                               |  tester:tempPassword123
                               v
LAYER 3 — PLUGIN (port 8081)
+-------------------------------------------------------------+
|  openMF/mifos-x-credit-bureau-plugin  (PR #126)             |
|                                                             |
|  +--------------------+    +-----------------------------+  |
|  | ClientApiService   |    | ConsolidatedCreditReport    |  |
|  |                    |    | Service                     |  |
|  | Fetches from       |    | Builds CDC payload          |  |
|  | Fineract:          |    | Signs with ECDSA secp384r1  |  |
|  | - name, DOB        |    | Sends to CDC via VPN        |  |
|  | - RFC (tax ID)     |    | Returns credit report       |  |
|  | - address          |    |                             |  |
|  +----------+---------+    +-------------+---------------+  |
|             |                            |                  |
|  creditbureau (MariaDB port 3306)        |                  |
|  CDC credentials AES-256-GCM encrypted  |                  |
+-------------+----------------------------+------------------+
              |                            |
              | REST API                   | HTTPS + ECDSA signature
              | GET /clients/{id}          |
              | GET /clients/{id}/          |  [!] VPN REQUIRED
              |     identifiers            |  Cloudflare blocks
              v                            |  non-Mexican IPs
+------------------------+                 v
| Apache Fineract         | +------------------------------+
| mifos-bank-1.mifos.    | | Circulo de Credito (CDC)     |
| community               | | services.circulodecredito    |
|                         | | .com.mx/sandbox/v1           |
| Stores: client data,    | |                              |
| RFC tax ID, loan        | | Returns: riskBand,           |
| history, payments       | | tradelines, delinquency      |
+------------------------+ +------------------------------+
```

---

## Step-by-Step Request Flow — Trigger 1 (KYC Check)

```
STEP 1   KYC Officer opens Angular Tab 1
         Enters client ID 5, clicks Check Bureau Readiness
         |
         v
STEP 2   Angular sends request to CB-ILD:
         GET /api/clients/5/bureau-readiness
         Authorization: Basic kyc_officer:password
         |
         v
STEP 3   CB-ILD BureauReadinessController receives request
         @PreAuthorize checks role = KYC_OFFICER  [OK]
         @Auditable starts timing the operation
         CorrelationIdFilter adds requestId to every log line
         |
         v
STEP 4   CB-ILD calls FineractApiClient
         GET /clients/5              -> name, DOB, address
         GET /clients/5/identifiers  -> RFC tax ID
         |
         v
STEP 5   KycCompletenessScorer calculates score:
         RFC present?   +30 pts  [OK]  (CDC primary matching key)
         DOB present?   +20 pts  [OK]
         Name present?  +30 pts  [OK]
         Address?       +15 pts  [MISSING]
         Phone?         + 5 pts  [MISSING]
         Total = 80 / 100
         Threshold = 70  ->  Result = READY
         |
         v
STEP 6   CB-ILD calls Plugin:
         POST /circulo-de-credito/rcc/5
         Plugin decrypts CDC credentials from MariaDB (AES-256-GCM)
         Plugin builds CDC request payload (name, DOB, RFC)
         Plugin signs request with ECDSA secp384r1 private key
         Plugin sends HTTPS request to CDC via VPN tunnel
         |
         v
STEP 7   CDC returns credit report for the client:
         riskBand = LOW
         tradelines = [loan at MICROFINANCIERA, balance 14714]
         worstDelinquency = 0 (never missed a payment)
         |
         v
STEP 8   CB-ILD saves bureau_response to cbild_db:
         riskBand = LOW
         hasDelinquencies = false
         scoreDropAlert = false
         expiryDate = today + 72 months  <- LRSIC compliance
         |
         v
STEP 9   @Auditable AOP saves audit_entry (REQUIRES_NEW transaction):
         action = BUREAU_READINESS_CHECK
         performedBy = kyc_officer
         result = SUCCESS
         durationMs = 1842
         NOTE: saved even if main transaction fails
         |
         v
STEP 10  CB-ILD returns KycReadinessResult to Angular:
         {
           "clientId": 5,
           "score": 80,
           "ready": true,
           "missingFields": ["address", "phoneNumber"],
           "riskBand": "LOW",
           "scoreDropAlert": false,
           "pulledAt": "2026-06-29T02:06:10Z"
         }
         |
         v
STEP 11  Angular Tab 1 renders the result:
         [OK] Client is CDC-ready — score 80/100
         [!!] Missing fields: address, phone number
         [--] Risk Band: LOW — safe to proceed with loan
```

---

## Step-by-Step Request Flow -- Trigger 2 (Loan Approval Reporting)

```
STEP 1   Loan Officer clicks Approve Loan in Mifos web-app

STEP 2   Angular automatically calls CB-ILD in background:
         POST /api/submissions/report-approval
         Body: {clientId: 5, loanId: 1}
         Role: credit_analyst

STEP 3   CB-ILD SubmissionController receives request
         @PreAuthorize checks role = CREDIT_ANALYST  OK
         @Auditable starts timing
         try-catch wraps everything -- NEVER throws to caller

STEP 4   CB-ILD calls Plugin to report loan to CDC
         Plugin decrypts credentials from MariaDB
         Plugin builds CDC loan approval payload
         Plugin signs with ECDSA secp384r1
         Plugin sends to CDC via VPN

STEP 5A  CDC accepts:
         submission_record: status=ACCEPTED, triggerType=LOAN_APPROVAL
         expiryDate = today + 72 months (LRSIC)
         Returns: 201 Created

STEP 5B  CDC is down or fails:
         submission_record: status=PENDING_RETRY
         SubmissionRetryScheduler retries in 60 minutes (max 3 attempts)
         Returns: 200 OK -- loan approval NOT blocked
         Loan officer sees nothing -- loan proceeds normally

STEP 6   @Auditable saves audit_entry (REQUIRES_NEW):
         action=SUBMISSION_LOAN_APPROVAL, performedBy=credit_analyst
         Saved even if CDC rejected the submission
```

---

## Step-by-Step Request Flow -- Trigger 3 (Screening Event Logging)

```
STEP 1   KYC Officer runs a credit check on a client

STEP 2   Angular calls CB-ILD:
         POST /api/submissions/report-screening
         Body: {clientId: 5, loanId: null, inquiryType: HARD}
         Role: kyc_officer

STEP 3   CB-ILD validates request
         @PreAuthorize checks role = KYC_OFFICER  OK
         inquiryType must be HARD or SOFT
         Any other value -- returns 400 Bad Request immediately

STEP 4   CB-ILD calls Plugin to log inquiry to CDC
         CDC records: institution ran HARD inquiry on this client today
         LRSIC requires every credit inquiry to be logged

STEP 5   submission_record saved:
         status=ACCEPTED, triggerType=SCREENING_EVENT, inquiryType=HARD
         Returns: 201 Created

STEP 6   @Auditable saves audit_entry (REQUIRES_NEW):
         action=SUBMISSION_SCREENING, performedBy=kyc_officer
         result=SUCCESS

STEP 7   Compliance audits this via GET /api/clients/5/audit-trail
         Compliance CANNOT create screening events -- only audit them
         Separation of duties enforced at the API level
```

---

## All 11 REST Endpoints

| Method | Endpoint | Role | What it does | Response |
|---|---|---|---|---|
| GET | /api/clients/{id}/bureau-readiness | KYC_OFFICER, COMPLIANCE | Trigger 1 — KYC score + live CDC pull | 200 KycReadinessResult |
| GET | /api/clients/{id}/bureau-response | CREDIT_ANALYST, COMPLIANCE | Stored CDC report — no new CDC call | 200 BureauResponseDTO |
| GET | /api/clients/{id}/audit-trail | COMPLIANCE only | Full paginated audit log — exclusive | 200 PagedModel |
| POST | /api/submissions/run | CREDIT_ANALYST, COMPLIANCE | Start async batch CDC submission | 202 BatchSubmissionAck |
| GET | /api/submissions/history | CREDIT_ANALYST, COMPLIANCE | Paginated submission history | 200 PagedModel |
| GET | /api/submissions/schedule | CREDIT_ANALYST, COMPLIANCE | View PENDING_RETRY retry queue | 200 List |
| POST | /api/submissions/report-screening | KYC_OFFICER only | Trigger 3 — log HARD/SOFT inquiry | 201 SubmissionRecordResponse |
| POST | /api/submissions/report-approval | CREDIT_ANALYST, COMPLIANCE | Trigger 2 — report loan approval | 201 or 200 |
| POST | /api/disputes | ALL roles | Open dispute against CDC data | 201 DisputeResponse |
| PUT | /api/disputes/{id}/status | ALL roles | OPEN to UNDER_REVIEW to RESOLVED | 200 DisputeResponse |
| GET | /api/disputes/{id} | ALL roles | View dispute by ID | 200 DisputeResponse |

---

## RBAC — Who Can Access What

| Endpoint | KYC Officer | Credit Analyst | Compliance |
|---|---|---|---|
| GET bureau-readiness | YES | NO 403 | YES |
| GET bureau-response | NO 403 | YES | YES |
| GET audit-trail | NO 403 | NO 403 | YES EXCLUSIVE |
| POST submissions/run | NO 403 | YES | YES |
| GET submissions/history | NO 403 | YES | YES |
| GET submissions/schedule | NO 403 | YES | YES |
| POST report-screening | YES | NO 403 | NO 403 |
| POST report-approval | NO 403 | YES | NO 403 |
| POST/PUT/GET disputes | YES | YES | YES |

**Swagger credentials (sandbox only — not for production):**

| Username | Password | Role |
|---|---|---|
| kyc_officer | password | KYC_OFFICER |
| credit_analyst | password | CREDIT_ANALYST |
| compliance | password | COMPLIANCE |

---

## Tech Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 21 | Records, text blocks, pattern matching |
| Framework | Spring Boot | 3.2.6 | Application server |
| Database | MariaDB | 10.11 | Persistent storage on port 3307 |
| Migrations | Liquibase | boot-managed | Schema versioning V1 through V10 |
| Security | Spring Security | 6.x | HTTP Basic Auth + @PreAuthorize RBAC |
| Encryption | AES-256-GCM | — | CDC credential storage at rest |
| Signing | ECDSA secp384r1 | — | CDC request signing via plugin |
| Audit | Spring AOP | 6.x | @Auditable auto-logs every CDC action |
| API Docs | SpringDoc OpenAPI | 2.5.0 | Swagger UI at /swagger-ui/index.html |
| HTTP Client | RestTemplate | 6.x | Two named beans for Fineract and plugin |
| Testing | JUnit 5 + WireMock | — | 146 tests, JaCoCo 80% minimum enforced |
| Build | Gradle | 9.x | ./gradlew bootRun |

---

## Database Schema (cbild_db — port 3307)

Liquibase migrations V1 through V10 run automatically on CB-ILD startup.

| Table | Purpose | Soft Delete | LRSIC Expiry |
|---|---|---|---|
| bureau_response | CDC credit report per client per pull | YES | dateOfFirstDelinquency + 72 months |
| submission_record | Every CDC submission attempt with status | YES | submission date + 72 months |
| audit_entry | Every @Auditable action — never deleted | NO | Permanent |
| dispute_case | Dispute workflow OPEN to RESOLVED | YES | openedAt + 72 months |
| bureau_response_archive | LRSIC-expired bureau rows | NO | Archived only |
| submission_record_archive | LRSIC-expired submission rows | NO | Archived only |

---

## Quick Start — No VPN Needed (Mock Mode)

```bash
git clone https://github.com/saksham869/cb-ild && cd cb-ild
docker compose up -d
export CB_ENC_KEY="dev-test-key-32-bytes-long-aes!!"
./gradlew bootRun
open http://localhost:8084/swagger-ui/index.html
```

Click Authorize, enter kyc_officer / password, test all 11 endpoints immediately.

> mock.enabled=true by default — returns realistic fake data, no VPN or CDC needed.
> For real CDC sandbox with VPN, see SETUP.md.

---

## Compliance

**LRSIC — Ley para Regular las Sociedades de Informacion Crediticia**
Mexican law requiring negative credit data deleted 72 months after debt settlement.
CB-ILD tracks expiryDate on every record. RetentionService archives expired rows nightly at 2am.
Hibernate @SQLRestriction ensures expired data never appears in any query.
Hard delete is banned — soft_deleted flag only.

**CONDUSEF — Mexico financial consumer protection regulator**
Requires MFIs to maintain traceable dispute records and CDC audit trails.
CB-ILD implements DisputeCase state machine (OPEN to UNDER_REVIEW to RESOLVED) with full
timestamps. COMPLIANCE-only audit-trail endpoint shows every CDC interaction per client.

**RFC — Registro Federal de Contribuyentes**
Mexico tax ID — CDC primary client matching key.
Treated as PII: never logged, never in any DTO, never in audit_entry.
Missing RFC forces score=0 and hard-vetoes the CDC call.

---

## Testing

```bash
./gradlew test
./gradlew test jacocoTestReport
# HTML report: build/reports/jacoco/test/html/index.html
```

146 tests across 21 test files: unit tests for all services, MockMvc integration
tests for all 11 endpoints, Spring Security tests for 401 and 403 enforcement,
WireMock stubs for Fineract and CDC HTTP, AOP proxy behavior tests.
JaCoCo 80% minimum enforced — build fails if coverage drops below threshold.

---

## Project Links

| Resource | Link |
|---|---|
| CB-ILD Repository | https://github.com/saksham869/cb-ild |
| Plugin Repository | https://github.com/openMF/mifos-x-credit-bureau-plugin |
| Plugin PR 126 | https://github.com/openMF/mifos-x-credit-bureau-plugin/pull/126 |
| Mifos Initiative | https://mifos.org |


## Author

**Satyam Mishra** — MSOC 2026 Intern, Mifos Initiative
Mentors: **Victor Romero** and **Yu Wati Nyi**
GitHub: [github.com/saksham869](https://github.com/saksham869)

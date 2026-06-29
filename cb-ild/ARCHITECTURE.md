# CB-ILD Architecture Reference

**Project:** `cb-ild` — Credit Bureau ILD (Información de Lealtad de Datos)  
**Stack:** Spring Boot 3.2.6 · Java 21 · Gradle · MariaDB / H2 (test) · Liquibase · WireMock  
**Root package:** `org.mifos.creditbureau.cb_ild`

---

## 1. Package Structure

```
org.mifos.creditbureau.cb_ild
│
├── aop/                      # Cross-cutting audit concern
│   ├── Auditable.java            annotation — marks methods for audit capture
│   ├── CbildAuditAspect.java     @Around advice — captures caller, args, result
│   └── AuditPersistenceService.java  separate @Service to avoid self-invocation
│
├── client/                   # Fineract HTTP layer
│   ├── FineractApiClient.java    @Component — calls 3 Fineract endpoints
│   ├── FineractClientData.java   aggregator record + KYC helper methods
│   ├── FineractClientResponse.java
│   ├── FineractClientListResponse.java
│   ├── FineractIdentifierDTO.java
│   └── FineractAddressDTO.java
│
├── config/                   # Spring configuration
│   ├── RestTemplateConfig.java   @Configuration — fineractRestTemplate + pluginRestTemplate beans
│   └── security/
│       └── SecurityConfig.java   @Configuration — Phase 1 permitAll, stateless, CSRF off
│
├── controller/               # REST layer
│   ├── BureauReadinessController.java
│   ├── SubmissionController.java
│   ├── DisputeController.java
│   ├── GlobalExceptionHandler.java   @RestControllerAdvice
│   └── ErrorResponse.java            record
│
├── entity/                   # JPA entities
│   ├── BureauResponseEntity.java     15-column CDC score row
│   ├── BureauResponseArchive.java    LRSIC 72-month archive copy
│   ├── SubmissionRecord.java         per-client CDC submission
│   ├── DisputeCase.java              dispute state machine
│   ├── AuditEntry.java               audit trail row
│   └── enums/
│       ├── DisputeStatus.java        OPEN → UNDER_REVIEW → RESOLVED / REJECTED
│       ├── SubmissionStatus.java     ACCEPTED / REJECTED / PENDING_RETRY / PERMANENTLY_FAILED
│       └── TriggerType.java          MANUAL / SCHEDULED / RETRY
│
├── exception/                # Domain exceptions (all extend RuntimeException)
│   ├── CdcNotConfiguredException.java    503
│   ├── CdcBadRequestException.java       400
│   ├── CdcServerException.java           502
│   ├── CdcTimeoutException.java          504
│   ├── KycPrerequisiteException.java     422
│   ├── FineractNotFoundException.java    404
│   ├── FineractConnectionException.java  504
│   └── FineractServerException.java      502
│
├── filter/
│   └── CorrelationIdFilter.java   @Component — injects X-Correlation-ID into MDC
│
├── repository/               # Spring Data JPA interfaces
│   ├── AuditEntryRepository.java
│   ├── BureauResponseRepository.java
│   ├── BureauResponseArchiveRepository.java
│   ├── SubmissionRecordRepository.java
│   └── DisputeCaseRepository.java
│
├── security/
│   └── EncryptionService.java   @Service — AES-GCM, key from CB_ENC_KEY env var
│
└── service/
    ├── bureau/
    │   ├── IBureauReadinessService.java
    │   └── BureauReadinessService.java   orchestrates Fineract → KYC → CDC pipeline
    │
    ├── cdc/
    │   ├── ICdcScorePullService.java
    │   └── CdcScorePullServiceImpl.java  FICO pull, SHA-256 hash, score-drop detect
    │
    ├── dispute/
    │   ├── IDisputeService.java
    │   └── DisputeServiceImpl.java       dispute CRUD + state machine
    │
    ├── kyc/
    │   ├── IKycScoringService.java
    │   ├── KycCompletenessScorer.java    weighted scorer (threshold=70)
    │   ├── KycScoringProperties.java     @ConfigurationProperties prefix=mifos.kyc.scoring
    │   └── KycReadinessResult.java       record returned to Angular Tab 1
    │
    ├── retention/
    │   ├── IRetentionService.java
    │   ├── RetentionService.java         @Scheduled 2am — LRSIC nightly archive
    │   └── RetentionArchiveService.java  separate bean for per-row @Transactional
    │
    └── submission/
        ├── ISubmissionService.java
        ├── SubmissionServiceImpl.java    batch submit + exponential backoff retry
        ├── SubmissionRetryScheduler.java @Scheduled every 6h — processes PENDING_RETRY
        ├── SubmissionRetryProperties.java  @ConfigurationProperties prefix=cbild.schedule
        ├── BatchSubmissionAck.java       record — POST /run response
        └── SubmissionRecordResponse.java record — GET /history response
```

---

## 2. Spring Beans and Their Dependencies

### 2.1 Configuration Beans

| Bean | Class | Annotation | Provides |
|------|-------|-----------|---------|
| SecurityFilterChain | `SecurityConfig` | `@Configuration` | Phase 1 permitAll, stateless, CSRF disabled |
| `fineractRestTemplate` | `RestTemplateConfig` | `@Configuration @Bean` | RestTemplate wired to Fineract base URL |
| `pluginRestTemplate` | `RestTemplateConfig` | `@Configuration @Bean` | RestTemplate wired to plugin (CDC proxy) URL |

### 2.2 Infrastructure / Cross-cutting Beans

| Bean | Stereotype | Constructor dependencies |
|------|-----------|--------------------------|
| `CorrelationIdFilter` | `@Component` | none |
| `CbildAuditAspect` | `@Component` `@Order(HIGHEST_PRECEDENCE)` | `AuditPersistenceService` |
| `AuditPersistenceService` | `@Service` | `AuditEntryRepository` |
| `EncryptionService` | `@Service` | `@Value("${CB_ENC_KEY}")` |

### 2.3 KYC Beans

| Bean | Stereotype | Constructor dependencies |
|------|-----------|--------------------------|
| `KycScoringProperties` | `@Component @ConfigurationProperties(mifos.kyc.scoring)` | none |
| `KycCompletenessScorer` → `IKycScoringService` | `@Service` | `KycScoringProperties` |

### 2.4 External Client Beans

| Bean | Stereotype | Constructor dependencies |
|------|-----------|--------------------------|
| `FineractApiClient` | `@Component` | `fineractRestTemplate`, `pluginRestTemplate`, `@Value` (fineract URL, credentials) |

### 2.5 Domain Service Beans

| Bean | Stereotype | Constructor dependencies |
|------|-----------|--------------------------|
| `CdcScorePullServiceImpl` → `ICdcScorePullService` | `@Service` | `BureauResponseRepository`, `@Value(mifos.cdc.mock.enabled)` |
| `BureauReadinessService` → `IBureauReadinessService` | `@Service` | `FineractApiClient`, `IKycScoringService`, `ICdcScorePullService` |
| `RetentionArchiveService` | `@Service` | `BureauResponseRepository`, `BureauResponseArchiveRepository` |
| `RetentionService` → `IRetentionService` | `@Service @Scheduled(cron="0 0 2 * * *")` | `BureauResponseRepository`, `RetentionArchiveService` |
| `DisputeServiceImpl` → `IDisputeService` | `@Service` | `DisputeCaseRepository`, `SubmissionRecordRepository`, `BureauResponseRepository`, `FineractApiClient`, `ObjectMapper` |
| `SubmissionRetryProperties` | `@Component @ConfigurationProperties(cbild.schedule)` | none |
| `SubmissionServiceImpl` → `ISubmissionService` | `@Service` | `SubmissionRecordRepository`, `FineractApiClient`, `IKycScoringService`, `SubmissionRetryProperties`, `@Value(mifos.cdc.mock.enabled)` |
| `SubmissionRetryScheduler` | `@Component @Scheduled(cron="0 0 */6 * * *")` | `SubmissionRecordRepository`, `ISubmissionService`, `SubmissionRetryProperties` |

### 2.6 Controller Beans

| Bean | Stereotype | Constructor dependencies |
|------|-----------|--------------------------|
| `BureauReadinessController` | `@RestController` | `IBureauReadinessService` |
| `SubmissionController` | `@RestController` | `ISubmissionService`, `SubmissionRecordRepository` |
| `DisputeController` | `@RestController` | `IDisputeService`, `DisputeCaseRepository` |
| `GlobalExceptionHandler` | `@RestControllerAdvice` | none |

### 2.7 Repository Beans (Spring Data JPA)

| Interface | @Annotation |
|-----------|------------|
| `AuditEntryRepository` | `@Repository` |
| `BureauResponseRepository` | `@Repository` |
| `BureauResponseArchiveRepository` | `@Repository` |
| `SubmissionRecordRepository` | `@Repository` |
| `DisputeCaseRepository` | `@Repository` |

### 2.8 Dependency Graph (text form)

```
BureauReadinessController
  └─ IBureauReadinessService (BureauReadinessService)
       ├─ FineractApiClient
       │    ├─ fineractRestTemplate  [RestTemplateConfig]
       │    └─ pluginRestTemplate    [RestTemplateConfig]
       ├─ IKycScoringService (KycCompletenessScorer)
       │    └─ KycScoringProperties
       └─ ICdcScorePullService (CdcScorePullServiceImpl)
            └─ BureauResponseRepository

SubmissionController
  ├─ ISubmissionService (SubmissionServiceImpl)
  │    ├─ SubmissionRecordRepository
  │    ├─ FineractApiClient
  │    ├─ IKycScoringService
  │    └─ SubmissionRetryProperties
  └─ SubmissionRecordRepository

DisputeController
  ├─ IDisputeService (DisputeServiceImpl)
  │    ├─ DisputeCaseRepository
  │    ├─ SubmissionRecordRepository
  │    ├─ BureauResponseRepository
  │    ├─ FineractApiClient
  │    └─ ObjectMapper
  └─ DisputeCaseRepository

SubmissionRetryScheduler
  ├─ SubmissionRecordRepository
  ├─ ISubmissionService
  └─ SubmissionRetryProperties

RetentionService   [@Scheduled 2am]
  ├─ BureauResponseRepository
  └─ RetentionArchiveService
       ├─ BureauResponseRepository
       └─ BureauResponseArchiveRepository

CbildAuditAspect   [@Around @Auditable]
  └─ AuditPersistenceService
       └─ AuditEntryRepository
```

---

## 3. REST Endpoints and @PreAuthorize Rules

> **Phase status:** `SecurityConfig` is Phase 1 `permitAll()` + stateless + CSRF disabled.  
> `@EnableMethodSecurity` is NOT yet active (pending MX-276).  
> All `@PreAuthorize` annotations are written ahead of time but not currently enforced.

### 3.1 Endpoint Table

| Method | Path | Controller Method | @PreAuthorize | @Auditable action |
|--------|------|------------------|---------------|-------------------|
| `GET` | `/api/clients/{id}/bureau-readiness` | `BureauReadinessController#checkReadiness` | `hasAnyRole('KYC_OFFICER', 'COMPLIANCE')` | `CDC_SCORE_PULL` |
| `POST` | `/api/submissions/run` | `SubmissionController#runBatch` | `hasAnyRole('CREDIT_ANALYST', 'COMPLIANCE')` | `SUBMISSION_RUN` |
| `GET` | `/api/submissions/history` | `SubmissionController#getHistory` | `hasAnyRole('CREDIT_ANALYST', 'COMPLIANCE')` | *(none — read only)* |
| `POST` | `/api/disputes` | `DisputeController#createDispute` | `hasAnyRole('KYC_OFFICER', 'CREDIT_ANALYST', 'COMPLIANCE')` | `DISPUTE_CREATE` |
| `PUT` | `/api/disputes/{id}/status` | `DisputeController#updateStatus` | `hasAnyRole('KYC_OFFICER', 'CREDIT_ANALYST', 'COMPLIANCE')` | *(none)* |
| `GET` | `/api/disputes/{id}` | `DisputeController#getDispute` | `hasAnyRole('KYC_OFFICER', 'CREDIT_ANALYST', 'COMPLIANCE')` | *(none — read only)* |

### 3.2 RBAC Summary

| Role | Endpoints accessible |
|------|---------------------|
| `KYC_OFFICER` | `/bureau-readiness`, all `/disputes/*` |
| `CREDIT_ANALYST` | `/submissions/*`, all `/disputes/*` |
| `COMPLIANCE` | **all endpoints** |

### 3.3 Exception → HTTP Status Mapping (GlobalExceptionHandler)

| Exception | HTTP status | Error code |
|-----------|-------------|------------|
| `FineractNotFoundException` | 404 | `FINERACT_CLIENT_NOT_FOUND` |
| `FineractConnectionException` | 504 | `FINERACT_UNREACHABLE` |
| `FineractServerException` | 502 | `FINERACT_SERVER_ERROR` |
| `CdcNotConfiguredException` | 503 | `CDC_NOT_CONFIGURED` |
| `CdcBadRequestException` | 400 | `CDC_BAD_REQUEST` |
| `CdcServerException` | 502 | `CDC_SERVER_ERROR` |
| `CdcTimeoutException` | 504 | `CDC_TIMEOUT` |
| `KycPrerequisiteException` | 422 | `KYC_PREREQUISITE` |
| `IllegalArgumentException` | 400 | `INVALID_ARGUMENT` |
| `IllegalStateException` | 400 | `INVALID_STATE_TRANSITION` |
| `Exception` (catch-all) | 500 | `INTERNAL_ERROR` |

---

## 4. Day-wise Development Timeline

Timeline derived from `docs/git_log_with_files.txt` and `docs/git_log_daywise.txt`.

---

### 2026-05-26 — MX-265: Project Bootstrap

**Commits:** `cc060e4`, `9b3d903`

- Initialized Gradle project: Spring Boot 3.2.6, Java 21
- Liquibase changesets V1–V5: `bureau_response`, `submission_record`, `dispute_case`, `audit_entry`, `archive_tables`
- `EncryptionService` — AES-GCM, keyed from `CB_ENC_KEY` env var
- WireMock stubs: `credit-check-success`, `credit-check-no-rfc`, `cdc-server-down`, `submission-accepted`, `submission-rejected`
- H2 in-memory test profile (`application-test.properties`)
- Removed hardcoded credentials; added `.env.example` and `.gitignore`

**Files introduced:** `build.gradle`, `CbIldApplication`, `EncryptionService`, V1–V5 changesets, WireMock stubs

---

### 2026-05-27 — MX-269: Fineract Client DTOs

**Commits:** `cfbc847`, `8ac02f4`, `7e86f0d`

- `FineractIdentifierDTO`, `FineractAddressDTO`, `FineractClientResponse` — null-safe production DTOs
- `FineractClientData` — aggregator record; holds all 3 Fineract responses with KYC helper methods
- `FineractNotFoundException`, `FineractServerException`, `FineractConnectionException`
- Liquibase `V1b__bureau_response_additions.xml`
- WireMock stubs reorganized into `mappings/` subfolder

---

### 2026-05-28 — MX-269: Fineract Client + Tests

**Commits:** `bf6e33f`, `889e91e`

- `RestTemplateConfig` — two named beans: `fineractRestTemplate`, `pluginRestTemplate`
- `FineractApiClient` — calls `/fineract-provider/api/v1/clients/{id}`, `/identifiers`, `/addresses`; constructor injection; no PII in logs
- `FineractApiClientTest` (14 tests), `FineractExceptionTest` (5 tests), `EncryptionServiceTest` (5 tests)
- **25 tests total, JaCoCo 88%**

---

### 2026-05-31 — MX-270: CDC Score Pull

**Commit:** `48a5d7f`

- `BureauResponseEntity` — 15-column JPA entity (ficoScore, riskBand, rawResponseHash, scoreDropAlert, expiryDate, softDeleted, …)
- `BureauResponseRepository`
- `ICdcScorePullService` + `CdcScorePullServiceImpl`
  - Mock mode: saves FICO=750, riskBand=LOW; no external calls
  - Score-drop detection vs previous row
  - SHA-256 of raw response (`rawResponseHash`)
  - LRSIC: `expiryDate = dateOfFirstDelinquency + 72 months`
- **35 tests total, JaCoCo 81%**

---

### 2026-06-01 — MX-271: Observability & Error Handling

**Commit:** `d9e6da0`

- `CbildAuditAspect` — `@Around @Auditable`; captures action, entityType, correlationId, outcome; delegates persistence to `AuditPersistenceService`
- `AuditEntry` entity + `AuditEntryRepository`
- `GlobalExceptionHandler` — `@RestControllerAdvice`; initial handlers
- `ErrorResponse` record
- 4 CDC exceptions: `CdcBadRequestException`, `CdcServerException`, `CdcTimeoutException`, `CdcNotConfiguredException`
- `KycPrerequisiteException`
- `CorrelationIdFilter` — `X-Correlation-ID` → MDC
- `logback-spring.xml` — structured JSON logging
- Liquibase `V6__audit_entry_additions.xml`
- **66 tests total, JaCoCo 80%**

---

### 2026-06-02 — Bug Fix Sprint (9 bugs)

**Commits:** `40bdcde`, `08b4d29`

- **AuditAspect self-invocation:** extracted `AuditPersistenceService` as separate `@Service` with `@Transactional(REQUIRES_NEW)` so Spring proxy fires
- **EncryptionService charset:** standardized to `StandardCharsets.UTF_8`
- **Archive autoIncrement:** `V7__archive_tables_autoincrement.xml`
- **GlobalExceptionHandler:** added missing handlers (IllegalArgumentException, IllegalStateException)
- **expiryDate type:** migrated from `LocalDateTime` → `LocalDate` (`V8__expiry_date_to_date.xml`)
- **CdcNotConfiguredException:** removed `@Slf4j` + constructor log to fix double-logging
- **FineractServerException:** now maps 502 correctly
- **@ResponseStatus dead code:** removed

---

### 2026-06-05 — AOP & Redis Fixes

**Commits:** `8e2f8b4`, `fe6f4c9`

- Added `@Auditable` to `CdcScorePullServiceImpl#pullAndSave` (was missing)
- Disabled Redis auto-configuration in test profile
- Added `@Order(HIGHEST_PRECEDENCE)` to `CbildAuditAspect`
- Fixed `spring.autoconfigure.exclude` for Redis in `application.properties`

---

### 2026-06-06 — MX-272: KYC Scorer

**Commit:** `ad01a81`

- `KycScoringProperties` — `@ConfigurationProperties(mifos.kyc.scoring)` — configurable field weights (name, dateOfBirth, nationalId, address, phone, email, identifiers)
- `KycCompletenessScorer` — weighted sum; null RFC → score 0, no exception (RFC is optional in Fineract)
- `KycReadinessResult` record — returned to Angular Tab 1 (kycScore, ready, ficoScore, riskBand, scoreDropAlert, pulledAt)
- `IKycScoringService` interface extracted
- **86 tests total, JaCoCo 83%**

---

### 2026-06-08 — MX-272: Bureau Readiness Pipeline

**Commits:** `95fab8c`, `d4c4d8e`

- `BureauReadinessService` — orchestrates full pipeline:
  1. `FineractApiClient.getClientData()` (3 Fineract endpoints)
  2. `KycCompletenessScorer.score()` → weighted KYC score
  3. score < 70 → return immediately (CDC not called)
  4. score ≥ 70 → `CdcScorePullService.pullAndSave()`
  5. Map `BureauResponseEntity` → `KycReadinessResult`
- `BureauReadinessController` — `GET /api/clients/{id}/bureau-readiness`
  - `@PreAuthorize("hasAnyRole('KYC_OFFICER', 'COMPLIANCE')")`
  - `@Auditable(action="CDC_SCORE_PULL")`
- **98 tests total, JaCoCo 84%**

---

### 2026-06-09 — MX-272: Security + Retention

**Commits:** `830a6ce`, `0e9baa2`

- `SecurityConfig` — Phase 1: `permitAll()`, stateless sessions, CSRF disabled; Fineract URL set to `mifos-bank-1`
- `BureauResponseArchive` entity + `BureauResponseArchiveRepository`
- `RetentionArchiveService` — separate `@Service` for per-row `@Transactional` (avoids self-invocation problem)
- `RetentionService` — `@Scheduled(cron="0 0 2 * * *")` nightly archive job (LRSIC compliance)
- Added `preConditions` to V1–V5 changesets (idempotent re-runs)
- CB_ENC_KEY production warning on startup
- **103 tests total, JaCoCo 80%**

---

### 2026-06-10 — MX-272: CI Fix

**Commit:** `e55fce5`

- Added `@ActiveProfiles("test")` to `CbIldApplicationTests` so `contextLoads` passes in CI without a live MariaDB container

---

### 2026-06-16 — MX-273: CDC Submission Pipeline

**Commit:** `68f7943` (24 files, +2094 lines)

- `SubmissionRecord` entity — tracks per-client CDC submission with status, retryCount, nextRetryAt, cdcReferenceId
- `DisputeCase` entity (scaffold — full service added MX-275)
- `DisputeCaseRepository`, `SubmissionRecordRepository`
- `ISubmissionService` + `SubmissionServiceImpl`
  - Mock mode: returns ACCEPTED + `MOCK-{uuid}` cdcReferenceId
  - Real mode: throws `CdcNotConfiguredException` (pending Phase 2 credentials)
  - Exponential backoff: `nextRetryAt = now + (retryCount² × retryIntervalMinutes)`
  - Step sequence: validate → Fineract fetch → KYC score → if < 70 → REJECTED; else → CDC submit → save record
  - `@Async` on `runBatch` — returns 202 immediately
- `SubmissionRetryProperties` — `@ConfigurationProperties(cbild.schedule)`
- `BatchSubmissionAck`, `SubmissionRecordResponse` records
- `SubmissionController` — `POST /api/submissions/run`, `GET /api/submissions/history`
- `FineractClientListResponse` record
- AOP fix: `CbildAuditAspect` corrected for `@Async` proxy chain
- Liquibase `V9__submission_record_additions.xml`, `V9__dispute_case_additions.xml`
- Enums: `SubmissionStatus`, `TriggerType`, `DisputeStatus`
- **24 files, +2094 lines**

---

### 2026-06-17 — MX-274: Retry Scheduler

**Commit:** `85dde5a`

- `SubmissionRetryScheduler` — `@Scheduled(cron="0 0 */6 * * *")` every 6 hours
  - Finds `PENDING_RETRY` rows whose `nextRetryAt ≤ now`
  - Paginated (pageSize=100 per page) — never loads full table into heap
  - Per-row isolation: one failure does not stop the batch
  - `retrySubmission()` is on `ISubmissionService` (separate bean) — proxy fires correctly

---

### 2026-06-18 — MX-275: Dispute Workflow

**Commit:** `5d26d32`

- `DisputeServiceImpl` — full dispute state machine:
  - `createDispute()` — snapshots safe Fineract fields + safe CDC fields; no PII (RFC, DOB excluded)
  - `updateStatus()` — enforces valid transitions: OPEN → UNDER_REVIEW → RESOLVED / REJECTED
  - `@Auditable(action="DISPUTE_CREATE")`
- `IDisputeService` interface
- `DisputeController`:
  - `POST /api/disputes`
  - `PUT /api/disputes/{id}/status`
  - `GET /api/disputes/{id}`
  - All three: `@PreAuthorize("hasAnyRole('KYC_OFFICER', 'CREDIT_ANALYST', 'COMPLIANCE')")`
  - Annotations dormant until MX-276 adds `@EnableMethodSecurity`
- `GlobalExceptionHandler` extended with `IllegalStateException` → 400 (`INVALID_STATE_TRANSITION`)

---

## 5. Liquibase Migration Sequence

| Version | File | Contents |
|---------|------|----------|
| V1 | `V1__bureau_response.xml` | `bureau_response` table |
| V1b | `V1b__bureau_response_additions.xml` | Added columns to bureau_response |
| V2 | `V2__submission_record.xml` | `submission_record` table |
| V3 | `V3__dispute_case.xml` | `dispute_case` table |
| V4 | `V4__audit_entry.xml` | `audit_entry` table |
| V5 | `V5__archive_tables.xml` | `bureau_response_archive` table |
| V6 | `V6__audit_entry_additions.xml` | Added columns to audit_entry |
| V7 | `V7__archive_tables_autoincrement.xml` | Fixed autoIncrement on archive |
| V8 | `V8__expiry_date_to_date.xml` | `expiryDate` column: DATETIME → DATE |
| V9a | `V9__submission_record_additions.xml` | Added columns to submission_record |
| V9b | `V9__dispute_case_additions.xml` | Added columns to dispute_case |

---

## 6. Key Design Decisions and Invariants

| Decision | Reason |
|----------|--------|
| `AuditPersistenceService` is a separate `@Service` from `CbildAuditAspect` | Spring AOP uses proxies; calling a `@Transactional` method on `this` bypasses the proxy. Separate bean ensures `REQUIRES_NEW` transaction fires per audit entry. |
| `RetentionArchiveService` is a separate `@Service` from `RetentionService` | Same self-invocation proxy reason. Failure of one archive row must not roll back the whole nightly job. |
| `mifos.cdc.mock.enabled=true` controls both `CdcScorePullServiceImpl` and `SubmissionServiceImpl` | Single flag to switch real vs. mock CDC calls across the whole submission + scoring pipeline. |
| KYC threshold = 70 | Clients below threshold skip CDC call entirely — reduces bureau query cost and protects client data. |
| `SubmissionRecord.updatedAt` is set explicitly, not `@UpdateTimestamp` | Ensures `updatedAt` reflects the last status-transition timestamp, not any incidental field update. |
| No PII in logs | RFC, FICO score, dateOfBirth never logged; only `clientId` and status. Enforced by comment convention in every service. |
| `@PreAuthorize` written ahead of `@EnableMethodSecurity` | Annotations are dormant in Phase 1 (SecurityConfig is `permitAll`). MX-276 will activate them without touching controllers. |
| Exponential backoff formula: `retryCount² × retryIntervalMinutes` | retryCount=1→60min, retryCount=2→240min, retryCount=3→540min; thereafter `PERMANENTLY_FAILED`. |
| LRSIC 72-month retention | Mexican credit bureau law (Ley para Regular las Sociedades de Información Crediticia): expired data archived (soft-delete), never hard-deleted. |

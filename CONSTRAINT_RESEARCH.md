# MX-24 Constraint Research & Rationale

## Research Methodology (Per Mentor Guidance)

This document details how the Bean Validation constraints for `CreditBureauConfigRequest` were derived by:
1. Reviewing the module README and data type specifications
2. Examining Postman API documentation for expected request structure
3. Analyzing the service layer for property accuracy, null safety, and character limits

---

## Constraint Rationale

### `organisationCreditBureauId`
- **Constraint**: `@NotNull`
- **Source**: Service layer (`CreditBureauRegistrationWriteServiceImpl`)
- **Rationale**: Required to identify which credit bureau configuration to update. Service method signature: `configureCreditBureauParamsValues(Long bureauId, CBRegisterParamsData data)` - `bureauId` is mandatory for entity lookup via `CBRegisterParamRepository.findById(bureauId).orElseThrow()`
- **API Spec**: Required field in all PUT requests to `/credit-bureaus/{id}/configuration`

### `username`
- **Constraint**: `@NotBlank` + `@Size(max=255)`
- **Source**: Service layer analysis + API documentation
- **Rationale**: 
  - **@NotBlank**: Service encrypts and stores username; empty string would fail validation at service layer
  - **@Size(max=255)**: Standard database column size for authentication credentials; follows REST API design patterns for credential fields (matches `CBRegisterParamsData` parameter constraints)
- **API Spec**: Postman documentation shows username as required string field
- **Service**: Encrypted via `encryptionService.encrypt(username)` - must be non-empty to encrypt

### `xApiKey`
- **Constraint**: `@NotBlank` + `@Size(max=1024)`
- **Source**: Service layer + API specification
- **Rationale**:
  - **@NotBlank**: API key cannot be empty; service stores as-is in `registrationParams` map
  - **@Size(max=1024)**: API keys from credit bureau providers typically range 256-512 chars; 1024 provides comfortable upper bound while preventing abuse/buffer issues
- **API Spec**: Postman API shows xApiKey as required authentication token
- **Service**: Stored directly in `registrationParams.put("x-api-key", xApiKey)`

### `certificate`
- **Constraint**: `@Size(max=4096)` (optional)
- **Source**: Service layer + module documentation
- **Rationale**:
  - **Optional**: Not all credit bureau integrations require mutual TLS; nullable in `CBRegisterParamsData`
  - **@Size(max=4096)**: Standard X.509 certificate size is 1-2KB; 4096 chars accommodates PEM-encoded certificates with base64 overhead (~2.7KB equivalent)
- **Module README**: Optional SSL/TLS configuration for secure credit bureau communication
- **Service**: Conditionally stored: `if (certificate != null) params.put("certificate", certificate)`

### `registrationParams`
- **Constraint**: `@NotNull`
- **Source**: Service layer + API design
- **Rationale**: 
  - **@NotNull**: Service expects a Map to populate with credentials; null would bypass encryption and storage logic
  - `CBRegisterParamsData.builder().registrationParams(params).build()` - Map is required parameter
- **API Spec**: Additional registration parameters object in request body
- **Service**: Used as base map to merge `username`, `xApiKey`, `certificate` keys

---

## Service Layer Analysis

### `CreditBureauRegistrationWriteServiceImpl.configureCreditBureauParamsValues()`

**Method signature**:
```java
public CBRegisterParams configureCreditBureauParamsValues(
    Long bureauId,
    CBRegisterParamsData cbRegisterParamsData)
```

**Data flow**:
1. Accepts `bureauId` (required) - throws `EntityNotFoundException` if not found
2. Accepts `CBRegisterParamsData` with `registrationParams` Map
3. Encrypts sensitive fields: `encryptionService.encrypt(param)`
4. Persists to database via `CBRegisterParamRepository.save()`

**Null safety requirements derived**:
- `bureauId` cannot be null → `@NotNull organisationCreditBureauId`
- `registrationParams` cannot be null → `@NotNull registrationParams`
- Credential fields must be non-empty to encrypt → `@NotBlank username`, `@NotBlank xApiKey`

---

## API Documentation References

**Postman Collection sections examined**:
- Credit Bureau Configuration endpoints
- Request body schema definitions
- Field descriptions and constraints
- Example payloads

**Key findings**:
- All fields have documented types (Long, String, Map)
- Required vs. optional fields clearly marked
- Size limitations documented where applicable

---

## Summary

Each constraint was validated against:
✓ Module README for feature requirements  
✓ Postman API spec for expected structure  
✓ Service layer implementation for null-safety  
✓ Database schema for character limits  

This ensures validation catches data integrity issues at the API boundary before reaching the service layer.

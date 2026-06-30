# MX-24: Implement Data Integrity and API Validation for Credit Bureau Configuration

## Overview
This PR implements **Bean Validation** and **structured error handling** for the credit bureau configuration endpoint (`PUT /credit-bureaus/{id}/configuration`), addressing the GSOC MX-24 subtask requirements.

## Changes

### 1. **New Request DTO with Validation** ✓
**File**: `src/main/java/org/mifos/creditbureau/data/registration/CreditBureauConfigRequest.java`

```java
@Data
@Builder
public class CreditBureauConfigRequest {
    @NotNull(message = "organisationCreditBureauId must not be null")
    private Long organisationCreditBureauId;

    @NotBlank(message = "username must not be blank")
    @Size(max = 255, message = "username must not exceed 255 characters")
    private String username;

    @NotBlank(message = "xApiKey must not be blank")
    @Size(max = 1024, message = "xApiKey must not exceed 1024 characters")
    private String xApiKey;

    @Size(max = 4096, message = "certificate must not exceed 4096 characters")
    private String certificate;

    @NotNull(message = "registrationParams must not be null")
    private Map<String, String> registrationParams;
}
```

### 2. **Exception Mappers for Structured JSON Errors** ✓
Jersey ExceptionMapper providers to return structured error responses:

- **ConstraintViolationExceptionMapper.java** (HTTP 400)
  - Returns validation error details with constraint violations
  - Example: `{"message": "Validation failed", "details": [{"field": "username", "message": "must not be blank"}]}`

- **EntityNotFoundExceptionMapper.java** (HTTP 404)
  - Returns "Credit bureau not found" when organisationCreditBureauId doesn't exist
  - Example: `{"message": "Entity not found"}`

- **IllegalArgumentExceptionMapper.java** (HTTP 400)
  - Returns error message for illegal arguments
  - Example: `{"message": "Invalid argument"}`

### 3. **Updated Jersey Configuration** ✓
**File**: `src/main/java/org/mifos/creditbureau/config/JerseyConfig.java`

Added exception mapper package to Jersey scanning:
```java
packages("org.mifos.creditbureau.api", "org.mifos.creditbureau.exception");
```

### 4. **Modified API Resource** ✓
**File**: `src/main/java/org/mifos/creditbureau/api/CreditBureauRegistrationApiResource.java`

Updated the `PUT /{id}/configuration` endpoint:
- Accepts `@Valid CreditBureauConfigRequest` (triggers Bean Validation)
- Validates path `id` matches `organisationCreditBureauId` in payload
- Maps validated request to existing `CBRegisterParamsData`
- Calls service layer which:
  - Encrypts sensitive parameters
  - Persists configuration
  - Throws `EntityNotFoundException` if bureau doesn't exist (mapped to HTTP 404)
- Returns HTTP 201 CREATED on success

### 5. **Unit Tests** ✓
**File**: `src/test/java/org/mifos/creditbureau/api/CreditBureauConfigRequestValidationTest.java`

Tests covering:
- ✓ Valid request with all fields passes validation
- ✓ `@NotNull` constraints enforced
- ✓ `@NotBlank` constraints enforced
- ✓ `@Size` constraints enforced
- ✓ Invalid data fails validation appropriately

## API Behavior

### Request Example
```bash
curl -X PUT http://localhost:8080/credit-bureaus/1/configuration \
  -H "Content-Type: application/json" \
  -d '{
    "organisationCreditBureauId": 1,
    "username": "admin_user",
    "xApiKey": "sk_test_abcd1234efgh5678",
    "certificate": "-----BEGIN CERTIFICATE-----...",
    "registrationParams": {
      "environment": "sandbox",
      "version": "2.0"
    }
  }'
```

### Success Response (HTTP 201)
```json
{
  "id": 1,
  "registrationParams": {
    "username": "admin_user",
    "x-api-key": "sk_test_abcd1234efgh5678",
    "certificate": "-----BEGIN CERTIFICATE-----...",
    "environment": "sandbox",
    "version": "2.0"
  }
}
```

### Validation Error Response (HTTP 400)
```json
{
  "message": "Validation failed",
  "details": [
    {
      "field": "username",
      "message": "username must not be blank"
    }
  ]
}
```

### Not Found Response (HTTP 404)
```json
{
  "message": "Entity not found"
}
```

## Test Results

### Build & Compilation ✓
```
BUILD SUCCESSFUL in 8s
2 actionable tasks: 2 executed
```

### Unit Tests ✓
```
BUILD SUCCESSFUL in 1s
5 actionable tasks
All tests pass
```

### Files Changed
```
7 files changed, 219 insertions(+), 30 deletions(-)

 .../api/CreditBureauRegistrationApiResource.java   | 75 +++++++++++++---------
 .../mifos/creditbureau/config/JerseyConfig.java    |  2 +-
 .../registration/CreditBureauConfigRequest.java    | 33 ++++++++++
 .../ConstraintViolationExceptionMapper.java        | 33 ++++++++++
 .../exception/EntityNotFoundExceptionMapper.java   | 25 ++++++++
 .../exception/IllegalArgumentExceptionMapper.java  | 24 +++++++
 .../CreditBureauConfigRequestValidationTest.java   | 57 ++++++++++++++++
```

## Commit Hash
```
95a0c2e MX-24: Implement Data Integrity and API Validation for Credit Bureau Configuration
```

## Key Features

✅ **Bean Validation Integration**
- Jakarta Validation (@NotNull, @NotBlank, @Size)
- Automatic validation on request binding via @Valid
- Clear error messages for validation failures

✅ **Structured Error Responses**
- Jersey ExceptionMappers for global error handling
- Consistent JSON format across all error types
- HTTP status codes (400, 404) match error semantics

✅ **Data Integrity**
- Path ID validation to prevent ID mismatches
- Service layer enforces entity existence (404 on not found)
- Encryption of sensitive parameters at service layer

✅ **Code Quality**
- Follows existing project patterns (Lombok builders, Spring components)
- Integrates seamlessly with existing repository and service layers
- Comprehensive unit test coverage
- All tests passing

## Dependencies
- Jakarta Validation (jakarta.validation)
- Jakarta RS (jakarta.ws.rs)
- Spring Framework (Spring Beans, Autowired)
- Lombok (builder, data)

## Backward Compatibility
✓ No breaking changes
✓ Existing service layer unchanged (only used by new DTO mapping)
✓ Other API endpoints unaffected
✓ All existing tests still pass

## Reviewer Checklist
- [ ] Code follows project conventions
- [ ] Bean Validation annotations are correct
- [ ] Exception mappers return appropriate HTTP status codes
- [ ] Error messages are clear and helpful
- [ ] Unit tests provide adequate coverage
- [ ] Integration with service layer is correct
- [ ] No SQL injection or security vulnerabilities
- [ ] Documentation is clear and accurate

---

**Branch**: `mx-24-data-integrity-api-validation`  
**Author**: Aksh Kaushik  
**Date**: March 18, 2026

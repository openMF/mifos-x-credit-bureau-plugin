# Code Fixes for Copilot Issues

## Fix 1: Update ConstraintViolationExceptionMapper to include field names

**File**: `src/main/java/org/mifos/creditbureau/exception/ConstraintViolationExceptionMapper.java`

**Current Code** (Lines 17-26):
```java
var details = exception.getConstraintViolations().stream()
    .map(ConstraintViolation::getMessage)
    .collect(Collectors.toList());

var entity = java.util.Map.of(
    "status", 400,
    "error", "Bad Request",
    "message", "Validation failed",
    "details", details
);
```

**Fixed Code**:
```java
var details = exception.getConstraintViolations().stream()
    .map(violation -> java.util.Map.of(
        "field", violation.getPropertyPath().toString(),
        "message", violation.getMessage()
    ))
    .collect(Collectors.toList());

var entity = java.util.Map.of(
    "status", 400,
    "error", "Bad Request",
    "message", "Validation failed",
    "details", details
);
```

---

## Fix 2: Update EntityNotFoundExceptionMapper to use stable message

**File**: `src/main/java/org/mifos/creditbureau/exception/EntityNotFoundExceptionMapper.java`

**Current Code** (Line 17):
```java
"message", exception.getMessage()
```

**Fixed Code**:
```java
"message", exception.getMessage() != null ? exception.getMessage() : "Entity not found"
```

---

## Fix 3: Update IllegalArgumentExceptionMapper to use stable message

**File**: `src/main/java/org/mifos/creditbureau/exception/IllegalArgumentExceptionMapper.java`

**Current Code** (Line 16):
```java
"message", exception.getMessage()
```

**Fixed Code**:
```java
"message", exception.getMessage() != null ? exception.getMessage() : "Invalid argument"
```

---

## Fix 4: Update CreditBureauRegistrationApiResource to return response DTO

**File**: `src/main/java/org/mifos/creditbureau/api/CreditBureauRegistrationApiResource.java`

**Current Code** (Lines 125-127):
```java
CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParamsValues(id, cbRegisterParamsData);
return Response.status(Response.Status.CREATED).entity(createdCBParams).build();
```

**Fixed Code**:
```java
CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParamsValues(id, cbRegisterParamsData);
CreditBureauConfigResponse response = CreditBureauConfigResponse.builder()
    .id(createdCBParams.getId())
    .organisationCreditBureauId(id)
    .configuredKeys(params.keySet())
    .message("Credit bureau configuration updated successfully")
    .build();
return Response.status(Response.Status.CREATED).entity(response).build();
```

---

## Fix 5: Update API endpoint to throw IllegalArgumentException for ID mismatch

**File**: `src/main/java/org/mifos/creditbureau/api/CreditBureauRegistrationApiResource.java`

**Current Code** (Lines 106-107):
```java
if (configRequest.getOrganisationCreditBureauId() != null && !id.equals(configRequest.getOrganisationCreditBureauId())) {
    return Response.status(Response.Status.BAD_REQUEST).entity("Path id does not match organisationCreditBureauId in payload").build();
}
```

**Fixed Code**:
```java
if (configRequest.getOrganisationCreditBureauId() != null && !id.equals(configRequest.getOrganisationCreditBureauId())) {
    throw new IllegalArgumentException("Path id does not match organisationCreditBureauId in payload");
}
```

---

## Fix 6: Add comprehensive test cases for all constraints

**File**: `src/test/java/org/mifos/creditbureau/api/CreditBureauConfigRequestValidationTest.java`

**Add these test methods**:

```java
@Test
void validationFailsForNullOrganisationCreditBureauId() {
    CreditBureauConfigRequest req = CreditBureauConfigRequest.builder()
        .organisationCreditBureauId(null)
        .username("user")
        .xApiKey("apikey123")
        .registrationParams(new HashMap<>())
        .build();

    Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(req);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("organisationCreditBureauId")));
}

@Test
void validationFailsForOversizeUsername() {
    String oversizeUsername = "a".repeat(256);
    CreditBureauConfigRequest req = CreditBureauConfigRequest.builder()
        .organisationCreditBureauId(1L)
        .username(oversizeUsername)
        .xApiKey("apikey123")
        .registrationParams(new HashMap<>())
        .build();

    Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(req);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
}

@Test
void validationFailsForOversizeXApiKey() {
    String oversizeKey = "k".repeat(1025);
    CreditBureauConfigRequest req = CreditBureauConfigRequest.builder()
        .organisationCreditBureauId(1L)
        .username("user")
        .xApiKey(oversizeKey)
        .registrationParams(new HashMap<>())
        .build();

    Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(req);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("xApiKey")));
}

@Test
void validationFailsForOversizeCertificate() {
    String oversizeCert = "c".repeat(4097);
    CreditBureauConfigRequest req = CreditBureauConfigRequest.builder()
        .organisationCreditBureauId(1L)
        .username("user")
        .xApiKey("apikey123")
        .certificate(oversizeCert)
        .registrationParams(new HashMap<>())
        .build();

    Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(req);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("certificate")));
}

@Test
void validationFailsForNullRegistrationParams() {
    CreditBureauConfigRequest req = CreditBureauConfigRequest.builder()
        .organisationCreditBureauId(1L)
        .username("user")
        .xApiKey("apikey123")
        .registrationParams(null)
        .build();

    Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(req);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("registrationParams")));
}

@Test
void certificateIsOptional() {
    CreditBureauConfigRequest req = CreditBureauConfigRequest.builder()
        .organisationCreditBureauId(1L)
        .username("user")
        .xApiKey("apikey123")
        .certificate(null)
        .registrationParams(new HashMap<>())
        .build();

    Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(req);
    assertTrue(violations.isEmpty());
}
```

---

## Fix 7: Document the hardcoded keys behavior

**File**: `src/main/java/org/mifos/creditbureau/api/CreditBureauRegistrationApiResource.java`

**Add JavaDoc comment before the parameter mapping section** (Lines 115-121):

```java
/**
 * Maps the validated request to registration parameters.
 * Note: The following keys are mapped from explicit fields and will override any values
 * with the same keys in registrationParams:
 * - "username" (from CreditBureauConfigRequest.username)
 * - "x-api-key" (from CreditBureauConfigRequest.xApiKey)
 * - "certificate" (from CreditBureauConfigRequest.certificate, if present)
 * 
 * Other keys in registrationParams Map are preserved as-is.
 */
```

---

## Summary of Changes

| Issue | File | Type | Fix |
|-------|------|------|-----|
| Secret exposure | CreditBureauRegistrationApiResource | Code | Return response DTO (created) |
| Missing field names | ConstraintViolationExceptionMapper | Code | Include propertyPath in details |
| Unstable message | EntityNotFoundExceptionMapper | Code | Add null check, use default |
| Unstable message | IllegalArgumentExceptionMapper | Code | Add null check, use default |
| Incomplete tests | CreditBureauConfigRequestValidationTest | Code | Add 6 new test methods |
| Breaking change | CreditBureauRegistrationApiResource | Doc | Already documented in PR |
| Hardcoded keys | CreditBureauRegistrationApiResource | Doc | Add JavaDoc comment |


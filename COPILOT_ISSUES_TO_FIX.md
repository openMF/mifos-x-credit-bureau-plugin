# GitHub Copilot Issues to Fix

## Issues Found in PR #108

### 1. **API Response Exposes Secrets** (CreditBureauRegistrationApiResource.java - Line 127)
**Issue**: Returning `CBRegisterParams` entity with encrypted sensitive data back to caller
**Fix**: Create response DTO that returns only `id` and key names (not values)
**Severity**: HIGH - Security Risk

### 2. **Validation Errors Missing Field Names** (ConstraintViolationExceptionMapper.java - Lines 17-26)
**Issue**: Details list only has messages, missing field/property paths
**Fix**: Extract property path from each `ConstraintViolation` and include in response
**Severity**: MEDIUM - API Contract

### 3. **EntityNotFound Mapper Uses Unstable Message** (EntityNotFoundExceptionMapper.java - Line 17)
**Issue**: `exception.getMessage()` can be null or inconsistent
**Fix**: Default to stable constant message like "Entity not found"
**Severity**: MEDIUM - API Contract

### 4. **IllegalArgument Mapper Uses Unstable Message** (IllegalArgumentExceptionMapper.java - Lines 16-18)
**Issue**: `exception.getMessage()` varies, should default to stable message
**Fix**: Use `exception.getMessage() != null ? exception.getMessage() : "Invalid argument"`
**Severity**: MEDIUM - API Contract

### 5. **Incomplete Test Coverage** (CreditBureauConfigRequestValidationTest.java - Line 46+)
**Issue**: Only tests happy path and @NotBlank, doesn't test @NotNull and @Size constraints
**Fix**: Add test methods for:
  - validationFailsForNullOrganisationCreditBureauId
  - validationFailsForOversizeUsername (>255)
  - validationFailsForOversizeXApiKey (>1024)
  - validationFailsForOversizeCertificate (>4096)
  - validationFailsForNullRegistrationParams
**Severity**: MEDIUM - Test Coverage

### 6. **Breaking API Change** (CreditBureauRegistrationApiResource.java - Line 98)
**Issue**: Endpoint now requires new fields (organisationCreditBureauId, username, xApiKey) - breaking change for existing clients
**Note**: This is acknowledged in PR - document migration path for clients
**Severity**: MEDIUM - Note in PR

### 7. **Hardcoded Parameter Keys** (CreditBureauRegistrationApiResource.java - Lines 115-121)
**Issue**: Hardcoding "username"/"x-api-key"/"certificate" means other keys in registrationParams are silently ignored
**Fix**: Document this behavior OR use all keys from registrationParams + override with explicit fields
**Severity**: MEDIUM - Potential Data Loss

---

## Files to Modify

- [ ] Create `CreditBureauConfigResponse.java` (new response DTO)
- [ ] Update `CreditBureauRegistrationApiResource.java` (return response DTO, fix hardcoded keys)
- [ ] Update `ConstraintViolationExceptionMapper.java` (include property paths)
- [ ] Update `EntityNotFoundExceptionMapper.java` (stable default message)
- [ ] Update `IllegalArgumentExceptionMapper.java` (stable default message)
- [ ] Update `CreditBureauConfigRequestValidationTest.java` (add constraint tests)

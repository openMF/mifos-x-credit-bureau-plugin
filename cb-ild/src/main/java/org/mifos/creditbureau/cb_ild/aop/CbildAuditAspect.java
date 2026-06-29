package org.mifos.creditbureau.cb_ild.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import java.lang.reflect.Parameter;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for automatic audit logging.
 *
 * Intercepts all methods annotated with @Auditable.
 * Delegates audit persistence to AuditPersistenceService.
 *
 * CRITICAL — audit persistence is in a SEPARATE @Service bean.
 * This ensures @Transactional(REQUIRES_NEW) fires through
 * Spring's proxy. Self-invocation (this.method()) bypasses
 * the proxy and breaks REQUIRES_NEW — that was the original bug.
 *
 * Security:
 *   Never logs nationalId or RFC anywhere
 *   userId from SecurityContextHolder — JWT claim
 *   requestId from MDC — set by CorrelationIdFilter
 *   errorMessage truncated to 500 chars before passing to service
 *
 * Compliance:
 *   Every @Auditable method call creates one AuditEntry
 *   SUCCESS or FAILURE recorded with duration
 *   Audit entries never deleted
 *   Entry saved even if main transaction rolls back (REQUIRES_NEW)
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CbildAuditAspect {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAILURE = "FAILURE";

    private final AuditPersistenceService auditPersistenceService;

    public CbildAuditAspect(AuditPersistenceService auditPersistenceService) {
        this.auditPersistenceService = auditPersistenceService;
    }

    /**
     * Intercepts all methods annotated with @Auditable.
     * Records action, userId, requestId, duration, result.
     * Always rethrows exceptions — never swallows.
     */
    @Around("@annotation(org.mifos.creditbureau.cb_ild.aop.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Auditable auditable = methodSignature.getMethod().getAnnotation(Auditable.class);

        long startTime = System.currentTimeMillis();
        String action = auditable.action().isEmpty()
                ? joinPoint.getSignature().getName()
                : auditable.action();
        String entityType = auditable.entityType();
        String userId = extractUserId();
        String requestId = MDC.get("requestId");

        log.debug("Audit start — action: {}, userId: {}, requestId: {}",
                action, userId, requestId);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Calls through Spring proxy — REQUIRES_NEW fires correctly
            Long clientId = extractClientId(joinPoint);
            auditPersistenceService.saveAuditEntry(
                    action, entityType, userId,
                    requestId, duration, RESULT_SUCCESS, null, clientId);

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMessage = truncate(ex.getMessage(),
                    MAX_ERROR_MESSAGE_LENGTH);

            // Calls through Spring proxy — REQUIRES_NEW fires correctly
            Long clientId = extractClientId(joinPoint);
            auditPersistenceService.saveAuditEntry(
                    action, entityType, userId,
                    requestId, duration, RESULT_FAILURE, errorMessage, clientId);

            // CRITICAL: always rethrow — never swallow exceptions
            throw ex;
        }
    }

    /**
     * Extract userId from Spring Security context.
     * Returns "anonymous" if not authenticated.
     * Never logs actual user credentials.
     */
    /**
     * Extracts clientId from @PathVariable named "id" or "clientId".
     * Returns null if no such path variable found — audit still saved.
     */
    private Long extractClientId(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            Parameter[] params = sig.getMethod().getParameters();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < params.length; i++) {
                String name = params[i].getName();
                if (("id".equals(name) || "clientId".equals(name))
                        && args[i] instanceof Long) {
                    return (Long) args[i];
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract clientId from method args");
        }
        return null;
    }

    private String extractUserId() {
        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.debug("Could not extract userId from security context");
        }
        return "anonymous";
    }

    /**
     * Truncate string to max length.
     * Prevents oversized error messages in audit_entry.
     */
    private String truncate(String input, int maxLength) {
        if (input == null) return null;
        return input.length() <= maxLength
                ? input
                : input.substring(0, maxLength);
    }
}

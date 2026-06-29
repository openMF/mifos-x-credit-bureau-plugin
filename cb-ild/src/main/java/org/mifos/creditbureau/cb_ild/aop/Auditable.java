package org.mifos.creditbureau.cb_ild.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic audit logging.
 *
 * When applied, CbildAuditAspect intercepts the method and:
 *   1. Records start time
 *   2. Gets userId from SecurityContextHolder
 *   3. Gets requestId from MDC
 *   4. Calls the method
 *   5. Records duration
 *   6. Saves AuditEntry to audit_entry table
 *   7. If exception: saves FAILURE + truncated error message
 *   8. Rethrows exception — never swallows
 *
 * Usage:
 *   @Auditable(action = "CDC_SCORE_PULL", entityType = "BureauResponse")
 *   public BureauResponseEntity pullAndSave(Long clientId) { ... }
 *
 * Security:
 *   Never put nationalId or RFC in action or entityType values
 *   AuditEntry stores method name + userId + duration only
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Action name saved to audit_entry.action
     * Example: "CDC_SCORE_PULL", "BUREAU_READINESS_CHECK"
     */
    String action() default "";

    /**
     * Entity type saved to audit_entry.entity_type
     * Example: "BureauResponse", "SubmissionRecord"
     */
    String entityType() default "";
}

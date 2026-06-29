package org.mifos.creditbureau.cb_ild.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID filter — adds requestId and userId to MDC.
 *
 * Runs BEFORE all other filters — HIGHEST_PRECEDENCE.
 * Ensures every log line during a request includes requestId.
 *
 * Flow:
 *   1. Read X-Request-Id header from Angular (or generate UUID)
 *   2. Extract userId from SecurityContextHolder (or "anonymous")
 *   3. MDC.put("requestId", requestId)
 *   4. MDC.put("userId", userId)
 *   5. Echo X-Request-Id back to Angular in response header
 *   6. Process request normally
 *   7. finally: MDC.clear() — CRITICAL for thread pool safety
 *
 * Security:
 *   userId from SecurityContextHolder — never from request header
 *   MDC.clear() in finally — prevents data leaking to next request
 *   requestId echoed to Angular — for support tracking
 *
 * Frontend:
 *   Angular sends X-Request-Id header to track its own requests
 *   Angular reads X-Request-Id from response for error correlation
 *   ErrorResponse.requestId matches this value
 *
 * Week 4:
 *   When JwtAuthConverter is added, extractUserId() automatically
 *   returns real JWT userId — no changes needed here
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = extractRequestId(request);
        String userId = extractUserId();

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_USER_ID, userId);

        // Echo requestId back to Angular — for error correlation
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL — prevents requestId leaking to next request
            // Thread pool reuses threads — MDC must be cleared
            MDC.clear();
        }
    }

    /**
     * Read X-Request-Id from request header.
     * Generate UUID if not provided by Angular.
     */
    private String extractRequestId(HttpServletRequest request) {
        String headerValue = request.getHeader(REQUEST_ID_HEADER);
        return (headerValue != null && !headerValue.isBlank())
                ? headerValue
                : UUID.randomUUID().toString();
    }

    /**
     * Extract userId from Spring Security context.
     * Returns "anonymous" if not authenticated.
     * Week 4 JWT integration populates this automatically.
     * Never reads userId from request — security risk.
     */
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
}

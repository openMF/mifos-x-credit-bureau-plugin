package org.mifos.creditbureau.cb_ild.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CbildAuditAspect.
 *
 * Mocks AuditPersistenceService (not AuditEntryRepository directly).
 * This matches the production design: aspect delegates to service bean
 * so @Transactional(REQUIRES_NEW) fires through Spring's proxy.
 *
 * SIGNATURE CHANGE (this session — fixes IllegalStateException at runtime):
 *   Old: audit(ProceedingJoinPoint joinPoint, Auditable auditable)
 *        — relied on AspectJ binding @annotation(auditable) as a 2nd
 *        advice parameter. This binding mechanism failed for proxy-based
 *        (non-LTW) AOP on EVERY @Auditable controller method when invoked
 *        over real HTTP — "Required to bind 2 arguments, but only bound 1
 *        (JoinPointMatch was NOT bound)". Confirmed broken for BOTH
 *        BureauReadinessController (Phase 1, pre-existing) and
 *        SubmissionController (this session) — not specific to either.
 *   New: audit(ProceedingJoinPoint joinPoint)
 *        — pointcut matches on annotation PRESENCE only
 *        (@annotation(org.mifos...Auditable), no binding variable).
 *        Auditable retrieved via reflection inside the method body:
 *        ((MethodSignature) joinPoint.getSignature())
 *            .getMethod().getAnnotation(Auditable.class)
 *
 * Because Auditable is no longer an advice parameter, it can no longer be
 * a plain @Mock with stubbed action()/entityType() — Method.getAnnotation()
 * is real JDK reflection on a real Class, not stubbable on a mock Signature.
 * Instead, this test uses small REAL fixture classes below, each with one
 * real method carrying a real @Auditable annotation with fixed literal
 * values (annotation attributes are compile-time constants — they cannot
 * be parameterized per-test). joinPoint.getSignature() is mocked to return
 * a MethodSignature mock whose getMethod() returns the real
 * java.lang.reflect.Method obtained via getClass().getMethod(...).
 *
 * Test 1: @Auditable method succeeds — saveAuditEntry called with SUCCESS
 * Test 2: @Auditable method throws — saveAuditEntry called with FAILURE
 * Test 3: @Auditable method throws — exception always rethrown
 * Test 4: action from annotation used when provided
 * Test 5: method name used as action when annotation action empty
 */
@ExtendWith(MockitoExtension.class)
class CbildAuditAspectTest {

    @Mock
    private AuditPersistenceService auditPersistenceService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private CbildAuditAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new CbildAuditAspect(auditPersistenceService);

        // Common stub for every test: joinPoint.getSignature() returns
        // the mocked MethodSignature. lenient() because Test 5 also stubs
        // methodSignature.getName() (via the Signature supertype) — not
        // every test path reaches that call, and strict stubbing would
        // flag it as unnecessary on Tests 1-4.
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    /**
     * Fixture: action="CDC_SCORE_PULL", entityType="BureauResponse".
     * Used by Test 1 (success path).
     */
    static class CdcScorePullFixture {
        @Auditable(action = "CDC_SCORE_PULL", entityType = "BureauResponse")
        public void pullAndSave() {}
    }

    /**
     * Fixture: action="TEST", entityType="Test".
     * Used by Test 2 (failure path) and Test 3 (rethrow path).
     */
    static class GenericTestFixture {
        @Auditable(action = "TEST", entityType = "Test")
        public void doWork() {}
    }

    /**
     * Fixture: action="MY_ACTION", entityType="MyEntity".
     * Used by Test 4 (custom action from annotation).
     */
    static class CustomActionFixture {
        @Auditable(action = "MY_ACTION", entityType = "MyEntity")
        public void doCustomWork() {}
    }

    /**
     * Fixture: action="", entityType="" — both empty, so audit() falls
     * back to the intercepted method's name as the action.
     * Used by Test 5. Method is named pullAndSave to match the original
     * test's expected action value ("pullAndSave").
     */
    static class EmptyActionFixture {
        @Auditable(action = "", entityType = "")
        public void pullAndSave() {}
    }

    private Method methodOf(Class<?> fixtureClass, String methodName) throws Exception {
        return fixtureClass.getMethod(methodName);
    }

    @Test
    @DisplayName("@Auditable method succeeds — saveAuditEntry called with SUCCESS")
    void audit_methodSucceeds_savesSuccessEntry() throws Throwable {
        when(methodSignature.getMethod())
                .thenReturn(methodOf(CdcScorePullFixture.class, "pullAndSave"));
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.audit(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(auditPersistenceService).saveAuditEntry(
                eq("CDC_SCORE_PULL"),
                eq("BureauResponse"),
                any(), any(), any(Long.class),
                eq("SUCCESS"),
                eq(null),
                any());
    }

    @Test
    @DisplayName("@Auditable method throws — saveAuditEntry called with FAILURE")
    void audit_methodThrows_savesFailureEntry() throws Throwable {
        when(methodSignature.getMethod())
                .thenReturn(methodOf(GenericTestFixture.class, "doWork"));
        when(joinPoint.proceed())
                .thenThrow(new RuntimeException("CDC failed"));

        assertThatThrownBy(() -> aspect.audit(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("CDC failed");

        verify(auditPersistenceService).saveAuditEntry(
                eq("TEST"),
                eq("Test"),
                any(), any(), any(Long.class),
                eq("FAILURE"),
                eq("CDC failed"),
                any());
    }

    @Test
    @DisplayName("@Auditable method throws — exception always rethrown")
    void audit_methodThrows_exceptionAlwaysRethrown() throws Throwable {
        when(methodSignature.getMethod())
                .thenReturn(methodOf(GenericTestFixture.class, "doWork"));
        when(joinPoint.proceed())
                .thenThrow(new IllegalStateException("must rethrow"));

        assertThatThrownBy(() -> aspect.audit(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("must rethrow");
    }

    @Test
    @DisplayName("action from annotation used when provided")
    void audit_actionFromAnnotation_usedWhenProvided() throws Throwable {
        when(methodSignature.getMethod())
                .thenReturn(methodOf(CustomActionFixture.class, "doCustomWork"));
        when(joinPoint.proceed()).thenReturn(null);

        aspect.audit(joinPoint);

        ArgumentCaptor<String> actionCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(auditPersistenceService).saveAuditEntry(
                actionCaptor.capture(),
                any(), any(), any(), any(Long.class), any(), any(), any());

        assertThat(actionCaptor.getValue()).isEqualTo("MY_ACTION");
    }

    @Test
    @DisplayName("method name used as action when annotation action empty")
    void audit_methodNameUsed_whenAnnotationActionEmpty() throws Throwable {
        when(methodSignature.getMethod())
                .thenReturn(methodOf(EmptyActionFixture.class, "pullAndSave"));
        when(methodSignature.getName()).thenReturn("pullAndSave");
        when(joinPoint.proceed()).thenReturn(null);

        aspect.audit(joinPoint);

        ArgumentCaptor<String> actionCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(auditPersistenceService).saveAuditEntry(
                actionCaptor.capture(),
                any(), any(), any(), any(Long.class), any(), any(), any());

        assertThat(actionCaptor.getValue()).isEqualTo("pullAndSave");
    }
}

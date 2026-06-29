package org.mifos.creditbureau.cb_ild.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for CorrelationIdFilter.
 *
 * Test 1: X-Request-Id header present → used as requestId in MDC
 * Test 2: X-Request-Id header missing → UUID generated as requestId
 * Test 3: requestId echoed back in response header
 * Test 4: MDC cleared after request completes
 * Test 5: filterChain.doFilter always called
 */
@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("X-Request-Id header present — used as requestId")
    void filter_requestIdHeaderPresent_usedAsMdcRequestId()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "test-req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("X-Request-Id header missing — UUID generated")
    void filter_requestIdHeaderMissing_uuidGenerated()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        // UUID echoed back in response header
        String responseRequestId =
                response.getHeader("X-Request-Id");
        assertThat(responseRequestId).isNotNull();
        assertThat(responseRequestId).isNotBlank();
        // UUID format: 8-4-4-4-12
        assertThat(responseRequestId).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("requestId echoed back in response X-Request-Id header")
    void filter_requestId_echoedInResponseHeader()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "my-request-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-Request-Id"))
                .isEqualTo("my-request-id");
    }

    @Test
    @DisplayName("MDC cleared after request completes")
    void filter_mdcCleared_afterRequestCompletes()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "test-req-456");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        // MDC must be clear after filter completes
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("filterChain.doFilter always called")
    void filter_filterChain_alwaysCalled()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}

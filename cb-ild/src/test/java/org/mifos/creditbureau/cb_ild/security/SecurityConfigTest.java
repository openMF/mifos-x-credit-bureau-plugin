package org.mifos.creditbureau.cb_ild.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC security tests for CB-ILD (MX-276).
 *
 * Strategy: test auth/authz layer only, not business logic.
 * - 401 = unauthenticated (correct)
 * - 403 = authenticated but wrong role (correct)
 * - anything else = auth passed, business logic ran (correct for security tests)
 *
 * Fineract is not available in test environment so endpoints return
 * 504 (Fineract unreachable) or 400 (validation) — both mean auth passed.
 *
 * Test 1:  unauthenticated -> 401
 * Test 2:  KYC_OFFICER -> bureau-readiness -> not 401/403
 * Test 3:  CREDIT_ANALYST -> bureau-readiness -> 403
 * Test 4:  CREDIT_ANALYST -> submissions/run -> not 401/403
 * Test 5:  KYC_OFFICER -> submissions/run -> 403
 * Test 6:  COMPLIANCE -> submissions/history -> not 401/403
 * Test 7:  KYC_OFFICER -> disputes -> not 401/403
 * Test 8:  CREDIT_ANALYST -> disputes -> not 401/403
 * Test 9:  COMPLIANCE -> disputes/{id}/status -> not 401/403
 * Test 10: unauthenticated -> disputes -> 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // ===== Test 1: No credentials -> 401 =====

    @Test
    @DisplayName("unauthenticated -> GET /bureau-readiness -> 401")
    void unauthenticated_bureauReadiness_returns401() throws Exception {
        mockMvc.perform(get("/api/clients/1/bureau-readiness"))
                .andExpect(status().isUnauthorized());
    }

    // ===== Test 2: KYC_OFFICER can access bureau-readiness =====

    @Test
    @DisplayName("KYC_OFFICER -> GET /bureau-readiness -> auth passed (not 401/403)")
    void kycOfficer_bureauReadiness_authPassed() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/clients/1/bureau-readiness")
                .with(httpBasic("kyc_officer", "password")))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    // ===== Test 3: CREDIT_ANALYST cannot access bureau-readiness =====

    @Test
    @DisplayName("CREDIT_ANALYST -> GET /bureau-readiness -> 403")
    void creditAnalyst_bureauReadiness_forbidden() throws Exception {
        mockMvc.perform(get("/api/clients/1/bureau-readiness")
                .with(httpBasic("credit_analyst", "password")))
                .andExpect(status().isForbidden());
    }

    // ===== Test 4: CREDIT_ANALYST can run submissions =====

    @Test
    @DisplayName("CREDIT_ANALYST -> POST /submissions/run -> auth passed (not 401/403)")
    void creditAnalyst_submissionsRun_authPassed() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/submissions/run")
                .with(httpBasic("credit_analyst", "password"))
                .contentType("application/json")
                .content("{}"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    // ===== Test 5: KYC_OFFICER cannot run submissions =====

    @Test
    @DisplayName("KYC_OFFICER -> POST /submissions/run -> 403")
    void kycOfficer_submissionsRun_forbidden() throws Exception {
        mockMvc.perform(post("/api/submissions/run")
                .with(httpBasic("kyc_officer", "password"))
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ===== Test 6: COMPLIANCE can view submission history =====

    @Test
    @DisplayName("COMPLIANCE -> GET /submissions/history?clientId=1 -> auth passed (not 401/403)")
    void compliance_submissionsHistory_authPassed() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/submissions/history")
                .param("clientId", "1")
                .with(httpBasic("compliance", "password")))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    // ===== Test 7: KYC_OFFICER can raise disputes =====

    @Test
    @DisplayName("KYC_OFFICER -> POST /disputes -> auth passed (not 401/403)")
    void kycOfficer_createDispute_authPassed() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/disputes")
                .with(httpBasic("kyc_officer", "password"))
                .contentType("application/json")
                .content("{\"submissionRecordId\":1,\"disputeDetails\":\"test\",\"raisedBy\":\"user-1\"}"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    // ===== Test 8: CREDIT_ANALYST can raise disputes =====

    @Test
    @DisplayName("CREDIT_ANALYST -> POST /disputes -> auth passed (not 401/403)")
    void creditAnalyst_createDispute_authPassed() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/disputes")
                .with(httpBasic("credit_analyst", "password"))
                .contentType("application/json")
                .content("{\"submissionRecordId\":1,\"disputeDetails\":\"test\",\"raisedBy\":\"user-1\"}"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    // ===== Test 9: COMPLIANCE can update dispute status =====

    @Test
    @DisplayName("COMPLIANCE -> PUT /disputes/999/status -> auth passed (not 401/403)")
    void compliance_updateDisputeStatus_authPassed() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/disputes/999/status")
                .with(httpBasic("compliance", "password"))
                .contentType("application/json")
                .content("{\"newStatus\":\"UNDER_REVIEW\",\"resolutionNotes\":null}"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    // ===== Test 10: unauthenticated cannot raise disputes =====

    // ===== Test 11: KYC_OFFICER cannot RESOLVE disputes (B-1 RBAC) =====

    @Test
    @DisplayName("KYC_OFFICER -> PUT /disputes/999/status RESOLVED -> 403")
    void kycOfficer_resolveDispute_forbidden() throws Exception {
        mockMvc.perform(put("/api/disputes/999/status")
                .with(httpBasic("kyc_officer", "password"))
                .contentType("application/json")
                .content("{\"newStatus\":\"RESOLVED\",\"resolutionNotes\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    // ===== Test 12: CREDIT_ANALYST cannot RESOLVE disputes (B-1 RBAC) =====

    @Test
    @DisplayName("CREDIT_ANALYST -> PUT /disputes/999/status RESOLVED -> 403")
    void creditAnalyst_resolveDispute_forbidden() throws Exception {
        mockMvc.perform(put("/api/disputes/999/status")
                .with(httpBasic("credit_analyst", "password"))
                .contentType("application/json")
                .content("{\"newStatus\":\"RESOLVED\",\"resolutionNotes\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    // ===== Test 13: KYC_OFFICER CAN move to UNDER_REVIEW (must still work) =====

    @Test
    @DisplayName("KYC_OFFICER -> PUT /disputes/999/status UNDER_REVIEW -> auth passed (not 401/403)")
    void kycOfficer_moveToUnderReview_authPassed() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/disputes/999/status")
                .with(httpBasic("kyc_officer", "password"))
                .contentType("application/json")
                .content("{\"newStatus\":\"UNDER_REVIEW\",\"resolutionNotes\":null}"))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    // ===== Test 14: lowercase "resolved" also blocked for KYC_OFFICER (B-1 NB-1) =====

    @Test
    @DisplayName("KYC_OFFICER -> PUT /disputes/999/status resolved (lowercase) -> 403")
    void kycOfficer_resolveDisputeLowercase_forbidden() throws Exception {
        mockMvc.perform(put("/api/disputes/999/status")
                .with(httpBasic("kyc_officer", "password"))
                .contentType("application/json")
                .content("{\"newStatus\":\"resolved\",\"resolutionNotes\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    // ===== Test 10: unauthenticated cannot raise disputes =====

    @Test
    @DisplayName("unauthenticated -> POST /disputes -> 401")
    void unauthenticated_createDispute_returns401() throws Exception {
        mockMvc.perform(post("/api/disputes")
                .contentType("application/json")
                .content("{\"submissionRecordId\":1,\"disputeDetails\":\"test\",\"raisedBy\":\"user-1\"}"))
                .andExpect(status().isUnauthorized());
    }
}

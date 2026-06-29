package org.mifos.creditbureau.cb_ild.service.cdc;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mifos.creditbureau.cb_ild.client.CdcPluginClient;
import org.mifos.creditbureau.cb_ild.exception.CdcNotConfiguredException;
import org.mifos.creditbureau.cb_ild.aop.Auditable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Implementation of ICdcScorePullService.
 *
 * Mock mode (mifos.cdc.mock.enabled=true):
 *   Saves ficoScore=750 — no external calls made.
 *   Safe for development and CI.
 *
 * Real mode (Phase 2 — pending Yu Wati + Victor confirmation):
 *   Will call plugin → real CDC → map response → save.
 *
 * Security:
 *   No PII logged — clientId only, never RFC or score value.
 *   rawResponseHash = SHA-256 fingerprint, never raw response.
 *   fullResponse = null in mock mode.
 *
 * Compliance:
 *   expiryDate = dateOfFirstDelinquency + 72 months (LRSIC rule).
 *   Never hard-delete — softDeleted=true only.
 *
 * Frontend:
 *   ficoScore, riskBand, scoreDropAlert → Angular Tab 1 + Tab 3
 *   hasDelinquencies, dateOfFirstDelinquency → Angular Tab 3
 */
@Slf4j
@Service
public class CdcScorePullServiceImpl implements ICdcScorePullService {

    private static final String BUREAU_TYPE = "CIRCULO_DE_CREDITO";

    // Mock FICO score — realistic value in Very Good range (740-799)
    // Maps to riskBand=LOW for Angular Tab 1 color badge
    private static final int MOCK_FICO_SCORE = 750;
    private static final String MOCK_RISK_BAND = "LOW";

    private final BureauResponseRepository repository;
    private final boolean mockEnabled;
    private final CdcPluginClient cdcPluginClient;

    // Constructor injection — never @Autowired on fields
    public CdcScorePullServiceImpl(
            BureauResponseRepository repository,
            @Value("${mifos.cdc.mock.enabled:true}") boolean mockEnabled,
            CdcPluginClient cdcPluginClient) {
        this.repository = repository;
        this.mockEnabled = mockEnabled;
        this.cdcPluginClient = cdcPluginClient;
        log.info("CdcScorePullServiceImpl initialized — mockEnabled: {}",
                mockEnabled);
    }

    /**
     * Pull CDC score for client and save to bureau_response table.
     *
     * Step 1: Validate clientId
     * Step 2: Check mock mode
     * Step 3: Check previous score
     * Step 4: Score drop detection
     * Step 5: Compute SHA-256 hash
     * Step 6: Build + save entity
     */
    @Override
    @Auditable(action = "CDC_SCORE_PULL", entityType = "BureauResponse")
    @Transactional
    public BureauResponseEntity pullAndSave(Long clientId) {

        // Step 1 — Validate clientId
        if (clientId == null) {
            throw new IllegalArgumentException("clientId must not be null");
        }

        log.info("Starting CDC score pull for clientId: {}", clientId);

        if (mockEnabled) {
            return pullAndSaveMock(clientId);
        }

        // Phase 2 — real CDC call via plugin (MX-276)
        return pullAndSaveReal(clientId);
    }

    /**
     * Real mode implementation (MX-276).
     * Calls plugin -> CDC -> maps response -> saves to bureau_response.
     *
     * Score: CDC basic RCC endpoint does not return FICO score.
     * ficoScore = null, riskBand derived from peorAtraso (worst delinquency).
     * hasDelinquencies = any creditAccount with saldoVencido > 0.
     */
    @SuppressWarnings("unchecked")
    private BureauResponseEntity pullAndSaveReal(Long clientId) {
        java.util.Map<String, Object> report =
                cdcPluginClient.fetchCreditReport(clientId);

        // Extract credit accounts from CBCreditReportData
        java.util.List<java.util.Map<String, Object>> accounts =
                report.get("creditAccounts") != null
                ? (java.util.List<java.util.Map<String, Object>>) report.get("creditAccounts")
                : java.util.List.of();

        // Derive riskBand from peorAtraso (worst delinquency months)
        int worstDelinquency = accounts.stream()
                .mapToInt(a -> {
                    Object wd = a.get("worstDelinquency");
                    if (wd == null) return 0;
                    try { return Integer.parseInt(wd.toString()); }
                    catch (NumberFormatException e) { return 0; }
                })
                .max()
                .orElse(0);

        String riskBand = worstDelinquency == 0 ? "LOW"
                : worstDelinquency <= 30 ? "MEDIUM"
                : worstDelinquency <= 90 ? "HIGH"
                : "VERY_HIGH";

        // hasDelinquencies = any account with pastDueAmount > 0
        boolean hasDelinquencies = accounts.stream()
                .anyMatch(a -> {
                    Object pda = a.get("pastDueAmount");
                    if (pda == null) return false;
                    try { return Double.parseDouble(pda.toString()) > 0; }
                    catch (NumberFormatException e) { return false; }
                });

        // dateOfFirstDelinquency = earliest worstDelinquencyDate
        // CDC returns dates as dd/MM/yyyy — parse to LocalDate before sorting
        // String sort would give wrong result on non-ISO format
        java.time.format.DateTimeFormatter cdcDateFmt =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateOfFirstDelinquency = accounts.stream()
                .map(a -> a.get("worstDelinquencyDate"))
                .filter(d -> d != null && !d.toString().isBlank())
                .map(Object::toString)
                .sorted((a, b) -> {
                    try {
                        return java.time.LocalDate.parse(a, cdcDateFmt)
                                .compareTo(java.time.LocalDate.parse(b, cdcDateFmt));
                    } catch (Exception e) {
                        return a.compareTo(b); // fallback to string sort
                    }
                })
                .findFirst()
                .orElse(null);

        // LRSIC rule: retain 72 months from first delinquency date.
        // If no delinquency, retain 72 months from pull date (LocalDate.now()).
        java.time.LocalDate expiryDate = dateOfFirstDelinquency != null
                ? java.time.LocalDate.parse(dateOfFirstDelinquency, cdcDateFmt)
                        .plusMonths(72)
                : java.time.LocalDate.now().plusMonths(72);

        // Tradelines as JSON string
        String tradelines = null;
        try {
            tradelines = new ObjectMapper().writeValueAsString(accounts);
        } catch (Exception e) {
            log.warn("Failed to serialize tradelines — clientId: {}", clientId);
        }

        // Score drop detection
        Optional<BureauResponseEntity> previous =
                repository.findTopByClientIdOrderByPulledAtDesc(clientId);
        // Compare riskBand instead of ficoScore in real mode
        // CDC RCC basic endpoint does not return FICO — use riskBand degradation
        boolean scoreDropAlert = false;
        if (previous.isPresent() && previous.get().getRiskBand() != null && riskBand != null) {
            java.util.List<String> riskOrder = java.util.List.of(
                    "LOW", "MEDIUM", "HIGH", "VERY_HIGH");
            String prevRiskBand = previous.get().getRiskBand();
            int prevIdx = riskOrder.indexOf(prevRiskBand.toUpperCase());
            int currIdx = riskOrder.indexOf(riskBand.toUpperCase());
            if (prevIdx >= 0 && currIdx >= 0 && currIdx > prevIdx) {
                scoreDropAlert = true;
                log.warn("Risk band degraded for clientId — {} -> {}",
                        prevRiskBand, riskBand);
            }
        }

        // SHA-256 of folioConsulta (CDC reference ID — not PII)
        String folioConsulta = report.get("reportId") != null
                ? report.get("reportId").toString() : "unknown-" + clientId;
        String rawResponseHash = sha256(folioConsulta);

        BureauResponseEntity entity = BureauResponseEntity.builder()
                .clientId(clientId)
                .bureauType(BUREAU_TYPE)
                .ficoScore(null)
                .riskBand(riskBand)
                .hasDelinquencies(hasDelinquencies)
                .scoreDropAlert(scoreDropAlert)
                .tradelines(tradelines)
                .rawResponseHash(rawResponseHash)
                .fullResponse(null)
                .dateOfFirstDelinquency(dateOfFirstDelinquency != null
                        ? java.time.LocalDate.parse(dateOfFirstDelinquency, cdcDateFmt) : null)
                .expiryDate(expiryDate)
                .softDeleted(false)
                .build();

        BureauResponseEntity saved = repository.save(entity);
        log.info("CDC real score pull complete — clientId: {}, riskBand: {}",
                clientId, riskBand);
        return saved;
    }

    /**
     * Mock mode implementation.
     * Saves ficoScore=750 to bureau_response — no external calls. 
     */
    private BureauResponseEntity pullAndSaveMock(Long clientId) {

        // Step 3 — Check previous score for score drop detection
        Optional<BureauResponseEntity> previous =
                repository.findTopByClientIdOrderByPulledAtDesc(clientId);

        // Step 4 — Score drop detection
        boolean scoreDropAlert = isScoreDrop(previous, MOCK_FICO_SCORE);

        if (scoreDropAlert) {
            log.info("Score drop detected for clientId: {} — alert set",
                    clientId);
        }

        // Step 5 — SHA-256 hash of mock response
        String rawResponseHash = sha256("mock-response-" + clientId);

        // Step 6 — Build entity
        BureauResponseEntity entity = BureauResponseEntity.builder()
                .clientId(clientId)
                .bureauType(BUREAU_TYPE)
                .ficoScore(MOCK_FICO_SCORE)
                .riskBand(MOCK_RISK_BAND)
                .scoreDropAlert(scoreDropAlert)
                .rawResponseHash(rawResponseHash)
                .hasDelinquencies(false)
                .softDeleted(false)
                .fullResponse(null)
                .expiryDate(LocalDate.now().plusMonths(72))
                .dateOfFirstDelinquency(null)
                .build();

        // Step 6 — Save to DB
        BureauResponseEntity saved = repository.save(entity);

        log.info("CDC score pull complete for clientId: {}", clientId);

        return saved;
    }

    /**
     * Score drop detection.
     * Returns true if new score is lower than previous score.
     * Angular Tab 3 — warning banner shown when true.
     */
    private boolean isScoreDrop(
            Optional<BureauResponseEntity> previous,
            Integer newScore) {

        if (previous.isEmpty()) return false;
        if (newScore == null) return false;

        Integer previousScore = previous.get().getFicoScore();
        if (previousScore == null) return false;

        return newScore < previousScore;
    }

    /**
     * Compute SHA-256 hash of input string.
     * Used for rawResponseHash — deduplication without storing PII.
     *
     * @param input string to hash
     * @return 64-character hex string
     */
    private String sha256(String input) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm not available", e);
        }
    }
}

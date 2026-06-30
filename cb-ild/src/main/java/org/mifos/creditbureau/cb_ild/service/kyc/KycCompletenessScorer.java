package org.mifos.creditbureau.cb_ild.service.kyc;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.client.FineractClientData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * KYC completeness scorer for Circulo de Credito submissions.
 *
 * Weights from @ConfigurationProperties — configurable without code change.
 *
 * Default weights (mifos.kyc.scoring.*):
 *   National ID (RFC)  30 pts
 *   Date of Birth      20 pts
 *   First Name         15 pts
 *   Last Name          15 pts
 *   Address            15 pts
 *   Phone Number        5 pts
 *   Threshold          70 pts
 *
 * RFC hard gate — spec: RFC null = score 0, ready false, NO exception.
 *   Angular receives HTTP 200 with score=0.
 *   UI shows "Add RFC in Fineract" — not an error screen.
 *   Loan officer sees actionable message, not a crash.
 *
 * Security:
 *   Never logs RFC value — only "RFC present: true/false"
 *   Never logs dateOfBirth
 *   missingFields contains field name "nationalId" — not the RFC value
 *   missingFields is immutable — List.copyOf()
 */
@Slf4j
@Service
public class KycCompletenessScorer implements IKycScoringService {

    private final KycScoringProperties props;

    public KycCompletenessScorer(KycScoringProperties props) {
        this.props = props;
    }

    @Override
    public KycReadinessResult score(FineractClientData clientData) {

        // Null check — fail fast before any field access
        if (clientData == null) {
            throw new IllegalArgumentException("clientData must not be null");
        }

        // RFC hard gate — spec: score 0, ready false, NO exception
        if (!clientData.hasNationalId()) {
            log.warn("KYC hard gate — RFC missing for clientId: {}",
                    clientData.clientId());
            return new KycReadinessResult(
                    clientData.clientId(),
                    0,
                    false,
                    List.of("nationalId"),
                    null, null, false, null
            );
        }

        log.debug("KYC scoring — clientId: {}, RFC present: true",
                clientData.clientId());

        int score = props.getNationalId();
        List<String> missingFields = new ArrayList<>();

        if (clientData.hasDob()) {
            score += props.getDob();
        } else {
            missingFields.add("dateOfBirth");
        }

        if (clientData.firstName() != null
                && !clientData.firstName().isBlank()) {
            score += props.getFirstName();
        } else {
            missingFields.add("firstName");
        }

        if (clientData.lastName() != null
                && !clientData.lastName().isBlank()) {
            score += props.getLastName();
        } else {
            missingFields.add("lastName");
        }

        if (clientData.hasAddress()) {
            score += props.getAddress();
        } else {
            missingFields.add("address");
        }

        if (clientData.hasPhone()) {
            score += props.getPhone();
        } else {
            missingFields.add("phoneNumber");
        }

        boolean ready = score >= props.getThreshold();

        log.debug("KYC score — clientId: {}, score: {}, ready: {}",
                clientData.clientId(), score, ready);

        return new KycReadinessResult(
                clientData.clientId(),
                score,
                ready,
                List.copyOf(missingFields),
                null, null, false, null
        );
    }
}

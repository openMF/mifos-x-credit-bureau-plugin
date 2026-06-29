package org.mifos.creditbureau.cb_ild.dto;

import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe DTO for GET /api/clients/{id}/bureau-response.
 *
 * NEVER exposes:
 *   fullResponse    — raw CDC response, PII
 *   rawResponseHash — internal audit only
 *   softDeleted     — internal only
 *
 * Angular Tab 3 receives this exactly.
 */
public record BureauResponseDTO(
        Long id,
        Long clientId,
        String bureauType,
        Integer ficoScore,
        String riskBand,
        Boolean hasDelinquencies,
        Boolean scoreDropAlert,
        String tradelines,
        String alerts,
        LocalDateTime pulledAt,
        LocalDate expiryDate,
        LocalDate dateOfFirstDelinquency
) {
    public static BureauResponseDTO from(BureauResponseEntity entity) {
        return new BureauResponseDTO(
                entity.getId(),
                entity.getClientId(),
                entity.getBureauType(),
                entity.getFicoScore(),
                entity.getRiskBand(),
                entity.getHasDelinquencies(),
                entity.getScoreDropAlert(),
                entity.getTradelines(),
                entity.getAlerts(),
                entity.getPulledAt(),
                entity.getExpiryDate(),
                entity.getDateOfFirstDelinquency()
        );
    }
}

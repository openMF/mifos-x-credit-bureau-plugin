package org.mifos.creditbureau.cb_ild.service.kyc;

import org.mifos.creditbureau.cb_ild.client.FineractClientData;

/**
 * SOLID interface for KYC completeness scoring.
 */
public interface IKycScoringService {
    KycReadinessResult score(FineractClientData clientData);
}

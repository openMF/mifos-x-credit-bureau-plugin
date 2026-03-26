package org.mifos.creditbureau.service.connectors;

import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.ConnectionTestResult;
import org.mifos.creditbureau.data.CreditReportResult;

/**
 * Strategy interface for credit bureau integrations.
 * <p>
 * Each supported credit bureau (e.g., Circulo de Credito, Equifax)
 * provides an implementation of this interface. The {@link ConnectorRegistry}
 * auto-discovers all implementations via Spring dependency injection
 * and resolves the correct one at runtime based on
 * {@link org.mifos.creditbureau.domain.CreditBureau#getBureauType()}.
 * <p>
 * To add support for a new credit bureau, simply create a new {@code @Service}
 * class implementing this interface — no configuration or factory changes needed.
 */
public interface CreditBureauConnector {

    /**
     * Unique type key matching the {@code bureau_type} column in the
     * {@code credit_bureau} table. Used by {@link ConnectorRegistry}
     * for connector resolution.
     *
     * @return the bureau type identifier (e.g., "CIRCULO_DE_CREDITO")
     */
    String getBureauType();

    /**
     * Fetch a credit report from the external credit bureau for the given client.
     *
     * @param creditBureauId the ID of the registered credit bureau (used to retrieve API keys)
     * @param clientData     the client data fetched from Fineract
     * @return a {@link CreditReportResult} containing the generalized report and metadata
     */
    CreditReportResult fetchCreditReport(Long creditBureauId, ClientData clientData);

    /**
     * Verify connectivity and credential validity with the external credit bureau.
     *
     * @param creditBureauId the ID of the registered credit bureau
     * @return a {@link ConnectionTestResult} indicating success or failure
     */
    ConnectionTestResult testConnection(Long creditBureauId);
}

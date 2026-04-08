package org.mifos.creditbureau.service.connectors;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry that auto-discovers all {@link CreditBureauConnector} beans
 * via Spring dependency injection and resolves the correct connector
 * at runtime based on the credit bureau's {@code bureauType}.
 * <p>
 * To register a new connector, simply create a {@code @Service} class
 * implementing {@link CreditBureauConnector} — the registry picks it
 * up automatically. No configuration, no switch statements.
 */
@Service
@Slf4j
public class ConnectorRegistry {

    private final Map<String, CreditBureauConnector> connectors;
    private final CreditBureauRepository creditBureauRepository;

    /**
     * Spring injects ALL {@link CreditBureauConnector} beans into
     * the {@code connectorBeans} list. The registry indexes them
     * by {@link CreditBureauConnector#getBureauType()}.
     *
     * @param connectorBeans all connector beans discovered by Spring
     * @param creditBureauRepository repository to look up bureau entities
     * @throws IllegalStateException if two connectors have the same bureau type
     */
    public ConnectorRegistry(List<CreditBureauConnector> connectorBeans,
                             CreditBureauRepository creditBureauRepository) {
        this.creditBureauRepository = creditBureauRepository;
        this.connectors = connectorBeans.stream()
                .collect(Collectors.toMap(
                        CreditBureauConnector::getBureauType,
                        Function.identity()));
        log.info("Registered {} credit bureau connector(s): {}",
                connectors.size(), connectors.keySet());
    }

    /**
     * Resolve the correct connector for the given credit bureau ID.
     *
     * @param creditBureauId the database ID of the credit bureau
     * @return the connector implementation for this bureau's type
     * @throws IllegalArgumentException if the bureau ID is not found,
     *         or if no connector is registered for the bureau's type
     */
    public CreditBureauConnector getConnector(Long creditBureauId) {
        CreditBureau bureau = creditBureauRepository.findById(creditBureauId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Credit Bureau not found with id: " + creditBureauId));

        String type = bureau.getBureauType();
        CreditBureauConnector connector = connectors.get(type);
        if (connector == null) {
            throw new IllegalArgumentException(
                    "No connector registered for bureau type: " + type
                            + ". Supported types: " + connectors.keySet());
        }
        return connector;
    }

    /**
     * Returns all bureau types that have a registered connector.
     *
     * @return an unmodifiable set of supported bureau type keys
     */
    public Set<String> getSupportedTypes() {
        return Collections.unmodifiableSet(connectors.keySet());
    }
}

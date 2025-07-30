package org.mifos.creditbureau.data;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Map;

/**
 * Represents configuration parameters for a Credit Bureau.
 * This is an immutable DTO. Use the builder to create new instances.
 */
@Getter
@Builder(toBuilder = true) // toBuilder=true allows you to create a modified copy
public class CBRegisterParamsData {

    @Builder.Default
    private final long id = 0;

    // Map to store dynamic configuration parameters
    @Singular // Lombok will generate a method to add one item at a time
    private final Map<String, String> registrationParams;

}

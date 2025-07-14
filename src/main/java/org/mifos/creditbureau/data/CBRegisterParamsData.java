package org.mifos.creditbureau.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents configuration parameters for a Credit Bureau.
 * This is a parent class that uses the Strategy pattern combined with a Map-based approach
 * to allow child classes to define their own specific keys.
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class CBRegisterParamsData {

    private long id;

    // Map to store dynamic configuration parameters
    protected Map<String, String> registrationParams = new HashMap<>();

    public String getParam(String key) {

        return key == null? null: registrationParams.get(key);
    }

    public CBRegisterParamsData setParam(String key, String value) {
        registrationParams.put(key, value);
        return this;
    }

    public List<String> getParamKeys() {
        return new ArrayList<>(registrationParams.keySet());
    }

}

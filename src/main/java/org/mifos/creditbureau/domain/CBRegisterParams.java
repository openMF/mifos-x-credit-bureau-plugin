package org.mifos.creditbureau.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing registration parameters for a Credit Bureau.
 * This entity uses a Map-based approach to store dynamic configuration parameters.
 */
@Entity
@Table(name = "cb_registration_params")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CBRegisterParams {

    @Id
    @Column(name = "credit_bureau_id")
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "credit_bureau_id")
    private CreditBureau creditBureau;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "credit_bureau_registration_param_values", joinColumns = @JoinColumn(name = "param_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> registrationParams = new HashMap<>();


}
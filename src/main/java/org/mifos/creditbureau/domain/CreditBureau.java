package org.mifos.creditbureau.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a Credit Bureau Organization.
 */
@Entity
@Table(name = "credit_bureau")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String creditBureauName;

    @Column(name = "is_available")
    private boolean available;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "country")
    private String country;

    @OneToOne(mappedBy = "creditBureau", cascade = CascadeType.ALL, orphanRemoval = true)
    private CBRegisterParams creditBureauParameter;



}
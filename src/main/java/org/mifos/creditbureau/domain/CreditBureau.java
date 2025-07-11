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

    @OneToOne(mappedBy = "credit_bureau", cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private CBRegisterParams creditBureauParameter;

    @Column(name = "name", nullable = false)
    private String creditBureauName;

    @Column(name = "is_available")
    private boolean isAvailable;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "country")
    private String country;



}
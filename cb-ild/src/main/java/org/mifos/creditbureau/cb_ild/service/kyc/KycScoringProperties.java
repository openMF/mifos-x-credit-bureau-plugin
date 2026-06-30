package org.mifos.creditbureau.cb_ild.service.kyc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KYC scoring weights — loaded from application.properties.
 * Prefix: mifos.kyc.scoring
 *
 * Why @ConfigurationProperties:
 *   Victor can change weights without code change or redeploy.
 *   Only application.properties needs updating.
 *   Default values match spec: RFC=30, DOB=20, names=15, address=15, phone=5.
 */
@Component
@ConfigurationProperties(prefix = "mifos.kyc.scoring")
public class KycScoringProperties {

    private int nationalId = 30;
    private int dob = 20;
    private int firstName = 15;
    private int lastName = 15;
    private int address = 15;
    private int phone = 5;
    private int threshold = 70;

    public int getNationalId()  { return nationalId; }
    public int getDob()         { return dob; }
    public int getFirstName()   { return firstName; }
    public int getLastName()    { return lastName; }
    public int getAddress()     { return address; }
    public int getPhone()       { return phone; }
    public int getThreshold()   { return threshold; }

    public void setNationalId(int v)  { this.nationalId = v; }
    public void setDob(int v)         { this.dob = v; }
    public void setFirstName(int v)   { this.firstName = v; }
    public void setLastName(int v)    { this.lastName = v; }
    public void setAddress(int v)     { this.address = v; }
    public void setPhone(int v)       { this.phone = v; }
    public void setThreshold(int v)   { this.threshold = v; }
}

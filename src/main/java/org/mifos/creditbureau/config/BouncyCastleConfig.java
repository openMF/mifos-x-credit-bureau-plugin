package org.mifos.creditbureau.config;

import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.security.Security;

@Configuration
public class BouncyCastleConfig {

    @PostConstruct
    public void initialize() {
        if(Security.getProvider("BC") == null){
            try {
                Class<?> providerClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                Security.addProvider((java.security.Provider) providerClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize BouncyCastle provider", e);
            }
        }
    }

}

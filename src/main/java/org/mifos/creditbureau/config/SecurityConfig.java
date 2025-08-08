package org.mifos.creditbureau.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simplicity with tools like Postman
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").authenticated() // All /api/ paths require auth
                        .anyRequest().permitAll()
                )
                .httpBasic(withDefaults()); // Enable Basic Auth

        return http.build();
    }
}


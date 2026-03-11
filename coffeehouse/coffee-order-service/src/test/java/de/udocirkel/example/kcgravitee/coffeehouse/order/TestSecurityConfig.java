package de.udocirkel.example.kcgravitee.coffeehouse.order;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Minimale Security-Konfiguration für Unit-Tests.
 * Aktiviert nur den Method-Security-AOP (@PreAuthorize) –
 * ohne JWT-Ressourceserver, ohne WebSecurity-FilterChain und ohne
 * externe Abhängigkeiten (jwk-set-uri, audiences, …).
 */
@Configuration
@EnableMethodSecurity
class TestSecurityConfig {
}

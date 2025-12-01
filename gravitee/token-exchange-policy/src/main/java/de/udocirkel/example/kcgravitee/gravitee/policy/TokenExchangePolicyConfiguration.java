package de.udocirkel.example.kcgravitee.gravitee.policy;

import io.gravitee.policy.api.PolicyConfiguration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuppressWarnings("unused")
public class TokenExchangePolicyConfiguration implements PolicyConfiguration {

    /**
     * Prefix of the authorized party of the incoming token that must match
     * in order for an OIDC Token Exchange to be performed.
     */
    private String authorizedPartyPrefix;

    /**
     * Scope parameter for the OIDC token exchange.
     */
    private String targetScope;

}

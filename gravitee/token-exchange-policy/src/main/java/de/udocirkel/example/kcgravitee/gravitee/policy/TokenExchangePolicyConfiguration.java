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
     * URL of the token endpoint used to perform the token exchange.
     * <p>
     * Mandatory parameter.
     */
    private String tokenEndpointUrl;

    /**
     * Client ID used to authenticate against the token endpoint during the token exchange.
     * <p>
     * Mandatory parameter.
     */
    private String tokenExchangeClientId;

    /**
     * Client secret used to authenticate against the token endpoint during the token exchange.
     * <p>
     * Mandatory parameter.
     */
    private String tokenExchangeClientSecret;

    /**
     * Prefix of the "authorized party" (azp) claim in the incoming token.
     * The token exchange is allowed only if the azp claim starts with this prefix.
     * <p>
     * Use case: Restricts token exchange to specific clients. For example, if only incoming lightweight tokens issued
     * for customer applications should be exchanged, and the corresponding client IDs follow a naming convention with
     * the prefix "customer-".
     * <p>
     * Mandatory parameter.
     */
    private String authorizedPartyPrefix;

    /**
     * Audience that must be present in the incoming token for the token exchange to succeed.
     * <p>
     * Use case: Ensures that the incoming token was issued for a client that is permitted to perform a token exchange.
     * For example, only tokens that contain the audience "api-gateway".
     * <p>
     * Optional parameter.
     */
    private String matchingAudience;

    /**
     * Audience that must NOT be present in the incoming token for the token exchange to succeed.
     * <p>
     * Use case: Prevents token exchanges for incoming tokens that already have the permissions represented by the given audience.
     * <p>
     * Optional parameter.
     */
    private String notMatchingAudience;

    /**
     * Scope that must NOT be present in the incoming token for the token exchange to succeed.
     * <p>
     * Use case: Prevents token exchanges for incoming tokens that already have the permissions represented by the given scope.
     * <p>
     * Optional parameter.
     */
    private String notMatchingScope;

    /**
     * Scope to request for the newly created token after the exchange.
     * <p>
     * Use case: Defines which permissions the newly issued token (result of the token exchange) should have.
     * <p>
     * Mandatory parameter.
     */
    private String targetScope;

}

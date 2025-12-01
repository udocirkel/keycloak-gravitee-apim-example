package de.udocirkel.example.kcgravitee.keycloak.extension;

import com.google.auto.service.AutoService;

import java.util.List;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

@AutoService(ProtocolMapper.class)
public class CustomerNumberMapper extends AbstractOIDCProtocolMapper implements OIDCIDTokenMapper, OIDCAccessTokenMapper, TokenIntrospectionTokenMapper {

    private static final String PROVIDER_ID = "kcgravitee-oidc-customer-number-mapper";

    private static final String ATTRIBUTE_CUSTOMER_NUMBER = "customer_number";
    private static final String CLAIM_CUSTOMER_NUMBER = "customer_number";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Customer Number Mapper";
    }

    @Override
    public String getHelpText() {
        return """
                Propagates the 'customer_number' claim from the incoming token to the newly issued ID and Access Token.
                If the incoming token contains a 'customer_number' claim, it is copied unchanged to the new token.
                If the claim is not present, the 'customer_number' user attribute is used.
                If no customer number can be found, no value is added.
                This mapper is intended for token exchange or chained token flows, where a downstream client
                needs to know the customer number that was already present in the incoming token.
                """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        mapCustomerNumber(token, session, userSession);
        return token;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        mapCustomerNumber(token, session, userSession);
        return token;
    }

    @Override
    public AccessToken transformIntrospectionToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        mapCustomerNumber(token, session, userSession);
        return token;
    }

    private void mapCustomerNumber(IDToken token, KeycloakSession session, UserSessionModel userSession) {
        // Incoming token must be present
        if (!(session.getContext().getBearerToken() instanceof IDToken originToken)) {
            return;
        }

        // Preserve value from incoming token if claim is present
        if (originToken.getOtherClaims().get(CLAIM_CUSTOMER_NUMBER) instanceof String customerNumber) {
            token.setOtherClaims(CLAIM_CUSTOMER_NUMBER, customerNumber);
            return;
        }

        // Set value from user attribute if attribute is present
        String customerNumber = userSession.getUser().getFirstAttribute(ATTRIBUTE_CUSTOMER_NUMBER);
        if (customerNumber != null) {
            token.setOtherClaims(CLAIM_CUSTOMER_NUMBER, customerNumber);
        }
    }

}

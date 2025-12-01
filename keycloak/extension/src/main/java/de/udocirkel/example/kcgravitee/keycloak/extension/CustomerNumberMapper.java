package de.udocirkel.example.kcgravitee.keycloak.extension;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.List;

public class CustomerNumberMapper extends AbstractOIDCProtocolMapper implements OIDCIDTokenMapper, OIDCAccessTokenMapper, TokenIntrospectionTokenMapper {

    private static final String PROVIDER_ID = "kcgravitee-oidc-customer-number-mapper";

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
                If the claim is not present, no value is added.
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
        mapCustomerNumber(token, session);
        return token;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        mapCustomerNumber(token, session);
        return token;
    }

    @Override
    public AccessToken transformIntrospectionToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        mapCustomerNumber(token, session);
        return token;
    }

    private void mapCustomerNumber(IDToken token, KeycloakSession session) {
        if (session.getContext().getBearerToken() instanceof IDToken originToken) {
            if (originToken.getOtherClaims().get(CLAIM_CUSTOMER_NUMBER) instanceof String customerNumber) {
                token.setOtherClaims(CLAIM_CUSTOMER_NUMBER, customerNumber);
            }
        }
    }

}

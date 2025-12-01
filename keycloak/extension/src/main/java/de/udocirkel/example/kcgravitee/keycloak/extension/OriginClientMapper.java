package de.udocirkel.example.kcgravitee.keycloak.extension;

import com.google.auto.service.AutoService;

import java.util.List;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;

@AutoService(ProtocolMapper.class)
public class OriginClientMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    private static final String PROVIDER_ID = "kcgravitee-oidc-origin-client-mapper";

    private static final String CLAIM_ORIGIN_CLIENT_ID = "origin_client_id";

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
        return "Origin Client Mapper";
    }

    @Override
    public String getHelpText() {
        return """
                Adds the Client ID of the incoming token to the newly issued Access Token.
                If the incoming token already contains an 'origin_client_id' claim, it is preserved.
                Otherwise, the mapper falls back to the 'issued for' value of the incoming token.
                This mapper is intended for token exchange or chained token flows, where a downstream client
                needs to know which client originally authenticated the request.
                """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // Incoming token must be an access token
        if (!(session.getContext().getBearerToken() instanceof AccessToken originToken)) {
            return token;
        }

        // Preserve value from incoming token if claim is present
        if (originToken.getOtherClaims().get(CLAIM_ORIGIN_CLIENT_ID) instanceof String originClientId) {
            token.setOtherClaims(CLAIM_ORIGIN_CLIENT_ID, originClientId);
            return token;
        }

        // Set value from incoming token (issued for)
        token.setOtherClaims(CLAIM_ORIGIN_CLIENT_ID, originToken.getIssuedFor());
        return token;
    }

}

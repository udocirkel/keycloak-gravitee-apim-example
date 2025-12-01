package de.udocirkel.example.kcgravitee.keycloak.extension;

import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;

public class TokenIntrospectionBlockerMapper extends AbstractOIDCProtocolMapper implements TokenIntrospectionTokenMapper {

    private static final Logger LOG = Logger.getLogger(TokenIntrospectionBlockerMapper.class);

    private static final String PROVIDER_ID = "kcgravitee-oidc-token-introspection-blocker";

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
        return "Token Introspection Blocker";
    }

    @Override
    public String getHelpText() {
        return "Prevents external clients with the ID prefix 'coffee-app-' from successfully performing token introspection.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public AccessToken transformIntrospectionToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

        // Caller Client ID aus der Keycloak Session holen (in der Client Session steht die Issued For Client ID)
        String clientId = nullSafe(() -> session.getContext().getClient().getClientId());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Caller Client ID from keycloak session is " + clientId);
        }

        if (clientId != null && clientId.toLowerCase().startsWith("coffee-app-")) {
            throw new ErrorResponseException(
                    "forbidden_client",
                    "This client is not allowed",
                    Response.Status.FORBIDDEN);
        }

        return token;
    }

    private <T> T nullSafe(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (NullPointerException e) {
            return null;
        }
    }

}

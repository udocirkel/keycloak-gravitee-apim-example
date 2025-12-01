package de.udocirkel.example.kcgravitee.keycloak.extension;

import com.google.auto.service.AutoService;

import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProvider;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProviderFactory;
import org.keycloak.protocol.oidc.TokenIntrospectionProvider;
import org.keycloak.protocol.oidc.TokenIntrospectionProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;

/**
 * Prevents external clients with the ID prefix <code>coffee-app-</code> from successfully performing token introspection.
 *
 * @param <T>
 */
public class CustomTokenIntrospectionProvider<T extends AccessToken> extends AccessTokenIntrospectionProvider<T> {

    private static final Logger LOG = Logger.getLogger(CustomTokenIntrospectionProvider.class);

    private static final String EXTERNAL_CLIENT_PREFIX = "coffee-app-";

    /**
     * This replaces the default implementation in Keycloak - as we keep the providerId the same as the original implementation.
     */
    @AutoService(TokenIntrospectionProviderFactory.class)
    public static class Factory extends AccessTokenIntrospectionProviderFactory {
        @Override
        public TokenIntrospectionProvider create(KeycloakSession session) {
            return new CustomTokenIntrospectionProvider<>(session);
        }
    }

    public CustomTokenIntrospectionProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public Response introspect(String tokenStr, EventBuilder event) {
        ClientModel authenticatedClient = session.getContext().getClient();
        if (!canIntrospect(authenticatedClient)) {
            String detail = String.format("Client %s not allowed to introspect token.", authenticatedClient.getClientId());
            if (LOG.isDebugEnabled()) {
                LOG.debug(detail);
            }
            throw createErrorResponseException(Errors.UNAUTHORIZED_CLIENT, detail, Response.Status.FORBIDDEN, event);
        }
        return super.introspect(tokenStr, event);
    }

    private boolean canIntrospect(ClientModel authenticatedClient) {
        String clientId = authenticatedClient.getClientId();
        if (clientId != null && clientId.toLowerCase().startsWith(EXTERNAL_CLIENT_PREFIX)) {
            return false;
        }
        return true;
    }

    private ErrorResponseException createErrorResponseException(String error, String detail, Response.Status status, EventBuilder event) {
        event.detail("detail", detail).error(error);
        return new ErrorResponseException(error, detail, status);
    }

}

package de.udocirkel.example.kcgravitee.gravitee.policy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OIDC Token Exchange Policy
 * <p>
 * This policy allows exchanging an incoming OIDC token for a new token with
 * different scopes or audiences. It validates the incoming token against
 * configurable criteria (authorized party, audiences, and scopes) and defines
 * the client credentials and target scopes for the token exchange.
 * <p>
 * Additionally, the policy caches newly issued tokens until they expire.
 * This allows reusing the same exchanged token for repeated requests with
 * the same incoming token and target scope, reducing unnecessary token
 * requests to the authorization server.
 * <p>
 * Use demo token endpoint URL: http://keycloak:8080/realms/coffeehouse/protocol/openid-connect/token
 */
public class TokenExchangePolicy {

    private static final Logger LOG = LoggerFactory.getLogger(TokenExchangePolicy.class);

    private static final long TOKEN_CACHE_DURATION = 5;
    private static final TimeUnit TOKEN_CACHE_TIMEUNIT = TimeUnit.MINUTES;
    private static final long TOKEN_CACHE_MAXSIZE = 10_000;

    private static final String TOKEN_EXCHANGE_ERROR = "TOKEN_EXCHANGE_ERROR";
    private static final String TOKEN_EXCHANGE_EXIT_ON_ERROR = "TOKEN_EXCHANGE_EXIT_ON_ERROR";

    private static final String ENCODED_GRANT_TYPE_FOR_TOKEN_EXCHANGE = encode("urn:ietf:params:oauth:grant-type:token-exchange");
    private static final String ENCODED_TOKEN_TYPE_FOR_ACCESS_TOKEN = encode("urn:ietf:params:oauth:token-type:access_token");

    private final Cache<String, String> tokenCache =
            Caffeine.newBuilder()
                    // Ablaufzeit der Tokens
                    .expireAfterWrite(TOKEN_CACHE_DURATION, TOKEN_CACHE_TIMEUNIT)
                    // Tokenanzahl begrenzen
                    .maximumSize(TOKEN_CACHE_MAXSIZE)
                    .build();

    /**
     * The associated configuration to this TokenExchange Policy
     */
    private TokenExchangePolicyConfiguration configuration;

    /**
     * Create a new TokenExchange Policy instance based on its associated configuration
     *
     * @param configuration the associated configuration to the new TokenExchange Policy instance
     */
    public TokenExchangePolicy(TokenExchangePolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext context, PolicyChain policyChain) {

        var incomingToken = getIncomingToken(context);
        if (incomingToken == null || incomingToken.isBlank()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Request has no Authorization header with Bearer token");
            }
            policyChain.doNext(request, response);
            return;
        }

        var issuedFor = getIncomingTokenIssuedFor(context);
        if (issuedFor == null || issuedFor.isBlank()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Incoming Bearer token has no authorized party (claim 'azp') specified");
            }
            policyChain.doNext(request, response);
            return;
        }

        var authorizedPartyPrefix = configuration.getAuthorizedPartyPrefix();
        if (!issuedFor.startsWith(authorizedPartyPrefix)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Incoming Bearer token has an authorized party (claim 'azp') not matching the configured prefix '{}'", authorizedPartyPrefix);
            }
            policyChain.doNext(request, response);
            return;
        }

        var matchingAudience = configuration.getMatchingAudience();
        if (matchingAudience != null && !matchingAudience.isBlank()) {
            var audiences = getIncomingTokenAudiences(context);
            if (!audiences.contains(matchingAudience)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Incoming Bearer token does not contain the audience '{}'", matchingAudience);
                }
                policyChain.doNext(request, response);
                return;
            }
        }

        var notMatchingAudience = configuration.getNotMatchingAudience();
        if (notMatchingAudience != null && !notMatchingAudience.isBlank()) {
            var audiences = getIncomingTokenAudiences(context);
            if (audiences.contains(notMatchingAudience)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Incoming Bearer token does already contain the audience '{}'", notMatchingAudience);
                }
                policyChain.doNext(request, response);
                return;
            }
        }

        var notMatchingScope = configuration.getNotMatchingScope();
        if (notMatchingScope != null && !notMatchingScope.isBlank()) {
            var scopes = getIncomingTokenScopes(context);
            if (scopes.contains(notMatchingScope)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Incoming Bearer token does already contain the scope '{}'", notMatchingScope);
                }
                policyChain.doNext(request, response);
                return;
            }
        }

        var targetScope = configuration.getTargetScope();
        var tokenFromCache = getTokenFromCache(incomingToken, targetScope);
        if (tokenFromCache != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cached token found for incoming Bearer token and target scope '{}': {}", targetScope, tokenFromCache);
            }
            setAuthorizationTokenForRequest(request, tokenFromCache);
            policyChain.doNext(request, response);
            return;
        }

        handleTokenExchange(request, response, context, policyChain, incomingToken);
    }

    private void handleTokenExchange(Request request, Response response, ExecutionContext context, PolicyChain policyChain, String incomingToken) {

        var options = new HttpClientOptions()
                .setSsl(false)
                .setTrustAll(true)
                .setVerifyHost(false);

        var httpClient = context
                .getComponent(Vertx.class)
                .createHttpClient(options);

        var form = "grant_type=" + ENCODED_GRANT_TYPE_FOR_TOKEN_EXCHANGE
                + "&client_id=" + encode(configuration.getTokenExchangeClientId())
                + "&client_secret=" + encode(configuration.getTokenExchangeClientSecret())
                + "&subject_token=" + encode(incomingToken)
                + "&subject_token_type=" + ENCODED_TOKEN_TYPE_FOR_ACCESS_TOKEN
                + "&requested_token_type=" + ENCODED_TOKEN_TYPE_FOR_ACCESS_TOKEN
                + "&scope=" + encode(configuration.getTargetScope());

        var requestOpts = new RequestOptions()
                .setMethod(HttpMethod.POST)
                .setAbsoluteURI(configuration.getTokenEndpointUrl())
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .putHeader("Content-Length", String.valueOf(form.length()));

        httpClient.request(requestOpts)
                .onFailure(throwable -> handleFailure(policyChain, httpClient, throwable))
                .onSuccess(httpClientRequest -> { // Connection established, lets continue
                    httpClientRequest.send(Buffer.buffer(form))
                            .onSuccess(httpResponse -> handleSuccess(httpResponse, request, response, policyChain, httpClient, incomingToken))
                            .onFailure(throwable -> handleFailure(policyChain, httpClient, throwable));
                });
    }

    private void handleSuccess(
            HttpClientResponse httpResponse,
            Request request,
            Response response,
            PolicyChain policyChain,
            HttpClient httpClient,
            String incomingToken
    ) {
        httpResponse.bodyHandler(body -> {
            try {

                if (!(body.toJson() instanceof JsonObject json)) {
                    var errorContent = "Request is terminated.";
                    policyChain.failWith(PolicyResult.failure(TOKEN_EXCHANGE_EXIT_ON_ERROR, errorContent));
                    return;
                }

                String newToken = json.getString("access_token");
                if (newToken == null || newToken.isBlank()) {
                    var errorContent = "Request is terminated.";
                    policyChain.failWith(PolicyResult.failure(TOKEN_EXCHANGE_EXIT_ON_ERROR, errorContent));
                    return;
                }

                putTokenInCache(incomingToken, configuration.getTargetScope(), newToken);
                setAuthorizationTokenForRequest(request, newToken);
                policyChain.doNext(request, response);

            } finally {
                httpClient.close();
            }
        });
    }

    private void handleFailure(PolicyChain policyChain, HttpClient httpClient, Throwable throwable) {
        try {

            policyChain.failWith(PolicyResult.failure(TOKEN_EXCHANGE_ERROR, throwable.getMessage()));

        } finally {
            httpClient.close();
        }
    }

    private String getIncomingToken(ExecutionContext context) {
        var token = context.getAttribute("jwt.token");
        if (token == null) {
            return null;
        }

        return token.toString();
    }

    private String getIncomingTokenIssuedFor(ExecutionContext context) {
        var claims = context.getAttribute("jwt.claims");
        if (!(claims instanceof Map<?, ?> map)) {
            return null;
        }

        var issuedFor = map.get("azp");
        if (issuedFor == null) {
            return null;
        }

        return issuedFor.toString();
    }

    private Collection<String> getIncomingTokenAudiences(ExecutionContext context) {
        var claims = context.getAttribute("jwt.claims");
        if (!(claims instanceof Map<?, ?> map)) {
            return List.of();
        }

        var audiences = map.get("aud");
        if (!(audiences instanceof Collection<?> coll)) {
            return List.of();
        }

        return (Collection<String>) coll;
    }

    private Collection<String> getIncomingTokenScopes(ExecutionContext context) {
        var claims = context.getAttribute("jwt.claims");
        if (!(claims instanceof Map<?, ?> map)) {
            return List.of();
        }

        var audiences = map.get("scope");
        if (!(audiences instanceof Collection<?> coll)) {
            return List.of();
        }

        return (Collection<String>) coll;
    }

    private String getTokenFromCache(String incomingToken, String targetScope) {
        var hashKey = getHashKeyForTokenAndScope(incomingToken, targetScope);
        return tokenCache.getIfPresent(hashKey);
    }

    private void putTokenInCache(String incomingToken, String targetScope, String newToken) {
        var hashKey = getHashKeyForTokenAndScope(incomingToken, targetScope);
        tokenCache.put(hashKey, newToken);
    }

    private String getHashKeyForTokenAndScope(String token, String targetScope) {
        return Objects.toString((token + "|" + targetScope).hashCode());
    }

    private void setAuthorizationTokenForRequest(Request request, String token) {
        var headerValue = "Bearer " + token;
        request.headers().set("Authorization", headerValue);
    }

    @OnResponse
    public void onResponse(Request request, Response response, PolicyChain policyChain) {
        if (isASuccessfulResponse(response)) {
            policyChain.doNext(request, response);
        } else {
            policyChain.failWith(
                    PolicyResult.failure(HttpStatusCode.INTERNAL_SERVER_ERROR_500, "Not a successful response :-("));
        }
    }

    private static boolean isASuccessfulResponse(Response response) {
        var status = response.status();
        return (status >= 100 && status <= 399);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

}

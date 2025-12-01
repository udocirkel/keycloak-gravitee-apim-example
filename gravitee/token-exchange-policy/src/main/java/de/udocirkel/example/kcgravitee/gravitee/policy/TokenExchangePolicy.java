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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenExchangePolicy {

    private static final Logger LOG = LoggerFactory.getLogger(TokenExchangePolicy.class);

    private static final String TOKEN_EXCHANGE_URL = "http://keycloak:8080/realms/coffeehouse/protocol/openid-connect/token";

    private static final long TOKEN_CACHE_DURATION = 5;
    private static final TimeUnit TOKEN_CACHE_TIMEUNIT = TimeUnit.MINUTES;
    private static final long TOKEN_CACHE_MAXSIZE = 10_000;

    private static final String TOKEN_EXCHANGE_ERROR = "TOKEN_EXCHANGE_ERROR";
    private static final String TOKEN_EXCHANGE_EXIT_ON_ERROR = "TOKEN_EXCHANGE_EXIT_ON_ERROR";

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
            if (LOG.isWarnEnabled()) {
                LOG.warn("Incoming Bearer token is missing in the Authorization header");
            }
            policyChain.doNext(request, response);
            return;
        }

        var issuedFor = getIncomingTokenIssuedFor(context);
        if (issuedFor == null || issuedFor.isBlank()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Incoming Bearer token is invalid: authorized party ('azp') claim is missing or blank");
            }
            policyChain.doNext(request, response);
            return;
        }

        var authorizedPartyPrefix = configuration.getAuthorizedPartyPrefix();
        if (!issuedFor.startsWith(authorizedPartyPrefix)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Incoming Bearer token is invalid: authorized party ('azp') does not match the configured prefix '{}'", authorizedPartyPrefix);
            }
            policyChain.doNext(request, response);
            return;
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

        handleTokenExchange(request, response, context, policyChain, incomingToken, targetScope);
    }

    private void handleTokenExchange(Request request, Response response, ExecutionContext context, PolicyChain policyChain, String incomingToken, String targetScope) {

        var options = new HttpClientOptions()
                .setSsl(false)
                .setTrustAll(true)
                .setVerifyHost(false);

        var httpClient = context
                .getComponent(Vertx.class)
                .createHttpClient(options);

        var form = "grant_type=" + encode("urn:ietf:params:oauth:grant-type:token-exchange")
                + "&client_id=" + encode("api-gateway")
                + "&client_secret=" + encode("api-gateway")
                + "&subject_token=" + encode(incomingToken)
                + "&subject_token_type=" + encode("urn:ietf:params:oauth:token-type:access_token")
                + "&requested_token_type=" + encode("urn:ietf:params:oauth:token-type:access_token")
                + "&scope=" + encode(targetScope);

        var requestOpts = new RequestOptions()
                .setMethod(HttpMethod.POST)
                .setAbsoluteURI(TOKEN_EXCHANGE_URL)
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .putHeader("Content-Length", String.valueOf(form.length()));

        httpClient.request(requestOpts)
                .onFailure(throwable -> handleFailure(policyChain, httpClient, throwable))
                .onSuccess(httpClientRequest -> { // Connection established, lets continue
                    httpClientRequest.send(Buffer.buffer(form))
                            .onSuccess(httpResponse -> handleSuccess(httpResponse, request, response, policyChain, httpClient, incomingToken, targetScope))
                            .onFailure(throwable -> handleFailure(policyChain, httpClient, throwable));
                });
    }

    private void handleSuccess(
            HttpClientResponse httpResponse,
            Request request,
            Response response,
            PolicyChain policyChain,
            HttpClient httpClient,
            String incomingToken,
            String targetScope
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

                putTokenInCache(incomingToken, targetScope, newToken);
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

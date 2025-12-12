package de.udocirkel.example.kcgravitee.gravitee.policy;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.slf4j.LoggerFactory;

class TokenExchangePolicyTest {

    private MemoryAppender memoryAppender;

    @BeforeEach
    void setup() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        memoryAppender = new MemoryAppender();
        memoryAppender.setContext(lc);

        Logger logger = lc.getLogger(TokenExchangePolicy.class); //logback logger
        logger.setLevel(Level.DEBUG);    // <-- funktioniert jetzt
        logger.addAppender(memoryAppender);

        memoryAppender.start();
    }

    private boolean logContainsMessage(String message) {
        return memoryAppender.getLoggedEvents().stream().anyMatch(loggingEvent -> loggingEvent.getFormattedMessage().equals(message));
    }

    @Test
    void testOnRequestWithInvalidToken() throws Exception {
        var config = new TokenExchangePolicyConfiguration();
        var policy = new TokenExchangePolicy(config);

        var request = mock(Request.class);
        var response = mock(Response.class);
        var context = mock(ExecutionContext.class);
        var policyChain = mock(PolicyChain.class);

        policy.onRequest(request, response, context, policyChain);

        assertThat(logContainsMessage("Request has no Authorization header with Bearer token")).isTrue();
    }


    @Test
    void testOnRequestWithNotMatchingAuthorizedPartyPrefix() throws Exception {
        var config = new TokenExchangePolicyConfiguration();
        config.setAuthorizedPartyPrefix("tea-");
        var policy = new TokenExchangePolicy(config);

        var request = mock(Request.class);
        var response = mock(Response.class);
        var context = mock(ExecutionContext.class);
        var policyChain = mock(PolicyChain.class);

        when(context.getAttribute("jwt.token"))
                .thenReturn("token");
        when(context.getAttribute("jwt.claims"))
                .thenReturn(Map.of("azp", "coffee-app-123"));

        policy.onRequest(request, response, context, policyChain);

        assertThat(logContainsMessage("Incoming Bearer token has an authorized party (claim 'azp') not matching the configured prefix 'tea-'")).isTrue();
    }

    @Test
    void testOnRequestWithNotMatchingAudience() throws Exception {
        var config = new TokenExchangePolicyConfiguration();
        config.setAuthorizedPartyPrefix("coffee-");
        config.setMatchingAudience("api-gateway");
        var policy = new TokenExchangePolicy(config);

        var request = mock(Request.class);
        var response = mock(Response.class);
        var context = mock(ExecutionContext.class);
        var policyChain = mock(PolicyChain.class);

        when(context.getAttribute("jwt.token"))
                .thenReturn("token");
        when(context.getAttribute("jwt.claims"))
                .thenReturn(Map.of("azp", "coffee-app-123"));

        policy.onRequest(request, response, context, policyChain);

        assertThat(logContainsMessage("Incoming Bearer token does not contain the audience 'api-gateway'")).isTrue();
    }

    @Test
    void testOnRequestWithMatchingAudience() throws Exception {
        var config = new TokenExchangePolicyConfiguration();
        config.setAuthorizedPartyPrefix("coffee-");
        config.setNotMatchingAudience("coffee-order-api");
        var policy = new TokenExchangePolicy(config);

        var request = mock(Request.class);
        var response = mock(Response.class);
        var context = mock(ExecutionContext.class);
        var policyChain = mock(PolicyChain.class);

        when(context.getAttribute("jwt.token"))
                .thenReturn("token");
        when(context.getAttribute("jwt.claims"))
                .thenReturn(Map.of(
                        "azp", "coffee-app-123",
                        "aud", List.of("coffee-order-api")));

        policy.onRequest(request, response, context, policyChain);

        assertThat(logContainsMessage("Incoming Bearer token does already contain the audience 'coffee-order-api'")).isTrue();
    }

    @Test
    void testOnRequestWithMatchingScope() throws Exception {
        var config = new TokenExchangePolicyConfiguration();
        config.setAuthorizedPartyPrefix("coffee-");
        config.setNotMatchingScope("coffee-order");
        var policy = new TokenExchangePolicy(config);

        var request = mock(Request.class);
        var response = mock(Response.class);
        var context = mock(ExecutionContext.class);
        var policyChain = mock(PolicyChain.class);

        when(context.getAttribute("jwt.token"))
                .thenReturn("token");
        when(context.getAttribute("jwt.claims"))
                .thenReturn(Map.of(
                        "azp", "coffee-app-123",
                        "scope", "profile email coffee-order"));

        policy.onRequest(request, response, context, policyChain);

        assertThat(logContainsMessage("Incoming Bearer token does already contain the scope 'coffee-order'")).isTrue();
    }

}
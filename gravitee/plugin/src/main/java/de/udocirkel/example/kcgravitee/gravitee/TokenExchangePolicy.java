package de.udocirkel.example.kcgravitee.gravitee;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyContext;
import io.gravitee.policy.api.annotations.OnRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenExchangePolicy {

    private final static Logger LOG = LoggerFactory.getLogger(TokenExchangePolicy.class);

    private final TokenExchangePolicyConfiguration configuration;

    public TokenExchangePolicy(TokenExchangePolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, PolicyChain chain, PolicyContext context) {
        // Beispiel: Header setzen
        request.headers().set(configuration.getHeaderName(), configuration.getHeaderValue());
        String authHeaderValue = request.headers().get("Authorization");
        LOG.info("###### Authorization Header Value: " + authHeaderValue);
        // Weiter zur nächsten Policy im Chain
        chain.doNext(request, response);
    }

}

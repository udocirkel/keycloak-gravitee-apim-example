package de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient;

import de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient.invoker.ApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class IngredientApiClientConfig {

    private static final Logger LOG = LoggerFactory.getLogger(IngredientApiClientConfig.class);

    private static final String AUTHORIZATION_HEADER_VALUE_PREFIX = "Bearer ";

    @Value("${coffeehouse.external.ingredient-service.url}")
    private String ingredientServiceUrl;

    @Bean(name = "ingredientApi")
    @Scope("singleton")
    public IngredientApi ingredientApi() {
        return new IngredientApi(ingredientApiClient());
    }

    private ApiClient ingredientApiClient() {
        var webClient = WebClient.builder()
                .baseUrl(ingredientServiceUrl)
                .filter(tokenRelayFilter())
                .build();

        var apiClient = new ApiClient(webClient);
        apiClient.setBasePath(ingredientServiceUrl);
        return apiClient;
    }

    private ExchangeFilterFunction tokenRelayFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request ->
                ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .filter(auth -> auth instanceof JwtAuthenticationToken)
                        .map(auth -> (JwtAuthenticationToken) auth)
                        .map(jwtAuth -> {
                            String token = jwtAuth.getToken().getTokenValue();
                            LOG.debug("Relaying incoming Bearer token to client request: {}", token);
                            return ClientRequest.from(request)
                                    .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE_PREFIX + token)
                                    .build();
                        })
                        .defaultIfEmpty(request)
        );
    }

}

package de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient;

import de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient.invoker.ApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Configuration
public class IngredientApiClientConfig {

    private static final Logger LOG = LoggerFactory.getLogger(IngredientApiClientConfig.class);

    @Value("${coffeehouse.external.ingredient-service.url}")
    private String ingredientServiceUrl;

    @Bean(name = "ingredientApi")
    @Scope("singleton")
    public IngredientApi ingredientApi() {
        return new IngredientApi(ingredientApiClient());
    }

    private ApiClient ingredientApiClient() {
        // Eigenen WebClient mit Filter aufbauen
        final var webClient = WebClient.builder()
                .baseUrl(ingredientServiceUrl)
                .filter(addBearerTokenFromSecurityContext())
                .build();

        // OpenAPI ApiClient mit eigenem WebClient verwenden
        final var apiClient = new ApiClient(webClient);
        apiClient.setBasePath(ingredientServiceUrl);
        return apiClient;
    }

    private ExchangeFilterFunction addBearerTokenFromSecurityContext() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                var tokenValue = jwtAuth.getToken().getTokenValue();
                LOG.debug("Set Authorization header in client request with token {}", tokenValue);
                return Mono.just(
                        ClientRequest.from(request)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue)
                                .build()
                );
            } else {
                LOG.debug("No token for Authorization header in client request");
                return Mono.just(request);
            }
        });
    }

    /*
    private ExchangeFilterFunction addBearerTokenFromSecurityContext() {
        return (request, next) ->
                ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .filter(auth -> auth.getCredentials() instanceof String)
                        .doOnNext(auth ->
                                LOG.debug("Forwarding bearer token for user: {}", auth.getName())
                        )
                        .map(auth -> ClientRequest.from(request)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.getCredentials())
                                .build())
                        .defaultIfEmpty(request)
                        .flatMap(next::exchange);
    }
     */

}

package de.udocirkel.example.kcgravitee.coffeehouse.order.external.menu;

import de.udocirkel.example.kcgravitee.coffeehouse.order.external.menu.invoker.ApiClient;

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
public class MenuApiClientConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MenuApiClientConfig.class);

    @Value("${coffeehouse.external.menu-service.url}")
    private String menuServiceUrl;

    @Bean(name = "menuApi")
    @Scope("singleton")
    public MenuApi menuApi() {
        return new MenuApi(ingredientApiClient());
    }

    private ApiClient ingredientApiClient() {
        // Eigenen WebClient mit Filter aufbauen
        final var webClient = WebClient.builder()
                .baseUrl(menuServiceUrl)
                .filter(addBearerTokenFromSecurityContext())
                .build();

        // OpenAPI ApiClient mit eigenem WebClient verwenden
        final var apiClient = new ApiClient(webClient);
        apiClient.setBasePath(menuServiceUrl);
        return apiClient;
    }

    private ExchangeFilterFunction addBearerTokenFromSecurityContext() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                var tokenValue = jwtAuth.getToken().getTokenValue();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Attaching Bearer token for user {} - request to {} will be sent WITH Authorization header and token {}", jwtAuth.getName(), request.url(), tokenValue);
                }
                return Mono.just(
                        ClientRequest.from(request)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue)
                                .build()
                );
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("No JwtAuthenticationToken found in SecurityContext — request to {} will be sent WITHOUT Authorization header", request.url());
                }
                return Mono.just(request);
            }
        });
    }

}

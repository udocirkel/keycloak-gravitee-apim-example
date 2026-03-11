package de.udocirkel.example.kcgravitee.coffeehouse.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.*;

import de.udocirkel.example.kcgravitee.coffeehouse.order.external.menu.Coffee;
import de.udocirkel.example.kcgravitee.coffeehouse.order.external.menu.MenuApi;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

/**
 * Schlanker Unit-Test für den OrderService.
 *
 * @ContextConfiguration lädt nur:
 * - OrderService          (zu testende Klasse)
 * - TestSecurityConfig    (aktiviert @EnableMethodSecurity / @PreAuthorize-AOP)
 * <p>
 * MenuApi wird als @MockitoBean bereitgestellt – kein echter WebClient,
 * kein JWT-Ressourceserver, keine Netzwerkverbindung nötig.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OrderService.class, TestSecurityConfig.class})
class OrderServiceTest {

    @MockitoBean
    private MenuApi menuApi;

    @Autowired
    private OrderService orderService;

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private Coffee aCoffee(String type, String... ingredients) {
        return new Coffee()
                .type(type)
                .price(new BigDecimal("2.50"))
                .ingredients(List.of(ingredients));
    }

    private NewOrder aNewOrder(String coffeeType) {
        return new NewOrder()
                .coffeeType(coffeeType)
                .userId("user-123");
    }

    // =========================================================================
    // createOrder
    // =========================================================================

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt eine Order zurück, wenn das Coffee vom MenuService gefunden wird")
        void shouldReturnOrder_whenCoffeeExists() {
            when(menuApi.getCoffee("espresso"))
                    .thenReturn(Mono.just(aCoffee("espresso", "coffee beans", "water")));

            var result = orderService.createOrder(aNewOrder("espresso"));

            assertThat(result).isPresent();
            assertThat(result.get().getCoffeeType()).isEqualTo("espresso");
            assertThat(result.get().getTotalPrice()).isEqualByComparingTo("2.50");
            assertThat(result.get().getIngredients()).containsExactly("coffee beans", "water");
            assertThat(result.get().getStatus()).isEqualTo(Order.StatusEnum.CREATED);
            assertThat(result.get().getUserId()).isEqualTo("user-123");
            assertThat(result.get().getId()).isNotBlank();
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("normalisiert den coffeeType auf Kleinbuchstaben")
        void shouldNormalizeCoffeeTypeToLowerCase() {
            when(menuApi.getCoffee("cappuccino"))
                    .thenReturn(Mono.just(aCoffee("cappuccino")));

            var result = orderService.createOrder(aNewOrder("CAPPUCCINO"));

            assertThat(result).isPresent();
            assertThat(result.get().getCoffeeType()).isEqualTo("cappuccino");
            verify(menuApi).getCoffee("cappuccino");
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt eine eindeutige ID für jede Order zurück")
        void shouldAssignUniqueIdToEachOrder() {
            when(menuApi.getCoffee(anyString()))
                    .thenReturn(Mono.just(aCoffee("latte")));

            var order1 = orderService.createOrder(aNewOrder("latte"));
            var order2 = orderService.createOrder(aNewOrder("latte"));

            assertThat(order1).isPresent();
            assertThat(order2).isPresent();
            assertThat(order1.get().getId()).isNotEqualTo(order2.get().getId());
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt Optional.empty() zurück, wenn der MenuService einen Fehler liefert")
        void shouldReturnEmpty_whenMenuServiceThrowsWebClientResponseException() {
            when(menuApi.getCoffee("espresso"))
                    .thenReturn(Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));

            var result = orderService.createOrder(aNewOrder("espresso"));

            assertThat(result).isEmpty();
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt Optional.empty() zurück, wenn der MenuService null liefert")
        void shouldReturnEmpty_whenMenuServiceReturnsNull() {
            when(menuApi.getCoffee("espresso"))
                    .thenReturn(Mono.empty()); // block() gibt null zurück → NullPointerException

            var result = orderService.createOrder(aNewOrder("espresso"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("wirft AuthenticationCredentialsNotFoundException, wenn kein User authentifiziert ist")
        void shouldThrowAccessDeniedException_whenNotAuthenticated() {
            assertThatThrownBy(() -> orderService.createOrder(aNewOrder("espresso")))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

            verifyNoInteractions(menuApi);
        }

        @Test
        @WithMockUser(roles = "OTHER_ROLE")
        @DisplayName("wirft AccessDeniedException, wenn User nicht die Rolle ACCESS hat")
        void shouldThrowAccessDeniedException_whenUserMissesAccessRole() {
            assertThatThrownBy(() -> orderService.createOrder(aNewOrder("espresso")))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(menuApi);
        }
    }

    // =========================================================================
    // getOrder
    // =========================================================================

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        private String existingOrderId;

        @BeforeEach
        void createAnOrder() {
            var previousAuth = authenticateForSetup();
            try {
                when(menuApi.getCoffee("latte"))
                        .thenReturn(Mono.just(aCoffee("latte", "espresso", "steamed milk")));

                existingOrderId = orderService.createOrder(aNewOrder("latte"))
                        .map(Order::getId)
                        .orElseThrow();
            } finally {
                setAuthentication(previousAuth);
            }
        }

        private Authentication authenticateForSetup() {
            var previousAuth = TestSecurityContextHolder.getContext().getAuthentication();
            TestSecurityContextHolder.setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "user", "password",
                            List.of(new SimpleGrantedAuthority("ROLE_ACCESS"))
                    )
            );
            return previousAuth;
        }

        private void setAuthentication(Authentication authentication) {
            if (authentication == null) {
                TestSecurityContextHolder.clearContext();
            } else {
                TestSecurityContextHolder.setAuthentication(authentication);
            }
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt die Order zurück, wenn sie existiert")
        void shouldReturnOrder_whenOrderExists() {
            var result = orderService.getOrder(existingOrderId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(existingOrderId);
            assertThat(result.get().getCoffeeType()).isEqualTo("latte");
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt Optional.empty() zurück, wenn die Order nicht existiert")
        void shouldReturnEmpty_whenOrderNotFound() {
            var result = orderService.getOrder("non-existing-id");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("wirft AuthenticationCredentialsNotFoundException, wenn kein User authentifiziert ist")
        void shouldThrowAuthenticationCredentialsNotFoundException_whenNotAuthenticated() {
            assertThatThrownBy(() -> orderService.getOrder(existingOrderId))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }

        @Test
        @WithMockUser(roles = "OTHER_ROLE")
        @DisplayName("wirft AuthorizationDeniedException, wenn User nicht die Rolle ACCESS hat")
        void shouldThrowAuthorizationDeniedException_whenUserMissesAccessRole() {
            assertThatThrownBy(() -> orderService.getOrder(existingOrderId))
                    .isInstanceOf(AuthorizationDeniedException.class);
        }
    }

    // =========================================================================
    // listOrders
    // =========================================================================

    @Nested
    @DisplayName("listOrders")
    class ListOrders {

        @BeforeEach
        void clearOrders() {
            orderService.clearOrders();
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt eine leere Liste zurück, wenn keine Orders existieren")
        void shouldReturnEmptyList_whenNoOrdersExist() {
            var result = orderService.listOrders();

            assertThat(result).isEmpty();
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt alle Orders zurück, die zuvor erstellt wurden")
        void shouldReturnAllCreatedOrders() {
            when(menuApi.getCoffee(anyString()))
                    .thenReturn(Mono.just(aCoffee("espresso")));

            orderService.createOrder(aNewOrder("espresso"));
            orderService.createOrder(aNewOrder("espresso"));

            var result = orderService.listOrders();

            assertThat(result).hasSize(2);
        }

        @Test
        @WithMockUser(roles = "ACCESS")
        @DisplayName("gibt eine unveränderliche Liste zurück")
        void shouldReturnUnmodifiableList() {
            var result = orderService.listOrders();

            assertThatThrownBy(() -> result.add(new Order()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("wirft AuthenticationCredentialsNotFoundException, wenn kein User authentifiziert ist")
        void shouldThrowAuthenticationCredentialsNotFoundException_whenNotAuthenticated() {
            assertThatThrownBy(() -> orderService.listOrders())
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }

        @Test
        @WithMockUser(roles = "OTHER_ROLE")
        @DisplayName("wirft AccessDeniedException, wenn User nicht die Rolle ACCESS hat")
        void shouldThrowAccessDeniedException_whenUserMissesAccessRole() {
            assertThatThrownBy(() -> orderService.listOrders())
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

}

package de.udocirkel.example.kcgravitee.coffeehouse.order;

import de.udocirkel.example.kcgravitee.coffeehouse.order.external.menu.MenuApi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final MenuApi menuApi;

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @PreAuthorize("hasRole('ACCESS')")
    public Optional<Order> createOrder(NewOrder newOrder) {
        var coffeeType = newOrder.getCoffeeType().toLowerCase();

        try {
            var coffee = Objects.requireNonNull(menuApi.getCoffee(coffeeType).block());

            var order = new Order()
                    .id(UUID.randomUUID().toString())
                    .coffeeType(coffeeType)
                    .totalPrice(coffee.getPrice())
                    .ingredients(coffee.getIngredients())
                    .status(Order.StatusEnum.CREATED)
                    .userId(newOrder.getUserId());

            orders.put(order.getId(), order);
            return Optional.of(order);
        } catch (WebClientResponseException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    @PreAuthorize("hasRole('ACCESS')")
    public Optional<Order> getOrder(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @PreAuthorize("hasRole('ACCESS')")
    public List<Order> listOrders() {
        return List.copyOf(orders.values());
    }

}

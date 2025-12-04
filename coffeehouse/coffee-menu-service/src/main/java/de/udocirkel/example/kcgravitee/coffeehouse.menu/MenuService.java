package de.udocirkel.example.kcgravitee.coffeehouse.menu;

import de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient.IngredientApi;
import de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient.IngredientList;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final IngredientApi ingredientApi;

    private final Map<String, Coffee> coffeeMenuMap = Stream.of(
                    new Coffee().type("espresso").price(new BigDecimal("2.5")),
                    new Coffee().type("latte").price(new BigDecimal("3.5")),
                    new Coffee().type("cappuccino").price(new BigDecimal("3.8"))
            )
            .collect(Collectors.toMap(Coffee::getType, c -> c));

    private Mono<Coffee> findCoffeeOnMenu(String coffeeType) {
        return Mono.justOrEmpty(coffeeType)
                .map(coffeeMenuMap::get)
                .map(this::copyCoffee);
    }

    @PreAuthorize("hasRole('ACCESS')")
    public Mono<Coffee> getCoffee(String coffeeType) {
        return findCoffeeOnMenu(coffeeType)
                .flatMap(coffee ->
                        ingredientApi.getIngredients(coffeeType)
                                .defaultIfEmpty(new IngredientList().ingredients(List.of()))
                                .map(ingredients -> coffee.ingredients(ingredients.getIngredients()))
                );
    }

    @PreAuthorize("hasRole('ACCESS')")
    public Flux<Coffee> listCoffees() {
        return ingredientApi.listIngredients()
                .collect(Collectors.toMap(
                        IngredientList::getCoffeeType,
                        IngredientList::getIngredients
                ))
                .flatMapMany(ingredientsMap ->
                        Flux.fromIterable(coffeeMenuMap.values())
                                .map(c -> copyCoffee(c)
                                        .ingredients(ingredientsMap.getOrDefault(c.getType(), List.of()))
                                )
                );
    }

    private Coffee copyCoffee(Coffee coffee) {
        return new Coffee()
                .type(coffee.getType())
                .price(coffee.getPrice());
    }

}

package de.udocirkel.example.kcgravitee.coffeehouse.menu;

import de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient.IngredientApi;
import de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient.IngredientApiClientConfig;
import de.udocirkel.example.kcgravitee.coffeehouse.menu.external.ingredient.IngredientList;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final IngredientApi ingredientApi;

    private final Map<String, Coffee> coffeeMap = Stream.of(
                    new Coffee()
                            .type("espresso")
                            .price(new BigDecimal("2.5")),
                    new Coffee()
                            .type("latte")
                            .price(new BigDecimal("3.5")),
                    new Coffee()
                            .type("cappuccino")
                            .price(new BigDecimal("3.8"))
            )
            .collect(Collectors.toMap(Coffee::getType, i -> i));

    @PreAuthorize("hasRole('ACCESS')")
    public Optional<Coffee> getCoffee(String coffeeType) {
        if (coffeeType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(coffeeMap.get(coffeeType))
                .map(coffee -> {
                    var ingredients = ingredientApi.getIngredients(coffeeType).block();
                    return new Coffee()
                            .type(coffee.getType())
                            .price(coffee.getPrice())
                            .ingredients(
                                    ingredients != null ? ingredients.getIngredients() : List.of()
                            );
                });
    }

    @PreAuthorize("hasRole('ACCESS')")
    public List<Coffee> listCoffees() {
        var ingredientsMap = ingredientApi.listIngredients().toStream()
                .filter(i -> i.getIngredients() != null)
                .collect(Collectors.toMap(
                        IngredientList::getCoffeeType,
                        IngredientList::getIngredients
                ));
        return coffeeMap.values().stream()
                .map(coffee -> new Coffee()
                        .type(coffee.getType())
                        .price(coffee.getPrice())
                        .ingredients(ingredientsMap.getOrDefault(coffee.getType(), List.of()))
                )
                .collect(Collectors.toList());
    }

}

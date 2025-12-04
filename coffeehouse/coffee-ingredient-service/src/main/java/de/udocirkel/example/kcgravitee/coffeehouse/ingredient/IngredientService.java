package de.udocirkel.example.kcgravitee.coffeehouse.ingredient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class IngredientService {

    private final Map<String, IngredientList> ingredientsMap = Stream.of(
                    new IngredientList()
                            .coffeeType("espresso")
                            .ingredients(List.of("coffee beans", "water")),
                    new IngredientList()
                            .coffeeType("latte")
                            .ingredients(List.of("espresso", "steamed milk")),
                    new IngredientList()
                            .coffeeType("cappuccino")
                            .ingredients(List.of("espresso", "steamed milk", "milk foam"))
            )
            .collect(Collectors.toMap(IngredientList::getCoffeeType, i -> i));

    @PreAuthorize("hasRole('ACCESS')")
    public Mono<IngredientList> getIngredients(String coffeeType) {
        IngredientList ingredient = ingredientsMap.get(coffeeType.toLowerCase());
        return ingredient != null ? Mono.just(ingredient) : Mono.empty();
    }

    @PreAuthorize("hasRole('ACCESS')")
    public Flux<IngredientList> listIngredients() {
        return Flux.fromIterable(ingredientsMap.values());
    }

}

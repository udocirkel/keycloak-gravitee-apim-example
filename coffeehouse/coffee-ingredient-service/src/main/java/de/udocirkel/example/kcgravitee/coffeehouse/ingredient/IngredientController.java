package de.udocirkel.example.kcgravitee.coffeehouse.ingredient;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class IngredientController implements IngredientApi {

    private final IngredientService ingredientService;

    @Override
    public Mono<ResponseEntity<IngredientList>> getIngredients(String coffeeType, ServerWebExchange exchange) {
        return ingredientService.getIngredients(coffeeType)
                .map(ResponseEntity::ok) // 200 OK
                .defaultIfEmpty(ResponseEntity.notFound().build()); // 404 Not Found
    }

    @Override
    public Mono<ResponseEntity<Flux<IngredientList>>> listIngredients(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok( // 200 OK
                ingredientService.listIngredients()));
    }

}

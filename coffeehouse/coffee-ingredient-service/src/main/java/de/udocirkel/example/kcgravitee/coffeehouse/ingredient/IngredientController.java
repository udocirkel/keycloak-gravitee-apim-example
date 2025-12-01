package de.udocirkel.example.kcgravitee.coffeehouse.ingredient;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class IngredientController implements IngredientApi {

    private final IngredientService ingredientService;

    @Override
    public ResponseEntity<IngredientList> getIngredients(String coffeeType) {
        return ingredientService.getIngredients(coffeeType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<IngredientList>> listIngredients() {
        return ResponseEntity.ok(ingredientService.listIngredients());
    }

}

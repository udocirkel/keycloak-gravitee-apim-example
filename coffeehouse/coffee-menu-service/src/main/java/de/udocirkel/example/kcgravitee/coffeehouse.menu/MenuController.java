package de.udocirkel.example.kcgravitee.coffeehouse.menu;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MenuController implements MenuApi {

    private final MenuService menuService;

    @Override
    public ResponseEntity<Coffee> getCoffee(String coffeeType) {
        return menuService.getCoffee(coffeeType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<Coffee>> listCoffees() {
        return ResponseEntity.ok(menuService.listCoffees());
    }

}

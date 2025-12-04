package de.udocirkel.example.kcgravitee.coffeehouse.menu;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class MenuController implements MenuApi {

    private final MenuService menuService;

    @Override
    public Mono<ResponseEntity<Coffee>> getCoffee(String coffeeType, ServerWebExchange exchange) {
        return menuService.getCoffee(coffeeType)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Override
    public Mono<ResponseEntity<Flux<Coffee>>> listCoffees(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok( // 200 OK
                menuService.listCoffees()));
    }

}

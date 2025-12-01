package de.udocirkel.example.kcgravitee.coffeehouse.order;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    public ResponseEntity<Order> createOrder(NewOrder newOrder) {
        return orderService.createOrder(newOrder)
                .map(o -> ResponseEntity.status(201).body(o))
                .orElseGet(() -> ResponseEntity.unprocessableEntity().build());
    }

    @Override
    public ResponseEntity<Order> getOrder(String orderId) {
        return orderService.getOrder(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<Order>> listOrders() {
        return ResponseEntity.ok(orderService.listOrders());
    }

}

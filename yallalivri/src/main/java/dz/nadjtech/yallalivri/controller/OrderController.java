package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderStatus;
import dz.nadjtech.yallalivri.service.OrderService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/status/{status}")
    public Flux<OrderDTO> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(required = false, defaultValue = "5") int hours) { // ✅ 5 heures max
        LocalDateTime since = LocalDateTime.now().minus(hours, ChronoUnit.HOURS);
        System.out.println("📡 Récupération des commandes '" + status + "' depuis : " + since);
        return orderService.getOrdersByStatusSince(status, since)
                .doOnNext(order -> System.out.println("🆕 Commande trouvée : " + order));
    }


    @GetMapping("/courier/{courierId}")
    public Flux<OrderDTO> getAllOrdersForCourier(@PathVariable Long courierId,
                                                 @RequestParam(required = false) String status) {
        if ("ASSIGNED".equalsIgnoreCase(status)) {
            return orderService.getOrdersByCourierAndStatus(courierId, OrderStatus.ASSIGNED);
        }
        return orderService.getAllOrderByCourierId(courierId);
    }


    @GetMapping("/store/{storeId}")
    public Flux<OrderDTO> getAllOrdersForStore(@PathVariable Long storeId,
                                               @RequestParam(required = false) Integer hours) {
        if (hours != null) {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            return orderService.getRecentOrdersByStoreId(storeId, since);
        }
        return orderService.getAllOrderByStoreId(storeId);
    }



    @GetMapping("/{id}")
    public Mono<OrderDTO> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping
    public Mono<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        return orderService.createOrder(orderDTO);
    }

    @PutMapping("/{id}")
    public Mono<OrderDTO> updateOrder(@PathVariable Long id, @RequestBody OrderDTO orderDTO) {
        return orderService.updateOrder(id, orderDTO);
    }

    @PatchMapping("/{id}/assign")
    public Mono<OrderDTO> assignOrderToCourier(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        return orderService.assignOrderToCourier(id, updates);
    }

    @PatchMapping("/{id}/status")
    public Mono<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates) {
        OrderStatus newStatus = OrderStatus.valueOf(updates.get("status"));
        return orderService.updateOrderStatus(id, newStatus);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteOrder(@PathVariable Long id) {
        return orderService.deleteOrder(id);
    }
}

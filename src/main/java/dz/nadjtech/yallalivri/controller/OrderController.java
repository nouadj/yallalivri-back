package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.AssignOrderRequest;
import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderDisplayDTO;
import dz.nadjtech.yallalivri.dto.OrderStatusUpdateRequest;
import dz.nadjtech.yallalivri.enums.OrderStatus;
import dz.nadjtech.yallalivri.security.CurrentUser;
import dz.nadjtech.yallalivri.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(orderService.getAllOrders(currentUser));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDisplayDTO>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(name = "idCourier", required = false) Long idCourier,
            @RequestParam(name = "courierId", required = false) Long courierId,
            @RequestParam(required = false, defaultValue = "20") Integer distance,
            @RequestParam(required = false, defaultValue = "5") int hours,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        if (hours <= 0 || distance <= 0) throw new IllegalArgumentException("Hours and distance must be positive");
        Long resolvedCourierId = courierId != null ? courierId : idCourier;
        LocalDateTime since = LocalDateTime.now().minus(hours, ChronoUnit.HOURS);
        OrderStatus resolvedStatus = OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(orderService.getOrdersByStatusSinceWithDistance(
                resolvedStatus,
                since,
                distance,
                resolvedCourierId,
                currentUser
        ));
    }

    @GetMapping("/courier/{courierId}")
    public ResponseEntity<List<OrderDisplayDTO>> getAllOrdersForCourier(
            @PathVariable Long courierId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(orderService.getAllOrderByCourierId(courierId, status, currentUser));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<OrderDisplayDTO>> getAllOrdersForStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) Integer hours,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(orderService.getAllOrderByStoreId(storeId, hours, currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(orderService.getOrderById(id, currentUser));
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @Valid @RequestBody OrderDTO orderDTO,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(orderService.createOrder(orderDTO, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderDTO orderDTO,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(orderService.updateOrder(id, orderDTO, currentUser));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<OrderDTO> assignOrderToCourier(
            @PathVariable Long id,
            @Valid @RequestBody AssignOrderRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(orderService.assignOrderToCourier(id, request.getCourierId(), currentUser));
    }

    @PatchMapping("/{id}/unassign")
    public ResponseEntity<OrderDTO> unassignOrderToCourier(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(orderService.unassignOrderToCourier(id, currentUser));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        OrderStatus resolvedStatus = OrderStatus.valueOf(request.getStatus().toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(orderService.updateOrderStatus(id, resolvedStatus, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        orderService.deleteOrder(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}

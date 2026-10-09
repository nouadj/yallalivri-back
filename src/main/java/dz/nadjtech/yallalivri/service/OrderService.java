package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderDisplayDTO;
import dz.nadjtech.yallalivri.enums.OrderStatus;
import dz.nadjtech.yallalivri.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    List<OrderDTO> getAllOrders(CurrentUser currentUser);
    OrderDTO getOrderById(Long id, CurrentUser currentUser);
    OrderDTO createOrder(OrderDTO orderDTO, CurrentUser currentUser);
    OrderDTO updateOrder(Long id, OrderDTO orderDTO, CurrentUser currentUser);
    OrderDTO updateOrderStatus(Long id, OrderStatus newOrderStatus, CurrentUser currentUser);
    void deleteOrder(Long id, CurrentUser currentUser);
    List<OrderDisplayDTO> getAllOrderByCourierId(Long courierId, String status, CurrentUser currentUser);
    List<OrderDisplayDTO> getAllOrderByStoreId(Long storeId, Integer hours, CurrentUser currentUser);
    List<OrderDisplayDTO> getOrdersByStatusSinceWithDistance(OrderStatus status, LocalDateTime since, Integer distance, Long courierId, CurrentUser currentUser);
    OrderDTO assignOrderToCourier(Long id, Long courierId, CurrentUser currentUser);
    OrderDTO unassignOrderToCourier(Long id, CurrentUser currentUser);
}

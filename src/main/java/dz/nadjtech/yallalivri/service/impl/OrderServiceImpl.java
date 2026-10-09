package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderDisplayDTO;
import dz.nadjtech.yallalivri.entity.Order;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.enums.OrderStatus;
import dz.nadjtech.yallalivri.enums.UserRole;
import dz.nadjtech.yallalivri.mapper.OrderMapper;
import dz.nadjtech.yallalivri.repository.OrderRepository;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.security.CurrentUser;
import dz.nadjtech.yallalivri.service.NotificationService;
import dz.nadjtech.yallalivri.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            OrderMapper orderMapper,
            NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders(CurrentUser currentUser) {
        assertAdmin(currentUser);
        return orderRepository.findAllDetailed().stream().map(orderMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id, CurrentUser currentUser) {
        Order order = getOrderOrThrow(id);
        assertCanReadOrder(order, currentUser);
        return orderMapper.toDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO, CurrentUser currentUser) {
        if (currentUser.role() != UserRole.ADMIN && currentUser.role() != UserRole.STORE) {
            throw new AccessDeniedException("Only ADMIN or STORE can create orders");
        }

        User store = userRepository.findById(orderDTO.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));

        if (store.getRole() != UserRole.STORE) {
            throw new IllegalArgumentException("Selected user is not a store");
        }

        if (currentUser.role() == UserRole.STORE && !store.getId().equals(currentUser.id())) {
            throw new AccessDeniedException("Store can create only its own orders");
        }

        Order order = Order.builder()
                .store(store)
                .customerName(orderDTO.getCustomerName())
                .customerPhone(orderDTO.getCustomerPhone())
                .customerAddress(orderDTO.getCustomerAddress())
                .amount(orderDTO.getAmount())
                .deliveryFee(orderDTO.getDeliveryFee())
                .status(OrderStatus.CREATED)
                .build();

        Order saved = orderRepository.save(order);
        notificationService.notifyCouriersForNewOrder(saved.getId());
        return orderMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public OrderDTO updateOrder(Long id, OrderDTO orderDTO, CurrentUser currentUser) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (!currentUser.isAdmin() && !Objects.equals(order.getStore().getId(), currentUser.id())) {
            throw new AccessDeniedException("Only ADMIN or owner STORE can update an order");
        }

        if (!currentUser.isAdmin() && order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Store can update only CREATED orders");
        }

        order.setCustomerName(orderDTO.getCustomerName());
        order.setCustomerPhone(orderDTO.getCustomerPhone());
        order.setCustomerAddress(orderDTO.getCustomerAddress());
        order.setAmount(orderDTO.getAmount());
        order.setDeliveryFee(orderDTO.getDeliveryFee());

        return orderMapper.toDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long id, OrderStatus newOrderStatus, CurrentUser currentUser) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (!currentUser.isAdmin()) {
            if (currentUser.role() == UserRole.COURIER) {
                if (order.getCourier() == null || !Objects.equals(order.getCourier().getId(), currentUser.id())) {
                    throw new AccessDeniedException("Courier can update only his assigned orders");
                }
            } else if (currentUser.role() == UserRole.STORE) {
                boolean isOwnerStore = Objects.equals(order.getStore().getId(), currentUser.id());
                boolean canCancelCreatedOrder = isOwnerStore
                        && order.getStatus() == OrderStatus.CREATED
                        && newOrderStatus == OrderStatus.CANCELLED;
                if (!canCancelCreatedOrder) {
                    throw new AccessDeniedException("Store can only cancel its CREATED orders");
                }
            } else {
                throw new AccessDeniedException("Unauthorized");
            }
        }

        if (newOrderStatus == OrderStatus.ASSIGNED) {
            throw new IllegalArgumentException("Use the assign endpoint to select a courier");
        }

        if (!isValidTransition(order.getStatus(), newOrderStatus)) {
            throw new IllegalArgumentException("Invalid status transition");
        }

        order.setStatus(newOrderStatus);

        if (newOrderStatus == OrderStatus.DELIVERED) {
            notificationService.notifyStore(order.getId(), "livrée");
        } else if (newOrderStatus == OrderStatus.RETURNED) {
            notificationService.notifyStore(order.getId(), "retournée");
        } else if (newOrderStatus == OrderStatus.SHIPPED) {
            notificationService.notifyStore(order.getId(), "en cours de livraison");
        } else if (newOrderStatus == OrderStatus.CANCELLED) {
            notificationService.notifyStore(order.getId(), "annulée");
        }

        return orderMapper.toDTO(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id, CurrentUser currentUser) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (!currentUser.isAdmin() && !Objects.equals(order.getStore().getId(), currentUser.id())) {
            throw new AccessDeniedException("Only ADMIN or owner STORE can delete an order");
        }

        if (!currentUser.isAdmin()
                && order.getStatus() != OrderStatus.CREATED
                && order.getStatus() != OrderStatus.RETURNED
                && order.getStatus() != OrderStatus.CANCELLED) {
            throw new AccessDeniedException("Store cannot delete this order status");
        }

        orderRepository.delete(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDisplayDTO> getAllOrderByCourierId(Long courierId, String status, CurrentUser currentUser) {
        if (!currentUser.isAdmin() && !courierId.equals(currentUser.id())) {
            throw new AccessDeniedException("Unauthorized");
        }

        if (status != null && !status.isBlank()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));
            return orderRepository.findByCourier_IdAndStatusOrderByUpdatedAtDesc(courierId, orderStatus)
                    .stream()
                    .map(orderMapper::toDisplayDTO)
                    .toList();
        }

        return orderRepository.findByCourier_IdOrderByUpdatedAtDesc(courierId)
                .stream()
                .map(orderMapper::toDisplayDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDisplayDTO> getAllOrderByStoreId(Long storeId, Integer hours, CurrentUser currentUser) {
        if (!currentUser.isAdmin() && !storeId.equals(currentUser.id())) {
            throw new AccessDeniedException("Unauthorized");
        }

        if (hours != null && hours <= 0) throw new IllegalArgumentException("Hours must be positive");
        if (hours != null) {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            return orderRepository.findByStore_IdAndCreatedAtAfterOrderByUpdatedAtDesc(storeId, since)
                    .stream()
                    .map(orderMapper::toDisplayDTO)
                    .toList();
        }

        return orderRepository.findByStore_IdOrderByUpdatedAtDesc(storeId)
                .stream()
                .map(orderMapper::toDisplayDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDisplayDTO> getOrdersByStatusSinceWithDistance(
            OrderStatus status,
            LocalDateTime since,
            Integer distance,
            Long courierId,
            CurrentUser currentUser
    ) {
        if (courierId == null) {
            throw new IllegalArgumentException("courierId is required");
        }
        if (!currentUser.isAdmin() && !courierId.equals(currentUser.id())) {
            throw new AccessDeniedException("Unauthorized");
        }

        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new EntityNotFoundException("Courier not found"));

        if (courier.getRole() != UserRole.COURIER) {
            throw new IllegalArgumentException("User is not a courier");
        }

        if (courier.getLatitude() == null || courier.getLongitude() == null) {
            return List.of();
        }

        if (!currentUser.isAdmin() && status != OrderStatus.CREATED) {
            throw new AccessDeniedException("Only available orders can be browsed");
        }
        int maxDistanceKm = distance == null ? 20 : distance;
        if (maxDistanceKm <= 0) throw new IllegalArgumentException("Distance must be positive");

        return orderRepository.findByStatusAndCreatedAtAfterOrderByUpdatedAtDesc(status, since)
                .stream()
                .filter(order -> order.getStore().getLatitude() != null && order.getStore().getLongitude() != null)
                .filter(order -> {
                    double calculatedDistance = calculateDistanceKm(
                            courier.getLatitude(),
                            courier.getLongitude(),
                            order.getStore().getLatitude(),
                            order.getStore().getLongitude()
                    );
                    return calculatedDistance <= maxDistanceKm;
                })
                .map(orderMapper::toDisplayDTO)
                .toList();
    }

    @Override
    @Transactional
    public OrderDTO assignOrderToCourier(Long id, Long courierId, CurrentUser currentUser) {
        if (currentUser.role() != UserRole.ADMIN && currentUser.role() != UserRole.COURIER) {
            throw new AccessDeniedException("Only ADMIN or COURIER can assign orders");
        }

        if (currentUser.role() == UserRole.COURIER && !courierId.equals(currentUser.id())) {
            throw new AccessDeniedException("Courier can assign only to himself");
        }

        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (order.getCourier() != null) {
            throw new IllegalStateException("Order already assigned");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Only CREATED orders can be assigned");
        }

        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new EntityNotFoundException("Courier not found"));

        if (courier.getRole() != UserRole.COURIER) {
            throw new IllegalArgumentException("Selected user is not a courier");
        }

        order.setCourier(courier);
        order.setStatus(OrderStatus.ASSIGNED);
        notificationService.notifyStore(order.getId(), "assignée à un livreur");

        return orderMapper.toDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO unassignOrderToCourier(Long id, CurrentUser currentUser) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (!currentUser.isAdmin()) {
            boolean isAssignedCourier = currentUser.role() == UserRole.COURIER
                    && order.getCourier() != null
                    && Objects.equals(order.getCourier().getId(), currentUser.id());
            boolean isOwnerStore = currentUser.role() == UserRole.STORE
                    && Objects.equals(order.getStore().getId(), currentUser.id());

            if (!isAssignedCourier && !isOwnerStore) {
                throw new AccessDeniedException("Unauthorized");
            }
        }

        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new IllegalStateException("Only ASSIGNED orders can be unassigned");
        }

        order.setCourier(null);
        order.setStatus(OrderStatus.CREATED);
        return orderMapper.toDTO(order);
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findDetailedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    private void assertAdmin(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    private void assertCanReadOrder(Order order, CurrentUser currentUser) {
        if (currentUser.isAdmin()) return;

        boolean isStoreOwner = currentUser.role() == UserRole.STORE
                && Objects.equals(order.getStore().getId(), currentUser.id());
        boolean isAssignedCourier = currentUser.role() == UserRole.COURIER
                && order.getCourier() != null
                && Objects.equals(order.getCourier().getId(), currentUser.id());

        if (!isStoreOwner && !isAssignedCourier) {
            throw new AccessDeniedException("Unauthorized");
        }
    }

    private boolean isValidTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        return switch (oldStatus) {
            case CREATED -> newStatus == OrderStatus.ASSIGNED || newStatus == OrderStatus.CANCELLED;
            case ASSIGNED -> newStatus == OrderStatus.SHIPPED
                    || newStatus == OrderStatus.RETURNED
                    || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.RETURNED;
            case CANCELLED, RETURNED, DELIVERED -> false;
        };
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double latDiff = Math.toRadians(lat2 - lat1);
        double lonDiff = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}

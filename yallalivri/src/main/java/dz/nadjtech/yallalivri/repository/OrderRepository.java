package dz.nadjtech.yallalivri.repository;

import dz.nadjtech.yallalivri.dto.OrderStatus;
import dz.nadjtech.yallalivri.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    Flux<Order> findByStatusAndCreatedAtAfter(String status, LocalDateTime createdAt);
    Flux<Order> findByCourierIdAndStatus(Long courierId, OrderStatus status);
    Flux<Order>  findByStoreIdAndCreatedAtAfter(Long storeId, LocalDateTime since);
    Flux<Order> findByStoreIdOrderByUpdatedAtDesc(Long storeId);
    Flux<Order> findByCourierIdOrderByUpdatedAtDesc(Long courierId);
}

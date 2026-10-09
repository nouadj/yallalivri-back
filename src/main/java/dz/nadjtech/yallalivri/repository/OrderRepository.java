package dz.nadjtech.yallalivri.repository;

import dz.nadjtech.yallalivri.entity.Order;
import dz.nadjtech.yallalivri.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"store", "courier"})
    @Query("select o from Order o order by o.updatedAt desc")
    List<Order> findAllDetailed();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    boolean existsByStore_IdOrCourier_Id(Long storeId, Long courierId);

    @EntityGraph(attributePaths = {"store", "courier"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"store", "courier"})
    List<Order> findByStatusAndCreatedAtAfterOrderByUpdatedAtDesc(OrderStatus status, LocalDateTime createdAt);

    @EntityGraph(attributePaths = {"store", "courier"})
    List<Order> findByCourier_IdAndStatusOrderByUpdatedAtDesc(Long courierId, OrderStatus status);

    @EntityGraph(attributePaths = {"store", "courier"})
    List<Order> findByStore_IdAndCreatedAtAfterOrderByUpdatedAtDesc(Long storeId, LocalDateTime since);

    @EntityGraph(attributePaths = {"store", "courier"})
    List<Order> findByStore_IdOrderByUpdatedAtDesc(Long storeId);

    @EntityGraph(attributePaths = {"store", "courier"})
    List<Order> findByCourier_IdOrderByUpdatedAtDesc(Long courierId);
}

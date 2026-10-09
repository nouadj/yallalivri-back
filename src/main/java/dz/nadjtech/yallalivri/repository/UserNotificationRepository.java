package dz.nadjtech.yallalivri.repository;
import dz.nadjtech.yallalivri.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface UserNotificationRepository extends JpaRepository<UserNotification,Long> {
    List<UserNotification> findByUser_IdOrderByIdDesc(Long userId, Pageable pageable);
    long countByUser_IdAndReadAtIsNull(Long userId);
    Optional<UserNotification> findByIdAndUser_Id(Long id, Long userId);
    @Modifying @Query("update UserNotification n set n.readAt=:now where n.user.id=:userId and n.readAt is null")
    void markAllRead(@Param("userId") Long userId,@Param("now") LocalDateTime now);
}

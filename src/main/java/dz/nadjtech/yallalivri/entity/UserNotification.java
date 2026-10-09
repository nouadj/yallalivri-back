package dz.nadjtech.yallalivri.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="user_notifications")
public class UserNotification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(name="order_id",nullable=false) private Long orderId;
    @Column(nullable=false,length=40) private String type;
    @Column(nullable=false,length=180) private String title;
    @Column(nullable=false,length=500) private String body;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="read_at") private LocalDateTime readAt;
    @PrePersist void onCreate() { createdAt=LocalDateTime.now(java.time.Clock.systemUTC()); }
}

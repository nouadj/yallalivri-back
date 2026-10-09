package dz.nadjtech.yallalivri.dto;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import dz.nadjtech.yallalivri.entity.UserNotification;
public record NotificationDTO(Long id,Long orderId,String type,String title,String body,OffsetDateTime createdAt,OffsetDateTime readAt) {
    public static NotificationDTO from(UserNotification n) { return new NotificationDTO(n.getId(),n.getOrderId(),n.getType(),n.getTitle(),n.getBody(),n.getCreatedAt().atOffset(ZoneOffset.UTC),n.getReadAt()==null?null:n.getReadAt().atOffset(ZoneOffset.UTC)); }
}

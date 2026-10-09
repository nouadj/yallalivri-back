package dz.nadjtech.yallalivri.controller;
import dz.nadjtech.yallalivri.dto.NotificationDTO;
import dz.nadjtech.yallalivri.repository.UserNotificationRepository;
import dz.nadjtech.yallalivri.security.CurrentUser;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    private final UserNotificationRepository repository;
    public NotificationController(UserNotificationRepository repository) { this.repository=repository; }
    public record Inbox(List<NotificationDTO> items,long unreadCount) {}
    @GetMapping @Transactional(readOnly=true)
    public Inbox getInbox(@AuthenticationPrincipal CurrentUser user) {
        return new Inbox(repository.findByUser_IdOrderByIdDesc(user.id(),PageRequest.of(0,100)).stream().map(NotificationDTO::from).toList(),repository.countByUser_IdAndReadAtIsNull(user.id()));
    }
    @PatchMapping("/{id}/read") @Transactional
    public NotificationDTO read(@PathVariable Long id,@AuthenticationPrincipal CurrentUser user) {
        var notification=repository.findByIdAndUser_Id(id,user.id()).orElseThrow(()->new EntityNotFoundException("Notification not found"));
        if(notification.getReadAt()==null) notification.setReadAt(LocalDateTime.now(java.time.Clock.systemUTC()));
        return NotificationDTO.from(notification);
    }
    @PostMapping("/read-all") @Transactional @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal CurrentUser user) { repository.markAllRead(user.id(),LocalDateTime.now(java.time.Clock.systemUTC())); }
}

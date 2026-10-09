package dz.nadjtech.yallalivri.service.impl;
import dz.nadjtech.yallalivri.config.NotificationProperties;
import dz.nadjtech.yallalivri.repository.UserNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.RestClient;
import java.util.Map;
@Service
public class ExpoPushService {
    private static final Logger log=LoggerFactory.getLogger(ExpoPushService.class);
    private final UserNotificationRepository repository;
    private final RestClient client;
    private final NotificationProperties properties;
    public ExpoPushService(UserNotificationRepository repository,RestClient client,NotificationProperties properties) { this.repository=repository;this.client=client;this.properties=properties; }
    @Async @Transactional(readOnly=true,propagation=Propagation.REQUIRES_NEW)
    public void send(Long notificationId) {
        if(!properties.enabled()) return;
        repository.findById(notificationId).ifPresent(n->{
            String token=n.getUser().getNotificationToken();
            if(token==null || !(token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["))) return;
            try {
                var response=client.post().uri("/--/api/v2/push/send").body(Map.of("to",token,"title",n.getTitle(),"body",n.getBody(),"sound","default","channelId","orders","data",Map.of("notificationId",n.getId(),"orderId",n.getOrderId())))
                    .retrieve().body(com.fasterxml.jackson.databind.JsonNode.class);
                if(response!=null && response.path("data").path("status").asText().equals("error")) {
                    log.warn("Expo rejected notification {}: {}",notificationId,response.path("data").path("message").asText());
                }
            } catch(Exception e) { log.warn("Push failed for notification {}: {}",notificationId,e.getMessage()); }
        });
    }
}

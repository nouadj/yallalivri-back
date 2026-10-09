package dz.nadjtech.yallalivri.service.impl;
import dz.nadjtech.yallalivri.config.NotificationProperties;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.entity.UserNotification;
import dz.nadjtech.yallalivri.enums.UserRole;
import dz.nadjtech.yallalivri.repository.OrderRepository;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.repository.UserNotificationRepository;
import dz.nadjtech.yallalivri.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
@Service @Transactional
public class NotificationServiceImpl implements NotificationService {
    private final OrderRepository orders;
    private final UserRepository users;
    private final UserNotificationRepository notifications;
    private final NotificationProperties properties;
    private final ExpoPushService push;
    public NotificationServiceImpl(OrderRepository orders,UserRepository users,UserNotificationRepository notifications,NotificationProperties properties,ExpoPushService push) {
        this.orders=orders;this.users=users;this.notifications=notifications;this.properties=properties;this.push=push;
    }
    @Override public void notifyCouriersForNewOrder(Long orderId) {
        var order=orders.findDetailedById(orderId).orElseThrow();
        User store=order.getStore();
        if(store.getLatitude()==null || store.getLongitude()==null) return;
        users.findAllByRole(UserRole.COURIER).stream()
            .filter(user->user.getLatitude()!=null && user.getLongitude()!=null)
            .filter(user->distance(store.getLatitude(),store.getLongitude(),user.getLatitude(),user.getLongitude())<=properties.courierRadiusKm())
            .forEach(user->create(user,orderId,"ORDER_AVAILABLE","Nouvelle livraison disponible",store.getName()+" propose la commande #"+orderId+"."));
    }
    @Override public void notifyStore(Long orderId,String message) {
        var order=orders.findDetailedById(orderId).orElseThrow();
        create(order.getStore(),orderId,"ORDER_"+order.getStatus().name(),"Commande #"+orderId+" "+message,"Votre commande est "+message+".");
        if(order.getStatus()==dz.nadjtech.yallalivri.enums.OrderStatus.CANCELLED && order.getCourier()!=null)
            create(order.getCourier(),orderId,"ORDER_CANCELLED","Livraison annulée","La commande #"+orderId+" a été annulée.");
    }
    private void create(User user,Long orderId,String type,String title,String body) {
        var saved=notifications.save(UserNotification.builder().user(user).orderId(orderId).type(type).title(title).body(body).build());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { push.send(saved.getId()); }
        });
    }
    private double distance(double lat1,double lon1,double lat2,double lon2) {
        double dLat=Math.toRadians(lat2-lat1),dLon=Math.toRadians(lon2-lon1);
        double a=Math.pow(Math.sin(dLat/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(dLon/2),2);
        return 6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(Math.max(0,1-a)));
    }
}

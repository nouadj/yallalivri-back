package dz.nadjtech.yallalivri.service;

public interface NotificationService {
    void notifyCouriersForNewOrder(Long orderId);
    void notifyStore(Long orderId, String message);
}

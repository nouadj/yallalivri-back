package dz.nadjtech.yallalivri.mapper;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderDisplayDTO;
import dz.nadjtech.yallalivri.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderDTO toDTO(Order order) {
        if (order == null) return null;

        return OrderDTO.builder()
                .id(order.getId())
                .storeId(order.getStore().getId())
                .courierId(order.getCourier() != null ? order.getCourier().getId() : null)
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .customerAddress(order.getCustomerAddress())
                .status(order.getStatus())
                .amount(order.getAmount())
                .deliveryFee(order.getDeliveryFee())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .storeLatitude(order.getStore().getLatitude())
                .storeLongitude(order.getStore().getLongitude())
                .build();
    }

    public OrderDisplayDTO toDisplayDTO(Order order) {
        if (order == null) return null;

        OrderDisplayDTO dto = new OrderDisplayDTO();
        dto.setId(order.getId());
        dto.setStoreId(order.getStore().getId());
        dto.setCourierId(order.getCourier() != null ? order.getCourier().getId() : null);
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setCustomerAddress(order.getCustomerAddress());
        dto.setStatus(order.getStatus());
        dto.setAmount(order.getAmount());
        dto.setDeliveryFee(order.getDeliveryFee());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setStoreLatitude(order.getStore().getLatitude());
        dto.setStoreLongitude(order.getStore().getLongitude());
        dto.setStoreName(order.getStore().getName());
        dto.setStorePhone(order.getStore().getPhone());
        dto.setCourierPhone(order.getCourier() != null ? order.getCourier().getPhone() : null);
        dto.setStoreAddress(order.getStore().getAddress());
        dto.setCourierName(order.getCourier() != null ? order.getCourier().getName() : "");
        return dto;
    }
}

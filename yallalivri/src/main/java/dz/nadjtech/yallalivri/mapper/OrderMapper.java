package dz.nadjtech.yallalivri.mapper;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderDisplayDTO;
import dz.nadjtech.yallalivri.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    static public OrderDTO toDTO(Order order) {
        if (order == null) return null;

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setStoreId(order.getStoreId());
        dto.setCourierId(order.getCourierId());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setCustomerAddress(order.getCustomerAddress());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        return dto;
    }

    public static OrderDisplayDTO toDisplayDTO(Order order, String storeName, String storeAddress, String courierName) {
        if (order == null) return null;

        OrderDisplayDTO dto = new OrderDisplayDTO();

        dto.setId(order.getId());
        dto.setStoreId(order.getStoreId());
        dto.setCourierId(order.getCourierId());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setCustomerAddress(order.getCustomerAddress());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        dto.setStoreName(storeName);
        dto.setStoreAddress(storeAddress);
        dto.setCourierName(courierName);

        return dto;
    }


    public static Order toEntity(OrderDTO dto) {
        if (dto == null) return null;

        Order order = new Order();
        order.setId(dto.getId());
        order.setStoreId(dto.getStoreId());
        order.setCourierId(dto.getCourierId());
        order.setCustomerName(dto.getCustomerName());
        order.setCustomerPhone(dto.getCustomerPhone());
        order.setCustomerAddress(dto.getCustomerAddress());
        order.setStatus(dto.getStatus());
        order.setTotalAmount(dto.getTotalAmount());
        order.setCreatedAt(dto.getCreatedAt());
        order.setUpdatedAt(dto.getUpdatedAt());

        return order;
    }
}

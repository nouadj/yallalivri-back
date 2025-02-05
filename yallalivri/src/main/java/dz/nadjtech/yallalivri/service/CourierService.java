package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.CourierDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CourierService {
    Flux<CourierDTO> getAllCouriers();
    Mono<CourierDTO> getCourierById(Long id);
    Mono<CourierDTO> createCourier(CourierDTO courierDTO);
    Mono<CourierDTO> updateCourier(Long id, CourierDTO courierDTO);
    Mono<Void> deleteCourier(Long id);
}

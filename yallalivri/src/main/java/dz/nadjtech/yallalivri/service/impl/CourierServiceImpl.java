package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.CourierDTO;
import dz.nadjtech.yallalivri.entity.Courier;
import dz.nadjtech.yallalivri.entity.Store;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.mapper.CourierMapper;
import dz.nadjtech.yallalivri.repository.CourierRepository;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.service.CourierService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CourierServiceImpl implements CourierService {

    private final CourierRepository courierRepository;
    private final UserRepository userRepository;
    private final CourierMapper courierMapper;

    public CourierServiceImpl(CourierRepository courierRepository, CourierMapper courierMapper, UserRepository userRepository) {
        this.courierRepository = courierRepository;
        this.courierMapper = courierMapper;
        this.userRepository = userRepository;
    }

    @Override
    public Flux<CourierDTO> getAllCouriers() {
        return courierRepository.findAll()
                .map(courierMapper::toDTO);
    }

    @Override
    public Mono<CourierDTO> getCourierById(Long id) {
        return Mono.zip(
                        courierRepository.findById(id),
                        userRepository.findById(id)
                )
                .map(tuple -> {
                    Courier courier = tuple.getT1();
                    User user = tuple.getT2();
                    courier.setName(user.getName());
                    return courier;
                })
                .map(courierMapper::toDTO);
    }



    @Override
    public Mono<CourierDTO> createCourier(CourierDTO courierDTO) {
        Courier courier = courierMapper.toEntity(courierDTO);
        return courierRepository.save(courier)
                .map(courierMapper::toDTO);
    }

    @Override
    public Mono<CourierDTO> updateCourier(Long id, CourierDTO courierDTO) {
        return courierRepository.findById(id)
                .flatMap(existingCourier -> {
                    existingCourier.setName(courierDTO.getName());
                    //existingCourier.setDateOfBirth(courierDTO.getDateOfBirth());
                    return courierRepository.save(existingCourier);
                })
                .map(courierMapper::toDTO);
    }

    @Override
    public Mono<Void> deleteCourier(Long id) {
        return courierRepository.deleteById(id);
    }
}

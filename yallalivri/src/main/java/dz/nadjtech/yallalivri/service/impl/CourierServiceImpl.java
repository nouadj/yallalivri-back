package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.CourierDTO;
import dz.nadjtech.yallalivri.dto.CourierDTOWithPassword;
import dz.nadjtech.yallalivri.dto.UserDTOWithPassword;
import dz.nadjtech.yallalivri.dto.UserRole;
import dz.nadjtech.yallalivri.entity.Courier;
import dz.nadjtech.yallalivri.mapper.CourierMapper;
import dz.nadjtech.yallalivri.repository.CourierRepository;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.service.CourierService;
import dz.nadjtech.yallalivri.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.Period;

@Service
public class CourierServiceImpl implements CourierService {

    private final CourierRepository courierRepository;
    private final UserRepository userRepository;
    private final CourierMapper courierMapper;
    private final UserService userService;
    private final TransactionalOperator transactionalOperator;

    public CourierServiceImpl(CourierRepository courierRepository, CourierMapper courierMapper, UserRepository userRepository, UserService userService, TransactionalOperator transactionalOperator) {
        this.courierRepository = courierRepository;
        this.courierMapper = courierMapper;
        this.userRepository = userRepository;
        this.userService = userService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Flux<CourierDTO> getAllCouriers() {
        return courierRepository.findAll()
                .map(courierMapper::toDTO);
    }

    @Override
    public Mono<CourierDTO> getCourierById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")))
                .flatMap(user -> courierRepository.findById(id)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Livreur non trouvé")))
                        .map(courier -> {
                            // 🔥 Mapper User + Courier vers CourierDTO
                            CourierDTO courierDTO = new CourierDTO();
                            courierDTO.setId(user.getId());
                            courierDTO.setName(user.getName());
                            courierDTO.setPhone(user.getPhone());
                            if (courier.getDateOfBirth() != null) {
                                int age = Period.between(courier.getDateOfBirth(), LocalDate.now()).getYears();
                                courierDTO.setAge(age);
                            }
                            return courierDTO;
                        }));
    }


    @Override
    public Mono<CourierDTO> createCourier(CourierDTOWithPassword courierDTOWithPassword) {
        UserDTOWithPassword userDTOWithPassword = new UserDTOWithPassword();
        BeanUtils.copyProperties(courierDTOWithPassword, userDTOWithPassword);
        userDTOWithPassword.setRole(UserRole.COURIER);

        return userService.createUser(userDTOWithPassword) // 🔥 Étape 1 : Créer l'utilisateur
                .flatMap(user -> userRepository.findById(user.getId()) // 🔥 Étape 2 : S'assurer qu'il existe
                        .switchIfEmpty(Mono.error(new IllegalStateException("Utilisateur non enregistré en base")))
                        .flatMap(persistedUser -> {
                            Courier courier = courierMapper.toEntity(courierDTOWithPassword);
                            courier.setId(persistedUser.getId()); // 🔥 Assurer que l’ID existe
                            return courierRepository.save(courier); // 🔥 Étape 3 : Insérer Courier avec un ID valide
                        })
                )
                .map(courierMapper::toDTO)
                .as(transactionalOperator::transactional);
    }


    @Override
    public Mono<CourierDTO> updateCourier(Long id, CourierDTO courierDTO) {
        return courierRepository.findById(id)
                .flatMap(existingCourier -> {
                    //existingCourier.setAge(courierDTO.get());
                    return courierRepository.save(existingCourier);
                })
                .map(courierMapper::toDTO);
    }

    @Override
    public Mono<Void> deleteCourier(Long id) {
        return courierRepository.deleteById(id);
    }
}

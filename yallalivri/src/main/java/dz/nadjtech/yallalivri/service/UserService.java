package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.dto.UserDTOWithPassword;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface UserService {
    Flux<UserDTO> getAllUsers();
    Mono<UserDTO> getUserById(Long id);
    Mono<UserDTO> createUser(UserDTOWithPassword userDTO);
    Mono<UserDTO> updateUser(Long id, UserDTOWithPassword userDTO);
    Mono<Void> deleteUser(Long id);
    Mono<UserDTO> findByEmail(String email);
    Mono<UserDTOWithPassword> findByEmailWithPassword(String email);

    Mono<UserDTO> updateNotificationToken(Long id, String token);

    Mono<Object> patchUser(Long id, Map<String, Object> updates);

    Mono<Void> patchUserPassword(Long id, Map<String, Object> updates);

    Mono<UserDTO> updateUserLocation(Long id, Double latitude, Double longitude);

}

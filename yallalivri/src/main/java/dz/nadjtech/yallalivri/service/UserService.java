package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.dto.UserWithPasswordDTO;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface UserService {
    Flux<UserDTO> getAllUsers();
    Mono<UserDTO> getUserById(Long id);
    Mono<UserDTO> createUser(UserWithPasswordDTO userDTO);
    Mono<UserDTO> updateUser(Long id, UserWithPasswordDTO userDTO);
    Mono<Void> deleteUser(Long id);
    Mono<UserDTO> findByEmail(String email);
    Mono<UserWithPasswordDTO> findByEmailWithPassword(String email);

    Mono<UserDTO> updateNotificationToken(Long id, String token);

    Mono<Object> patchUser(Long id, Map<String, Object> updates);

    Mono<Void> patchUserPassword(Long id, Map<String, Object> updates);
}

package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.dto.UserWithPasswordDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<UserDTO> getAllUsers();
    Mono<UserDTO> getUserById(Long id);
    Mono<UserDTO> createUser(UserWithPasswordDTO userDTO);
    Mono<UserDTO> updateUser(Long id, UserWithPasswordDTO userDTO);
    Mono<Void> deleteUser(Long id);
    Mono<UserDTO> findByEmail(String email);
    Mono<UserWithPasswordDTO> findByEmailWithPassword(String email);
}

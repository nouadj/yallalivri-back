package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.dto.UserWithPasswordDTO;
import dz.nadjtech.yallalivri.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Flux<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserDTO>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<UserDTO>> createUser(@RequestBody UserWithPasswordDTO userDTO) {
        return userService.createUser(userDTO)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserDTO>> updateUser(@PathVariable Long id, @RequestBody UserWithPasswordDTO userDTO) {
        return userService.updateUser(id, userDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public Mono<ResponseEntity<Object>> patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return userService.patchUser(id, updates)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/password")
    public Mono<Void> patchUserPassword(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return userService.patchUserPassword(id, updates);
    }



    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/location")
    public Mono<ResponseEntity<UserDTO>> updateUserLocation(
            @PathVariable Long id,
            @RequestBody Map<String, Double> location) {

        if (!location.containsKey("latitude") || !location.containsKey("longitude")) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return userService.updateUserLocation(id, location.get("latitude"), location.get("longitude"))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


}

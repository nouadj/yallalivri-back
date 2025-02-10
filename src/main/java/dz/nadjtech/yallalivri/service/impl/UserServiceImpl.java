package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.dto.UserDTOWithPassword;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.mapper.UserMapper;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<UserDTO> createUser(UserDTOWithPassword userDTO) {
        User user = userMapper.toEntity(userDTO);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        return userRepository.save(user)
                .map(userMapper::toDTO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<UserDTO> updateUser(Long id, UserDTOWithPassword userDTO) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")))
                .flatMap(existing -> {
                    existing.setName(userDTO.getName());
                    existing.setEmail(userDTO.getEmail());
                    existing.setRole(userDTO.getRole());
                    existing.setPhone(userDTO.getPhone());
                    if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                        existing.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                    }
                    return userRepository.save(existing);
                })
                .map(userMapper::toDTO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Flux<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .map(userMapper::toDTO);
    }

    @Override
    public Mono<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")))
                .map(userMapper::toDTO);
    }

    @Override
    public Mono<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found by email")))
                .map(userMapper::toDTO);
    }

    @Override
    public Mono<UserDTOWithPassword> findByEmailWithPassword(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found by email")))
                .map(userMapper::toWithPasswordDTO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<Void> deleteUser(Long id) {
        return userRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // NOTIFICATION TOKEN
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<UserDTO> updateNotificationToken(Long userId, String token) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")))
                .flatMap(user -> {
                    user.setNotificationToken(token);
                    return userRepository.save(user);
                })
                .map(userMapper::toDTO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PATCH (PROFILE UPDATE)
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<Object> patchUser(Long id, Map<String, Object> updates) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")))
                .flatMap(user -> {
                    updates.forEach((key, value) -> {
                        switch (key) {
                            case "email" -> user.setEmail((String) value);
                            case "phone" -> user.setPhone((String) value);
                            case "name" -> user.setName((String) value);
                            case "address" -> user.setAddress((String) value);
                            // etc. => on peut gérer le storeType, etc.
                        }
                    });
                    return userRepository.save(user);
                })
                .map(userMapper::toDTO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PATCH PASSWORD (USER)
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<Void> patchUserPassword(Long id, Map<String, Object> updates) {
        if (!updates.containsKey("oldPassword") || !updates.containsKey("newPassword")) {
            return Mono.error(new IllegalArgumentException("oldPassword & newPassword required"));
        }

        String oldPassword = (String) updates.get("oldPassword");
        String newPassword = (String) updates.get("newPassword");

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                        return Mono.error(new SecurityException("Old password mismatch"));
                    }
                    user.setPassword(passwordEncoder.encode(newPassword));
                    return userRepository.save(user).then();
                });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PATCH PASSWORD (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<Void> patchUserPasswordAdmin(Long id, Map<String, Object> updates) {
        if (!updates.containsKey("newPassword")) {
            return Mono.error(new IllegalArgumentException("newPassword is required"));
        }

        String newPassword = (String) updates.get("newPassword");
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    return userRepository.save(user).then();
                });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // LOCATION
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<UserDTO> updateUserLocation(Long id, Double latitude, Double longitude) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")))
                .flatMap(user -> {
                    user.setLatitude(latitude);
                    user.setLongitude(longitude);
                    return userRepository.save(user);
                })
                .map(userMapper::toDTO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Flux<UserDTO> searchUsers(Map<String, String> filters) {
        return userRepository.findAll()
                .filter(u -> matchesFilter(u, filters))
                .map(userMapper::toDTO);
    }

    private boolean matchesFilter(User user, Map<String, String> filters) {
        // ex: name, email, role, phone, address
        String nameFilter  = filters.getOrDefault("name", "");
        String emailFilter = filters.getOrDefault("email", "");
        String roleFilter  = filters.getOrDefault("role", "");
        String phoneFilter = filters.getOrDefault("phone", "");
        String addrFilter  = filters.getOrDefault("address", "");

        return (nameFilter.isEmpty()  || user.getName().toLowerCase().contains(nameFilter.toLowerCase()))
                && (emailFilter.isEmpty() || user.getEmail().toLowerCase().contains(emailFilter.toLowerCase()))
                && (roleFilter.isEmpty()  || user.getRole().name().equalsIgnoreCase(roleFilter))
                && (phoneFilter.isEmpty() || (user.getPhone() != null && user.getPhone().contains(phoneFilter)))
                && (addrFilter.isEmpty()  || (user.getAddress() != null && user.getAddress().toLowerCase().contains(addrFilter.toLowerCase())));
    }
}

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

    @Override
    public Mono<UserDTO> createUser(UserDTOWithPassword userDTO) {
        User user = userMapper.toEntity(userDTO);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        return userRepository.save(user)
                .map(userMapper::toDTO);
    }

    @Override
    public Mono<UserDTO> updateUser(Long id, UserDTOWithPassword userDTO) {
        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    existingUser.setName(userDTO.getName());
                    existingUser.setEmail(userDTO.getEmail());
                    existingUser.setRole(userDTO.getRole());
                    existingUser.setPhone(userDTO.getPhone());

                    if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
                    }

                    return userRepository.save(existingUser);
                })
                .map(userMapper::toDTO);
    }


    @Override
    public Flux<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .map(userMapper::toDTO);
    }

    @Override
    public Mono<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO);
    }


    @Override
    public Mono<Void> deleteUser(Long id) {
        return userRepository.deleteById(id);
    }

    @Override
    public Mono<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::toDTO);
    }

    @Override
    public Mono<UserDTOWithPassword> findByEmailWithPassword(String email) {
        return userRepository.findByEmail(email).map(userMapper::toWithPasswordDTO);
    }

    @Override
    public Mono<UserDTO> updateNotificationToken(Long userId, String token) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setNotificationToken(token);
                    return userRepository.save(user);
                })
                .map(userMapper::toDTO);
    }

    @Override
    public Mono<Object> patchUser(Long id, Map<String, Object> updates) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    updates.forEach((key, value) -> {
                        switch (key) {
                            case "email": user.setEmail((String) value); break;
                            case "phone": user.setPhone((String) value); break;
                        }
                    });
                    return userRepository.save(user);
                })
                .map(userMapper::toDTO); // 🔥 Vérifie que userMapper.toDTO() fonctionne bien
    }

    @Override
    public Mono<Void> patchUserPassword(Long id, Map<String, Object> updates) {
        if (!updates.containsKey("oldPassword") || !updates.containsKey("newPassword")) {
            return Mono.error(new IllegalArgumentException("❌ 'oldPassword' et 'newPassword' sont requis !"));
        }

        String oldPassword = (String) updates.get("oldPassword");
        String newPassword = (String) updates.get("newPassword");

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("❌ Utilisateur non trouvé !")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                        return Mono.error(new SecurityException("❌ Ancien mot de passe incorrect !"));
                    }

                    user.setPassword(passwordEncoder.encode(newPassword));
                    return userRepository.save(user).then();
                });
    }
    @Override
    public Mono<UserDTO> updateUserLocation(Long id, Double latitude, Double longitude) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setLatitude(latitude);
                    user.setLongitude(longitude);
                    return userRepository.save(user);
                })
                .map(userMapper::toDTO);
    }



}

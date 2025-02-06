package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.AuthRequest;
import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.dto.UserWithPasswordDTO;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.mapper.UserMapper;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<UserDTO> createUser(UserWithPasswordDTO userDTO) {
        User user = userMapper.toEntity(userDTO);

        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        return userRepository.save(user)
                .map(userMapper::toDTO);
    }

    @Override
    public Mono<UserDTO> updateUser(Long id, UserWithPasswordDTO userDTO) {
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
    public Mono<UserWithPasswordDTO> findByEmailWithPassword(String email) {
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
}

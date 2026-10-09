package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.*;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.enums.StoreType;
import dz.nadjtech.yallalivri.enums.UserRole;
import dz.nadjtech.yallalivri.mapper.UserMapper;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.security.CurrentUser;
import dz.nadjtech.yallalivri.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final dz.nadjtech.yallalivri.repository.OrderRepository orderRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, dz.nadjtech.yallalivri.repository.OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserDTO> getAllUsers(CurrentUser currentUser) {
        assertAdmin(currentUser);
        return userRepository.findAll().stream().map(userMapper::toDTO).toList();
    }

    @Override
    public UserDTO getUserById(Long id, CurrentUser currentUser) {
        assertAdminOrSelf(id, currentUser);
        return userMapper.toDTO(getUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserDTO createUser(UserDTOWithPassword userDTO, CurrentUser currentUser) {
        assertAdmin(currentUser);

        String email = normalizeEmail(userDTO.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userMapper.toEntity(userDTO);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        validateUser(user);
        return userMapper.toDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserDTOWithPassword userDTO, CurrentUser currentUser) {
        assertAdmin(currentUser);
        User existing = getUserOrThrow(id);

        String email = normalizeEmail(userDTO.getEmail());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("Email already exists");
        }

        existing.setName(userDTO.getName());
        existing.setEmail(email);
        assertRoleChangeAllowed(existing, userDTO.getRole());
        existing.setRole(userDTO.getRole());
        existing.setPhone(userDTO.getPhone());
        existing.setAddress(userDTO.getAddress());
        existing.setNotificationToken(userDTO.getNotificationToken());
        existing.setLatitude(userDTO.getLatitude());
        existing.setLongitude(userDTO.getLongitude());
        existing.setDateOfBirth(userDTO.getDateOfBirth());
        existing.setStoreType(userDTO.getStoreType());

        if (StringUtils.hasText(userDTO.getPassword())) {
            existing.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        validateUser(existing);
        return userMapper.toDTO(existing);
    }

    @Override
    @Transactional
    public UserDTO patchUser(Long id, Map<String, Object> updates, CurrentUser currentUser) {
        assertAdminOrSelf(id, currentUser);
        User user = getUserOrThrow(id);

        updates.forEach((key, value) -> {
            if (value == null) {
                switch (key) {
                    case "storeType" -> user.setStoreType(null);
                    case "dateOfBirth" -> user.setDateOfBirth(null);
                    case "latitude" -> user.setLatitude(null);
                    case "longitude" -> user.setLongitude(null);
                    default -> throw new IllegalArgumentException("Unsupported null field: " + key);
                }
                return;
            }

            switch (key) {
                case "email" -> {
                    String email = normalizeEmail(value.toString());
                    if (userRepository.existsByEmailAndIdNot(email, id)) {
                        throw new IllegalArgumentException("Email already exists");
                    }
                    user.setEmail(email);
                }
                case "phone" -> user.setPhone(value.toString());
                case "name" -> user.setName(value.toString());
                case "address" -> user.setAddress(value.toString());
                case "notificationToken" -> user.setNotificationToken(value.toString());
                case "latitude" -> user.setLatitude(asDouble(value));
                case "longitude" -> user.setLongitude(asDouble(value));
                case "dateOfBirth" -> user.setDateOfBirth(LocalDate.parse(value.toString()));
                case "storeType" -> user.setStoreType(StoreType.valueOf(value.toString().toUpperCase(Locale.ROOT)));
                case "role" -> {
                    assertAdmin(currentUser);
                    UserRole newRole = UserRole.valueOf(value.toString().toUpperCase(Locale.ROOT));
                    assertRoleChangeAllowed(user, newRole);
                    user.setRole(newRole);
                }
                default -> throw new IllegalArgumentException("Unsupported field: " + key);
            }
        });

        validateUser(user);
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional
    public void patchUserPassword(Long id, PasswordUpdateRequest request, CurrentUser currentUser) {
        assertAdminOrSelf(id, currentUser);
        User user = getUserOrThrow(id);

        if (!currentUser.isAdmin() && !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AccessDeniedException("Old password mismatch");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Override
    @Transactional
    public void patchUserPasswordAdmin(Long id, AdminPasswordUpdateRequest request, CurrentUser currentUser) {
        assertAdmin(currentUser);
        User user = getUserOrThrow(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Override
    @Transactional
    public void deleteUser(Long id, CurrentUser currentUser) {
        assertAdminOrSelf(id, currentUser);
        User user = getUserOrThrow(id);
        if (orderRepository.existsByStore_IdOrCourier_Id(id, id)) {
            throw new IllegalStateException("Cet utilisateur possède des commandes : conservez son compte pour préserver l’historique.");
        }
        userRepository.delete(user);
    }

    @Override
    public UserDTO findByEmail(String email, CurrentUser currentUser) {
        assertAdmin(currentUser);
        return userRepository.findByEmail(normalizeEmail(email))
                .map(userMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("User not found by email"));
    }

    @Override
    @Transactional
    public UserDTO updateNotificationToken(Long id, NotificationTokenRequest request, CurrentUser currentUser) {
        assertAdminOrSelf(id, currentUser);
        User user = getUserOrThrow(id);
        user.setNotificationToken(request.getNotificationToken());
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateUserLocation(Long id, LocationUpdateRequest request, CurrentUser currentUser) {
        assertAdminOrSelf(id, currentUser);
        User user = getUserOrThrow(id);
        user.setLatitude(request.getLatitude());
        user.setLongitude(request.getLongitude());
        return userMapper.toDTO(user);
    }

    @Override
    public List<UserDTO> searchUsers(Map<String, String> filters, CurrentUser currentUser) {
        assertAdmin(currentUser);

        return userRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String name = filters.get("name");
            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase(Locale.ROOT) + "%"));
            }

            String email = filters.get("email");
            if (StringUtils.hasText(email)) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase(Locale.ROOT) + "%"));
            }

            String role = filters.get("role");
            if (StringUtils.hasText(role)) {
                predicates.add(cb.equal(root.get("role"), UserRole.valueOf(role.toUpperCase(Locale.ROOT))));
            }

            String phone = filters.get("phone");
            if (StringUtils.hasText(phone)) {
                predicates.add(cb.like(root.get("phone"), "%" + phone + "%"));
            }

            String address = filters.get("address");
            if (StringUtils.hasText(address)) {
                predicates.add(cb.like(cb.lower(root.get("address")), "%" + address.toLowerCase(Locale.ROOT) + "%"));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().map(userMapper::toDTO).toList();
    }

    private void assertRoleChangeAllowed(User user, UserRole role) {
        if (user.getRole() != role && orderRepository.existsByStore_IdOrCourier_Id(user.getId(), user.getId()))
            throw new IllegalStateException("Impossible de changer le rôle d’un utilisateur lié à des commandes.");
    }

    private void validateUser(User user) {
        if (user.getName() == null || user.getName().isBlank() || user.getName().length() > 120)
            throw new IllegalArgumentException("Name is required (max 120 characters)");
        if (user.getEmail() == null || user.getEmail().length() > 180 || !user.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Invalid email");
        if (user.getLatitude() != null && (!Double.isFinite(user.getLatitude()) || Math.abs(user.getLatitude()) > 90))
            throw new IllegalArgumentException("Invalid latitude");
        if (user.getLongitude() != null && (!Double.isFinite(user.getLongitude()) || Math.abs(user.getLongitude()) > 180))
            throw new IllegalArgumentException("Invalid longitude");
        if ((user.getLatitude() == null) != (user.getLongitude() == null))
            throw new IllegalArgumentException("Provide both latitude and longitude");
        if (user.getPhone() != null && user.getPhone().length() > 30) throw new IllegalArgumentException("Phone too long");
        if (user.getDateOfBirth() != null && user.getDateOfBirth().isAfter(LocalDate.now())) throw new IllegalArgumentException("Birth date must be in the past");
        if (user.getRole() != UserRole.STORE) user.setStoreType(null);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private void assertAdmin(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    private void assertAdminOrSelf(Long userId, CurrentUser currentUser) {
        if (currentUser == null || (!currentUser.isAdmin() && !userId.equals(currentUser.id()))) {
            throw new AccessDeniedException("Unauthorized");
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.valueOf(value.toString());
    }
}

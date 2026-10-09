package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.*;
import dz.nadjtech.yallalivri.security.CurrentUser;
import dz.nadjtech.yallalivri.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getUsers(
            @RequestParam(value = "email", required = false) String email,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        if (email != null && !email.isBlank()) {
            return ResponseEntity.ok(List.of(userService.findByEmail(email, currentUser)));
        }
        return ResponseEntity.ok(userService.getAllUsers(currentUser));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(
            @RequestParam Map<String, String> filters,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(userService.searchUsers(filters, currentUser));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(userService.getUserById(userId, currentUser));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody UserDTOWithPassword userDTO,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(userService.createUser(userDTO, currentUser));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserDTOWithPassword userDTO,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, userDTO, currentUser));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserDTO> patchUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(userService.patchUser(userId, updates, currentUser));
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<String> patchUserPassword(
            @PathVariable Long userId,
            @Valid @RequestBody PasswordUpdateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        userService.patchUserPassword(userId, request, currentUser);
        return ResponseEntity.ok("Mot de passe mis à jour avec succès.");
    }

    @PatchMapping("/{userId}/password-admin")
    public ResponseEntity<String> patchUserPasswordAdmin(
            @PathVariable Long userId,
            @Valid @RequestBody AdminPasswordUpdateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        userService.patchUserPasswordAdmin(userId, request, currentUser);
        return ResponseEntity.ok("Mot de passe mis à jour avec succès.");
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        userService.deleteUser(userId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/location")
    public ResponseEntity<UserDTO> updateUserLocation(
            @PathVariable Long userId,
            @Valid @RequestBody LocationUpdateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(userService.updateUserLocation(userId, request, currentUser));
    }
}

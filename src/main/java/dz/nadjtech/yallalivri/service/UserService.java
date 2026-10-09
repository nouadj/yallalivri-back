package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.*;
import dz.nadjtech.yallalivri.security.CurrentUser;

import java.util.List;
import java.util.Map;

public interface UserService {
    List<UserDTO> getAllUsers(CurrentUser currentUser);
    UserDTO getUserById(Long id, CurrentUser currentUser);
    UserDTO createUser(UserDTOWithPassword userDTO, CurrentUser currentUser);
    UserDTO updateUser(Long id, UserDTOWithPassword userDTO, CurrentUser currentUser);
    UserDTO patchUser(Long id, Map<String, Object> updates, CurrentUser currentUser);
    void patchUserPassword(Long id, PasswordUpdateRequest request, CurrentUser currentUser);
    void patchUserPasswordAdmin(Long id, AdminPasswordUpdateRequest request, CurrentUser currentUser);
    void deleteUser(Long id, CurrentUser currentUser);
    UserDTO findByEmail(String email, CurrentUser currentUser);
    UserDTO updateNotificationToken(Long id, NotificationTokenRequest request, CurrentUser currentUser);
    UserDTO updateUserLocation(Long id, LocationUpdateRequest request, CurrentUser currentUser);
    List<UserDTO> searchUsers(Map<String, String> filters, CurrentUser currentUser);
}

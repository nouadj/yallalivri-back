package dz.nadjtech.yallalivri.mapper;

import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.dto.UserDTOWithPassword;
import dz.nadjtech.yallalivri.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) return null;

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .phone(user.getPhone())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .address(user.getAddress())
                .notificationToken(user.getNotificationToken())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .dateOfBirth(user.getDateOfBirth())
                .storeType(user.getStoreType())
                .build();
    }

    public UserDTOWithPassword toWithPasswordDTO(User user) {
        if (user == null) return null;

        return UserDTOWithPassword.builder()
                .id(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .phone(user.getPhone())
                .email(user.getEmail())
                .password(user.getPassword())
                .address(user.getAddress())
                .notificationToken(user.getNotificationToken())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .dateOfBirth(user.getDateOfBirth())
                .storeType(user.getStoreType())
                .build();
    }

    public User toEntity(UserDTOWithPassword dto) {
        if (dto == null) return null;

        return User.builder()
                .id(dto.getId())
                .name(dto.getName())
                .role(dto.getRole())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .address(dto.getAddress())
                .notificationToken(dto.getNotificationToken())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .dateOfBirth(dto.getDateOfBirth())
                .storeType(dto.getStoreType())
                .build();
    }
}

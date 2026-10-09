package dz.nadjtech.yallalivri.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTokenRequest {
    @Size(max=500)
    @Pattern(regexp="^(ExponentPushToken|ExpoPushToken)\\[[A-Za-z0-9_-]+\\]$", message="Invalid Expo push token")
    private String notificationToken;
}

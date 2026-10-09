package dz.nadjtech.yallalivri.bootstrap;

import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.enums.UserRole;
import dz.nadjtech.yallalivri.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.name:Admin}") String name
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("Bootstrap admin enabled but email/password are missing");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Bootstrap admin already exists: {}", normalizedEmail);
            return;
        }

        User admin = User.builder()
                .name(name)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .build();

        userRepository.save(admin);
        log.warn("Bootstrap admin created: {}. Disable app.bootstrap-admin.enabled after first startup.", normalizedEmail);
    }
}

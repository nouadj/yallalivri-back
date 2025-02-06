package dz.nadjtech.yallalivri.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/login").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .pathMatchers("/auth/me").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Personnalise l'entry point
                        .authenticationEntryPoint((exchange, ex) -> {
                            // Ici, on log l’exception pour voir le détail
                            ex.printStackTrace();  // ou un logger
                            return Mono.error(ex); // ou renvoie une réponse custom
                        })
                        .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter()))
                )
                .build();
    }


    /*
        @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/login").permitAll()
                        .pathMatchers("/api/**").permitAll()
                        .pathMatchers("/auth/me").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Personnalise l'entry point
                        .authenticationEntryPoint((exchange, ex) -> {
                            // Ici, on log l’exception pour voir le détail
                            ex.printStackTrace();  // ou un logger
                            return Mono.error(ex); // ou renvoie une réponse custom
                        })
                        .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter()))
                )
                .build();
    }
     */
    private ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) {
                return Flux.empty();
            }
            return Flux.just(new SimpleGrantedAuthority(role));
        });
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

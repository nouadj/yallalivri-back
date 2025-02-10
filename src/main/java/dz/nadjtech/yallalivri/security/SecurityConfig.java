package dz.nadjtech.yallalivri.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /* @Bean
      public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtAuthenticationFilter jwtFilter) {
          return http
                  .csrf(ServerHttpSecurity.CsrfSpec::disable)
                  .authorizeExchange(exchanges -> exchanges
                          .pathMatchers("/api/auth/login").permitAll()
                          .pathMatchers("/api/**").authenticated()
                          .anyExchange().authenticated()
                  )
                  .oauth2ResourceServer(oauth2 -> oauth2
                          .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter()))
                  )
                  .addFilterBefore(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION) // 🔥 Ajoute le filtre
                  .build();
      }*/
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtAuthenticationFilter jwtFilter) {
      return http
              .csrf(ServerHttpSecurity.CsrfSpec::disable)
              .authorizeExchange(exchanges -> exchanges
                      .pathMatchers("/api/auth/login").permitAll()
                      .pathMatchers("/**").permitAll()
              )
              .oauth2ResourceServer(oauth2 -> oauth2
                      .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter()))
              )
              .addFilterBefore(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION) // 🔥 Ajoute le filtre
              .build();
  }
/*
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:5173")); // 🔥 FRONTEND UNIQUEMENT
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        corsConfig.setAllowCredentials(true); // 🔥 IMPORTANT pour les JWT et cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
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

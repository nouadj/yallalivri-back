package dz.nadjtech.yallalivri.security;


import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        List<String> authHeaders = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);

        if (!authHeaders.isEmpty() && authHeaders.get(0).startsWith("Bearer ")) {
            String token = authHeaders.get(0).substring(7); // 🔥 Supprime "Bearer "
            try {
                Long userId = jwtUtil.extractUserId(token);
                exchange.getAttributes().put("userId", userId); // 🔥 Injecte userId dans l'attribut de la requête
            } catch (Exception e) {
                System.err.println("❌ JWT invalide ou expiré : " + e.getMessage());
            }
        }

        return chain.filter(exchange);
    }
}

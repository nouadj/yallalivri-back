package dz.nadjtech.yallalivri.repository;

import dz.nadjtech.yallalivri.dto.UserRole;
import dz.nadjtech.yallalivri.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByEmail(String email);

    Flux<User> findAllByRole(UserRole userRole);
}

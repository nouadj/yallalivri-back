package dz.nadjtech.yallalivri.repository;

import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    List<User> findAllByRole(UserRole role);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}

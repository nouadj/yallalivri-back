package dz.nadjtech.yallalivri.repository;

import dz.nadjtech.yallalivri.entity.Courier;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourierRepository extends ReactiveCrudRepository<Courier, Long> {
}

package dz.nadjtech.yallalivri.repository;

import dz.nadjtech.yallalivri.entity.Store;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends ReactiveCrudRepository<Store, Long> {
}

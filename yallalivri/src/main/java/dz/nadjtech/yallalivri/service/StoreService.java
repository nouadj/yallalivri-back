package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.StoreDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StoreService {
    Flux<StoreDTO> getAllStores();
    Mono<StoreDTO> getStoreById(Long id);
    Mono<StoreDTO> createStore(StoreDTO storeDTO);
    Mono<StoreDTO> updateStore(Long id, StoreDTO storeDTO);
    Mono<Void> deleteStore(Long id);
}

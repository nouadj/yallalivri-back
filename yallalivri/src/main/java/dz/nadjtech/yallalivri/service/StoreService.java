package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.StoreDTO;
import dz.nadjtech.yallalivri.dto.StoreDTOWithPassword;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StoreService {
    Flux<StoreDTO> getAllStores();
    Mono<StoreDTO> getStoreById(Long id);
    Mono<StoreDTO> createStore(StoreDTOWithPassword storeDTOWithPassword);
    Mono<StoreDTO> updateStore(Long id, StoreDTO storeDTO);
    Mono<Void> deleteStore(Long id);
}

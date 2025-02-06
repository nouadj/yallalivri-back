package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.StoreDTO;
import dz.nadjtech.yallalivri.entity.Store;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.mapper.StoreMapper;
import dz.nadjtech.yallalivri.repository.StoreRepository;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.service.StoreService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreMapper storeMapper;

    public StoreServiceImpl(StoreRepository storeRepository, StoreMapper storeMapper, UserRepository userRepository) {
        this.storeRepository = storeRepository;
        this.storeMapper = storeMapper;
        this.userRepository = userRepository;
    }

    @Override
    public Flux<StoreDTO> getAllStores() {
        return storeRepository.findAll()
                .map(storeMapper::toDTO);
    }

    @Override
    public Mono<StoreDTO> getStoreById(Long id) {
        return Mono.zip(
                        storeRepository.findById(id),
                        userRepository.findById(id)
                )
                .map(tuple -> {
                    Store store = tuple.getT1();
                    User user = tuple.getT2();
                    store.setName(user.getName());
                    return store;
                })
                .map(storeMapper::toDTO);
    }


    @Override
    public Mono<StoreDTO> createStore(StoreDTO storeDTO) {
        Store store = storeMapper.toEntity(storeDTO);
        return storeRepository.save(store)
                .map(storeMapper::toDTO);
    }

    @Override
    public Mono<StoreDTO> updateStore(Long id, StoreDTO storeDTO) {
        return storeRepository.findById(id)
                .flatMap(existingStore -> {
                    existingStore.setName(storeDTO.getName());
                    existingStore.setAddress(storeDTO.getAddress());
                    return storeRepository.save(existingStore);
                })
                .map(storeMapper::toDTO);
    }

    @Override
    public Mono<Void> deleteStore(Long id) {
        return storeRepository.deleteById(id);
    }
}

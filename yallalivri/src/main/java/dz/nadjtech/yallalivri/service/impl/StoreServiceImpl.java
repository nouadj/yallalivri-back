package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.StoreDTO;
import dz.nadjtech.yallalivri.dto.StoreDTOWithPassword;
import dz.nadjtech.yallalivri.dto.UserDTOWithPassword;
import dz.nadjtech.yallalivri.dto.UserRole;
import dz.nadjtech.yallalivri.entity.Store;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.mapper.StoreMapper;
import dz.nadjtech.yallalivri.repository.StoreRepository;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.service.StoreService;
import dz.nadjtech.yallalivri.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class StoreServiceImpl implements StoreService {


    private final StoreRepository storeRepository;
    private final UserService userService;
    private final StoreMapper storeMapper;
    private final UserRepository userRepository;
    private final TransactionalOperator transactionalOperator;

    public StoreServiceImpl(StoreRepository storeRepository, StoreMapper storeMapper, UserService userService, UserRepository userRepository, TransactionalOperator transactionalOperator) {
        this.storeRepository = storeRepository;
        this.storeMapper = storeMapper;
        this.userService = userService;
        this.userRepository = userRepository;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Flux<StoreDTO> getAllStores() {
        return storeRepository.findAll()
                .map(storeMapper::toDTO);
    }

    @Override
    public Mono<StoreDTO> getStoreById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")))
                .flatMap(user -> storeRepository.findById(id)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Magasin non trouvé")))
                        .map(store -> {
                            // 🔥 Mapper User + Store vers StoreDTO
                            StoreDTO storeDTO = new StoreDTO();
                            storeDTO.setId(user.getId());
                            storeDTO.setName(user.getName());
                            storeDTO.setPhone(user.getPhone());
                            storeDTO.setEmail(user.getEmail());
                            storeDTO.setAddress(store.getAddress());
                            storeDTO.setType(store.getType());

                            return storeDTO;
                        }));
    }



    @Override
    public Mono<StoreDTO> createStore(StoreDTOWithPassword storeDTOWithPassword) {
        // 🔥 Copier les attributs vers un UserDTOWithPassword
        UserDTOWithPassword userDTOWithPassword = new UserDTOWithPassword();
        BeanUtils.copyProperties(storeDTOWithPassword, userDTOWithPassword);
        userDTOWithPassword.setRole(UserRole.STORE);

        return userService.createUser(userDTOWithPassword)
                .flatMap(user -> {
                    // 🔥 Copier les attributs vers un StoreDTO
                    Store store = storeMapper.toEntity(storeDTOWithPassword);
                    store.setId(user.getId()); // 🔥 Associer le même ID
                    return storeRepository.save(store);
                })
                .map(storeMapper::toDTO).as(transactionalOperator::transactional);
    }


    @Override
    public Mono<StoreDTO> updateStore(Long id, StoreDTO storeDTO) {
        return storeRepository.findById(id)
                .flatMap(existingStore -> {
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

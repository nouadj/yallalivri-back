package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.StoreDTO;
import dz.nadjtech.yallalivri.dto.StoreDTOWithPassword;
import dz.nadjtech.yallalivri.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public Flux<StoreDTO> getAllStores() {
        return storeService.getAllStores();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<StoreDTO>> getStoreById(@PathVariable Long id) {
        return storeService.getStoreById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<StoreDTO>> createStore(@RequestBody StoreDTOWithPassword storeDTOWithPassword) {
        return storeService.createStore(storeDTOWithPassword)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<StoreDTO>> updateStore(@PathVariable Long id, @RequestBody StoreDTO storeDTO) {
        return storeService.updateStore(id, storeDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteStore(@PathVariable Long id) {
        return storeService.deleteStore(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}

package com.health.store;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.health.common.ApiResponse;
import com.health.common.BadRequestException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoreController {
    private final StoreRepository storeRepository;

    public StoreController(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @GetMapping("/api/store")
    public ApiResponse<StoreResponse> getStore() {
        Store store = storeRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BadRequestException("门店信息未配置"));
        return ApiResponse.ok(StoreResponse.from(store));
    }

    record StoreResponse(
            Long id,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String phone,
            LocalTime businessStart,
            LocalTime businessEnd,
            String announcement,
            String status
    ) {
        static StoreResponse from(Store store) {
            return new StoreResponse(
                    store.getId(),
                    store.getName(),
                    store.getAddress(),
                    store.getLatitude(),
                    store.getLongitude(),
                    store.getPhone(),
                    store.getBusinessStart(),
                    store.getBusinessEnd(),
                    store.getAnnouncement(),
                    store.getStatus()
            );
        }
    }
}

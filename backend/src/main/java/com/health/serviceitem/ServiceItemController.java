package com.health.serviceitem;

import java.math.BigDecimal;
import java.util.List;

import com.health.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceItemController {
    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping("/api/service-items")
    public ApiResponse<List<ServiceItemResponse>> listServiceItems() {
        return ApiResponse.ok(serviceItemService.listPublic().stream()
                .map(ServiceItemResponse::from)
                .toList());
    }

    @GetMapping("/api/service-items/{id}")
    public ApiResponse<ServiceItemResponse> getServiceItem(@PathVariable Long id) {
        return ApiResponse.ok(ServiceItemResponse.from(serviceItemService.getPublic(id)));
    }

    @GetMapping("/api/admin/service-items")
    public ApiResponse<List<ServiceItemResponse>> listAdminServiceItems() {
        return ApiResponse.ok(serviceItemService.listAdmin().stream().map(ServiceItemResponse::from).toList());
    }

    @PostMapping("/api/admin/service-items")
    public ApiResponse<ServiceItemResponse> createServiceItem(@Valid @RequestBody ServiceItemRequest request) {
        return ApiResponse.ok(ServiceItemResponse.from(serviceItemService.create(request)));
    }

    @PutMapping("/api/admin/service-items/{id}")
    public ApiResponse<ServiceItemResponse> updateServiceItem(
            @PathVariable Long id,
            @Valid @RequestBody ServiceItemRequest request
    ) {
        return ApiResponse.ok(ServiceItemResponse.from(serviceItemService.update(id, request)));
    }

    @PatchMapping("/api/admin/service-items/{id}/status")
    public ApiResponse<ServiceItemResponse> changeServiceItemStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request
    ) {
        return ApiResponse.ok(ServiceItemResponse.from(serviceItemService.changeStatus(id, request.status())));
    }

    record StatusRequest(@NotBlank(message = "项目状态不能为空") String status) {
    }

    record ServiceItemResponse(
            Long id,
            Long categoryId,
            String categoryName,
            String name,
            String imageUrl,
            int durationMinutes,
            BigDecimal originalPrice,
            BigDecimal salePrice,
            String suitablePeople,
            String highlights,
            String notice,
            boolean hot,
            boolean recommended,
            String status,
            int sortOrder
    ) {
        static ServiceItemResponse from(ServiceItem item) {
            ServiceCategory category = item.getCategory();
            return new ServiceItemResponse(
                    item.getId(),
                    category.getId(),
                    category.getName(),
                    item.getName(),
                    item.getImageUrl(),
                    item.getDurationMinutes(),
                    item.getOriginalPrice(),
                    item.getSalePrice(),
                    item.getSuitablePeople(),
                    item.getHighlights(),
                    item.getNotice(),
                    item.isHot(),
                    item.isRecommended(),
                    item.getStatus(),
                    item.getSortOrder()
            );
        }
    }
}

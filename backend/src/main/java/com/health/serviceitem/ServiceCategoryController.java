package com.health.serviceitem;

import java.util.List;

import com.health.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceCategoryController {
    private final ServiceCategoryService categoryService;

    public ServiceCategoryController(ServiceCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/admin/service-categories")
    public ApiResponse<List<ServiceCategoryResponse>> listCategories() {
        return ApiResponse.ok(categoryService.list().stream().map(ServiceCategoryResponse::from).toList());
    }

    @PostMapping("/api/admin/service-categories")
    public ApiResponse<ServiceCategoryResponse> createCategory(
            @Valid @RequestBody ServiceCategoryRequest request
    ) {
        return ApiResponse.ok(ServiceCategoryResponse.from(categoryService.create(request)));
    }

    @PutMapping("/api/admin/service-categories/{id}")
    public ApiResponse<ServiceCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ServiceCategoryRequest request
    ) {
        return ApiResponse.ok(ServiceCategoryResponse.from(categoryService.update(id, request)));
    }

    @PatchMapping("/api/admin/service-categories/{id}/status")
    public ApiResponse<ServiceCategoryResponse> changeCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody CategoryStatusRequest request
    ) {
        return ApiResponse.ok(ServiceCategoryResponse.from(categoryService.changeEnabled(id, request.enabled())));
    }

    record CategoryStatusRequest(@NotNull(message = "分类状态不能为空") Boolean enabled) {
    }

    record ServiceCategoryResponse(Long id, String name, int sortOrder, boolean enabled) {
        static ServiceCategoryResponse from(ServiceCategory category) {
            return new ServiceCategoryResponse(
                    category.getId(), category.getName(), category.getSortOrder(), category.isEnabled());
        }
    }
}

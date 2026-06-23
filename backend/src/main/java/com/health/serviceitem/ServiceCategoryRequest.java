package com.health.serviceitem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceCategoryRequest(
        @NotBlank(message = "分类名称不能为空") @Size(max = 50, message = "分类名称不能超过50个字") String name,
        int sortOrder,
        boolean enabled
) {
}

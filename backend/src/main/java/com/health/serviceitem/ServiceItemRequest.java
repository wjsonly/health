package com.health.serviceitem;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceItemRequest(
        @NotNull(message = "服务分类不能为空") Long categoryId,
        @NotBlank(message = "项目名称不能为空") @Size(max = 80, message = "项目名称不能超过80个字") String name,
        @Size(max = 500, message = "图片地址不能超过500个字") String imageUrl,
        @Min(value = 1, message = "服务时长必须大于0") int durationMinutes,
        @NotNull(message = "原价不能为空") @DecimalMin(value = "0", message = "原价不能小于0") BigDecimal originalPrice,
        @NotNull(message = "售价不能为空") @DecimalMin(value = "0", message = "售价不能小于0") BigDecimal salePrice,
        @Size(max = 500, message = "适合人群不能超过500个字") String suitablePeople,
        @Size(max = 1000, message = "服务亮点不能超过1000个字") String highlights,
        @Size(max = 500, message = "预约须知不能超过500个字") String notice,
        boolean hot,
        boolean recommended,
        @NotBlank(message = "项目状态不能为空") String status,
        int sortOrder
) {
}

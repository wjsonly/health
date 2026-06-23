package com.health.serviceitem;

import java.util.List;

import com.health.common.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceCategoryService {
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository itemRepository;

    public ServiceCategoryService(ServiceCategoryRepository categoryRepository, ServiceItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceCategory> list() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public ServiceCategory create(ServiceCategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("服务分类名称不能重复");
        }
        return categoryRepository.save(ServiceCategory.create(request));
    }

    @Transactional
    public ServiceCategory update(Long id, ServiceCategoryRequest request) {
        ServiceCategory category = get(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BadRequestException("服务分类名称不能重复");
        }
        validateDisable(id, request.enabled());
        category.updateFrom(request);
        return category;
    }

    @Transactional
    public ServiceCategory changeEnabled(Long id, boolean enabled) {
        ServiceCategory category = get(id);
        validateDisable(id, enabled);
        category.changeEnabled(enabled);
        return category;
    }

    private ServiceCategory get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("服务分类不存在"));
    }

    private void validateDisable(Long id, boolean enabled) {
        if (!enabled && itemRepository.existsByCategoryIdAndStatus(id, ServiceItemService.ACTIVE)) {
            throw new BadRequestException("分类下存在上架项目，不能停用");
        }
    }
}

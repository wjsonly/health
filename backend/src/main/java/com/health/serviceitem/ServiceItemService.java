package com.health.serviceitem;

import java.util.List;
import java.util.Set;

import com.health.common.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceItemService {
    static final String ACTIVE = "ACTIVE";
    static final String INACTIVE = "INACTIVE";
    private static final Set<String> SUPPORTED_STATUSES = Set.of(ACTIVE, INACTIVE);

    private final ServiceItemRepository itemRepository;
    private final ServiceCategoryRepository categoryRepository;

    public ServiceItemService(ServiceItemRepository itemRepository, ServiceCategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceItem> listPublic() {
        return itemRepository.findByStatusOrderBySortOrderAsc(ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ServiceItem> listAdmin() {
        return itemRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public ServiceItem getPublic(Long id) {
        return itemRepository.findById(id)
                .filter(item -> ACTIVE.equals(item.getStatus()))
                .orElseThrow(() -> new BadRequestException("服务项目不存在或已下架"));
    }

    @Transactional
    public ServiceItem create(ServiceItemRequest request) {
        validateRequest(request);
        return itemRepository.save(ServiceItem.create(request, getEnabledCategory(request.categoryId())));
    }

    @Transactional
    public ServiceItem update(Long id, ServiceItemRequest request) {
        validateRequest(request);
        ServiceItem item = getAdminItem(id);
        item.updateFrom(request, getEnabledCategory(request.categoryId()));
        return item;
    }

    @Transactional
    public ServiceItem changeStatus(Long id, String status) {
        validateStatus(status);
        ServiceItem item = getAdminItem(id);
        if (ACTIVE.equals(status) && !item.getCategory().isEnabled()) {
            throw new BadRequestException("服务分类不存在或已停用");
        }
        item.changeStatus(status);
        return item;
    }

    private ServiceItem getAdminItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("服务项目不存在"));
    }

    private ServiceCategory getEnabledCategory(Long id) {
        return categoryRepository.findById(id)
                .filter(ServiceCategory::isEnabled)
                .orElseThrow(() -> new BadRequestException("服务分类不存在或已停用"));
    }

    private void validateRequest(ServiceItemRequest request) {
        validateStatus(request.status());
        if (request.salePrice().compareTo(request.originalPrice()) > 0) {
            throw new BadRequestException("售价不能高于原价");
        }
    }

    private void validateStatus(String status) {
        if (!SUPPORTED_STATUSES.contains(status)) {
            throw new BadRequestException("项目状态不合法");
        }
    }
}

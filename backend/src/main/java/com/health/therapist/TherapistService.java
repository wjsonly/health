package com.health.therapist;

import java.util.List;

import com.health.common.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TherapistService {
    private static final String ACTIVE = "ACTIVE";

    private final TherapistRepository therapistRepository;

    public TherapistService(TherapistRepository therapistRepository) {
        this.therapistRepository = therapistRepository;
    }

    @Transactional(readOnly = true)
    public List<Therapist> listPublicTherapists(Long storeId) {
        return therapistRepository.findByStoreIdAndStatusAndVisibleOrderBySortOrderAsc(storeId, ACTIVE, true);
    }

    @Transactional(readOnly = true)
    public Therapist getPublicTherapist(Long id) {
        return therapistRepository.findByIdAndStatusAndVisible(id, ACTIVE, true)
                .orElseThrow(() -> new BadRequestException("技师不存在或已下架"));
    }

    @Transactional(readOnly = true)
    public List<Therapist> listAdminTherapists(Long storeId) {
        return therapistRepository.findByStoreIdOrderBySortOrderAsc(storeId);
    }

    @Transactional
    public Therapist create(TherapistCreateRequest request) {
        if (therapistRepository.existsByStoreIdAndEmployeeNo(request.storeId(), request.employeeNo())) {
            throw new BadRequestException("同一门店下技师工号不能重复");
        }
        return therapistRepository.save(Therapist.create(request));
    }

    @Transactional
    public Therapist update(Long id, TherapistUpdateRequest request) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("技师不存在"));
        therapist.updateFrom(request);
        return therapist;
    }

    @Transactional
    public Therapist changeStatus(Long id, String status) {
        if (!StringUtils.hasText(status)) {
            throw new BadRequestException("状态不能为空");
        }
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("技师不存在"));
        therapist.changeStatus(status);
        return therapist;
    }
}

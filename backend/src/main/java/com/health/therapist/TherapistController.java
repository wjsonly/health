package com.health.therapist;

import java.util.List;

import com.health.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TherapistController {
    private final TherapistService therapistService;

    public TherapistController(TherapistService therapistService) {
        this.therapistService = therapistService;
    }

    @GetMapping("/api/therapists")
    public ApiResponse<List<PublicTherapistResponse>> listTherapists(
            @RequestParam(defaultValue = "1") Long storeId
    ) {
        return ApiResponse.ok(therapistService.listPublicTherapists(storeId).stream()
                .map(PublicTherapistResponse::from)
                .toList());
    }

    @GetMapping("/api/therapists/{id}")
    public ApiResponse<PublicTherapistResponse> getTherapist(@PathVariable Long id) {
        return ApiResponse.ok(PublicTherapistResponse.from(therapistService.getPublicTherapist(id)));
    }

    @GetMapping("/api/admin/therapists")
    public ApiResponse<List<AdminTherapistResponse>> listAdminTherapists(
            @RequestParam(defaultValue = "1") Long storeId
    ) {
        return ApiResponse.ok(therapistService.listAdminTherapists(storeId).stream()
                .map(AdminTherapistResponse::from)
                .toList());
    }

    @PostMapping("/api/admin/therapists")
    public ApiResponse<AdminTherapistResponse> createTherapist(@Valid @RequestBody TherapistCreateRequest request) {
        return ApiResponse.ok(AdminTherapistResponse.from(therapistService.create(request)));
    }

    @PutMapping("/api/admin/therapists/{id}")
    public ApiResponse<AdminTherapistResponse> updateTherapist(
            @PathVariable Long id,
            @Valid @RequestBody TherapistUpdateRequest request
    ) {
        return ApiResponse.ok(AdminTherapistResponse.from(therapistService.update(id, request)));
    }

    @PatchMapping("/api/admin/therapists/{id}/status")
    public ApiResponse<AdminTherapistResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request
    ) {
        return ApiResponse.ok(AdminTherapistResponse.from(therapistService.changeStatus(id, request.status())));
    }

    record StatusChangeRequest(@NotBlank(message = "状态不能为空") String status) {
    }

    record PublicTherapistResponse(
            Long id,
            String name,
            String avatarUrl,
            String gender,
            int yearsOfExperience,
            String level,
            String introduction,
            String specialties,
            String serviceTags,
            boolean bookable
    ) {
        static PublicTherapistResponse from(Therapist therapist) {
            return new PublicTherapistResponse(
                    therapist.getId(),
                    therapist.getName(),
                    therapist.getAvatarUrl(),
                    therapist.getGender(),
                    therapist.getYearsOfExperience(),
                    therapist.getLevel(),
                    therapist.getIntroduction(),
                    therapist.getSpecialties(),
                    therapist.getServiceTags(),
                    therapist.isBookable()
            );
        }
    }

    record AdminTherapistResponse(
            Long id,
            Long storeId,
            String name,
            String avatarUrl,
            String gender,
            String phone,
            String employeeNo,
            int yearsOfExperience,
            String level,
            String status,
            String introduction,
            String specialties,
            String serviceTags,
            String certificateUrls,
            boolean bookable,
            boolean visible,
            int sortOrder
    ) {
        static AdminTherapistResponse from(Therapist therapist) {
            return new AdminTherapistResponse(
                    therapist.getId(),
                    therapist.getStoreId(),
                    therapist.getName(),
                    therapist.getAvatarUrl(),
                    therapist.getGender(),
                    therapist.getPhone(),
                    therapist.getEmployeeNo(),
                    therapist.getYearsOfExperience(),
                    therapist.getLevel(),
                    therapist.getStatus(),
                    therapist.getIntroduction(),
                    therapist.getSpecialties(),
                    therapist.getServiceTags(),
                    therapist.getCertificateUrls(),
                    therapist.isBookable(),
                    therapist.isVisible(),
                    therapist.getSortOrder()
            );
        }
    }
}

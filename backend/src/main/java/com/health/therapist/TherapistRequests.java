package com.health.therapist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record TherapistCreateRequest(
        @NotNull Long storeId,
        @NotBlank String name,
        String avatarUrl,
        @NotBlank String gender,
        @NotBlank String phone,
        @NotBlank String employeeNo,
        @Min(0) int yearsOfExperience,
        @NotBlank String level,
        @NotBlank String status,
        String introduction,
        String specialties,
        String serviceTags,
        String certificateUrls,
        boolean bookable,
        boolean visible,
        int sortOrder
) {}

record TherapistUpdateRequest(
        @NotBlank String name,
        String avatarUrl,
        @NotBlank String gender,
        @NotBlank String phone,
        @Min(0) int yearsOfExperience,
        @NotBlank String level,
        @NotBlank String status,
        String introduction,
        String specialties,
        String serviceTags,
        String certificateUrls,
        boolean bookable,
        boolean visible,
        int sortOrder
) {}

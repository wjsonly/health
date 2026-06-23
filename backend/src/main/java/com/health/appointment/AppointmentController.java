package com.health.appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.health.common.ApiResponse;
import com.health.common.BadRequestException;
import com.health.wechat.MiniProgramAuthInterceptor;
import com.health.wechat.MiniProgramPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/api/appointments")
    public ApiResponse<PublicAppointmentResponse> createAppointment(
            @RequestAttribute(value = MiniProgramAuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false)
            MiniProgramPrincipal principal,
            @Valid @RequestBody AppointmentCreateRequest request
    ) {
        AppointmentCreateRequest createRequest = principal == null ? request : request.withUserId(principal.id());
        return ApiResponse.ok(PublicAppointmentResponse.from(appointmentService.create(createRequest)));
    }

    @GetMapping("/api/appointments")
    public ApiResponse<List<PublicAppointmentResponse>> listAppointments(
            @RequestAttribute(MiniProgramAuthInterceptor.PRINCIPAL_ATTRIBUTE) MiniProgramPrincipal principal
    ) {
        return ApiResponse.ok(appointmentService.listByUser(principal.id()).stream()
                .map(PublicAppointmentResponse::from)
                .toList());
    }

    @GetMapping("/api/appointments/{id}")
    public ApiResponse<PublicAppointmentResponse> getAppointment(
            @PathVariable Long id,
            @RequestAttribute(MiniProgramAuthInterceptor.PRINCIPAL_ATTRIBUTE) MiniProgramPrincipal principal
    ) {
        return ApiResponse.ok(PublicAppointmentResponse.from(appointmentService.getByUser(id, principal.id())));
    }

    @PatchMapping("/api/appointments/{id}/cancel")
    public ApiResponse<PublicAppointmentResponse> cancelAppointment(
            @PathVariable Long id,
            @RequestAttribute(MiniProgramAuthInterceptor.PRINCIPAL_ATTRIBUTE) MiniProgramPrincipal principal
    ) {
        return ApiResponse.ok(PublicAppointmentResponse.from(appointmentService.cancelByUser(id, principal.id())));
    }

    @GetMapping("/api/admin/appointments")
    public ApiResponse<List<AdminAppointmentResponse>> listAdminAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(appointmentService.listAdmin(date).stream()
                .map(AdminAppointmentResponse::from)
                .toList());
    }

    @PatchMapping("/api/admin/appointments/{id}/arrive")
    public ApiResponse<AdminAppointmentResponse> arrive(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request
    ) {
        validateStatus(request, AppointmentService.STATUS_ARRIVED);
        return ApiResponse.ok(AdminAppointmentResponse.from(appointmentService.arrive(id, request.adminNote())));
    }

    @PatchMapping("/api/admin/appointments/{id}/start")
    public ApiResponse<AdminAppointmentResponse> start(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request
    ) {
        validateStatus(request, AppointmentService.STATUS_IN_SERVICE);
        return ApiResponse.ok(AdminAppointmentResponse.from(appointmentService.start(id, request.adminNote())));
    }

    @PatchMapping("/api/admin/appointments/{id}/complete")
    public ApiResponse<AdminAppointmentResponse> complete(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request
    ) {
        validateStatus(request, AppointmentService.STATUS_COMPLETED);
        return ApiResponse.ok(AdminAppointmentResponse.from(appointmentService.complete(id, request.adminNote())));
    }

    @PatchMapping("/api/admin/appointments/{id}/cancel")
    public ApiResponse<AdminAppointmentResponse> cancelByAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusRequest request
    ) {
        validateStatus(request, AppointmentService.STATUS_CANCELLED);
        return ApiResponse.ok(AdminAppointmentResponse.from(appointmentService.cancelByAdmin(id, request.adminNote())));
    }

    private void validateStatus(AppointmentStatusRequest request, String expectedStatus) {
        if (!expectedStatus.equals(request.status())) {
            throw new BadRequestException("状态参数与操作不一致");
        }
    }

    record PublicAppointmentResponse(
            Long id,
            Long userId,
            Long storeId,
            Long serviceItemId,
            Long therapistId,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal itemAmount,
            BigDecimal discountAmount,
            BigDecimal paidAmount,
            String paymentStatus,
            String status,
            String contactName,
            String contactPhone,
            String userNote
    ) {
        static PublicAppointmentResponse from(Appointment appointment) {
            return new PublicAppointmentResponse(
                    appointment.getId(),
                    appointment.getUserId(),
                    appointment.getStoreId(),
                    appointment.getServiceItemId(),
                    appointment.getTherapistId(),
                    appointment.getAppointmentDate(),
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    appointment.getItemAmount(),
                    appointment.getDiscountAmount(),
                    appointment.getPaidAmount(),
                    appointment.getPaymentStatus(),
                    appointment.getStatus(),
                    appointment.getContactName(),
                    appointment.getContactPhone(),
                    appointment.getUserNote()
            );
        }
    }

    record AdminAppointmentResponse(
            Long id,
            Long userId,
            Long storeId,
            Long serviceItemId,
            Long therapistId,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal itemAmount,
            BigDecimal discountAmount,
            BigDecimal paidAmount,
            String paymentStatus,
            String status,
            String contactName,
            String contactPhone,
            String userNote,
            String adminNote,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime arrivedAt,
            LocalDateTime serviceStartedAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt
    ) {
        static AdminAppointmentResponse from(Appointment appointment) {
            return new AdminAppointmentResponse(
                    appointment.getId(),
                    appointment.getUserId(),
                    appointment.getStoreId(),
                    appointment.getServiceItemId(),
                    appointment.getTherapistId(),
                    appointment.getAppointmentDate(),
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    appointment.getItemAmount(),
                    appointment.getDiscountAmount(),
                    appointment.getPaidAmount(),
                    appointment.getPaymentStatus(),
                    appointment.getStatus(),
                    appointment.getContactName(),
                    appointment.getContactPhone(),
                    appointment.getUserNote(),
                    appointment.getAdminNote(),
                    appointment.getCreatedAt(),
                    appointment.getPaidAt(),
                    appointment.getArrivedAt(),
                    appointment.getServiceStartedAt(),
                    appointment.getCompletedAt(),
                    appointment.getCancelledAt()
            );
        }
    }
}

package com.health.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.health.common.BadRequestException;
import com.health.serviceitem.ServiceItem;
import com.health.serviceitem.ServiceItemRepository;
import com.health.store.StoreRepository;
import com.health.therapist.Therapist;
import com.health.therapist.TherapistRepository;
import com.health.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    static final String STATUS_BOOKED = "BOOKED";
    static final String STATUS_ARRIVED = "ARRIVED";
    static final String STATUS_IN_SERVICE = "IN_SERVICE";
    static final String STATUS_COMPLETED = "COMPLETED";
    static final String STATUS_CANCELLED = "CANCELLED";
    static final String PAYMENT_UNPAID = "UNPAID";
    static final String STATUS_PAID = "PAID";

    private static final String ACTIVE = "ACTIVE";
    private static final Set<String> BLOCKING_STATUSES = Set.of(
            STATUS_BOOKED,
            STATUS_PAID,
            STATUS_ARRIVED,
            STATUS_IN_SERVICE,
            STATUS_COMPLETED
    );

    private final AppointmentRepository appointmentRepository;
    private final AppointmentAvailabilityService availabilityService;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final TherapistRepository therapistRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentAvailabilityService availabilityService,
            UserRepository userRepository,
            StoreRepository storeRepository,
            ServiceItemRepository serviceItemRepository,
            TherapistRepository therapistRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.therapistRepository = therapistRepository;
    }

    @Transactional
    public Appointment create(AppointmentCreateRequest request) {
        userRepository.findById(request.userId())
                .orElseThrow(() -> new BadRequestException("用户不存在"));
        storeRepository.findById(request.storeId())
                .orElseThrow(() -> new BadRequestException("门店信息未配置"));
        ServiceItem item = serviceItemRepository.findById(request.serviceItemId())
                .filter(serviceItem -> ACTIVE.equals(serviceItem.getStatus()))
                .orElseThrow(() -> new BadRequestException("服务项目不存在或已下架"));
        Therapist therapist = lockTherapist(request.therapistId());
        validateTherapist(request, therapist);

        LocalTime endTime = request.startTime().plusMinutes(item.getDurationMinutes());
        boolean startAvailable = availabilityService
                .availableSlots(request.therapistId(), request.serviceItemId(), request.appointmentDate())
                .stream()
                .anyMatch(slot -> slot.startTime().equals(request.startTime()));
        boolean overlap = appointmentRepository.existsBlockingOverlap(
                request.therapistId(),
                request.appointmentDate(),
                request.startTime(),
                endTime,
                BLOCKING_STATUSES
        );
        if (!startAvailable || overlap) {
            throw new BadRequestException("该时间段已被预约");
        }

        return appointmentRepository.save(Appointment.create(request, endTime, item.getSalePrice()));
    }

    @Transactional(readOnly = true)
    public List<Appointment> listByUser(Long userId) {
        return appointmentRepository.findByUserIdOrderByAppointmentDateDescStartTimeDesc(userId);
    }

    @Transactional(readOnly = true)
    public Appointment get(Long id) {
        return find(id);
    }

    @Transactional(readOnly = true)
    public Appointment getByUser(Long id, Long userId) {
        return appointmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BadRequestException("预约不存在"));
    }

    @Transactional(readOnly = true)
    public List<Appointment> listAdmin(LocalDate date) {
        return appointmentRepository.findByAppointmentDateOrderByStartTimeAsc(date);
    }

    @Transactional
    public Appointment cancelByUser(Long id, Long userId) {
        Appointment appointment = appointmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BadRequestException("预约不存在"));
        transition(appointment, STATUS_CANCELLED, null);
        return appointment;
    }

    @Transactional
    public Appointment arrive(Long id, String adminNote) {
        Appointment appointment = find(id);
        transition(appointment, STATUS_ARRIVED, adminNote);
        return appointment;
    }

    @Transactional
    public Appointment start(Long id, String adminNote) {
        Appointment appointment = find(id);
        transition(appointment, STATUS_IN_SERVICE, adminNote);
        return appointment;
    }

    @Transactional
    public Appointment complete(Long id, String adminNote) {
        Appointment appointment = find(id);
        transition(appointment, STATUS_COMPLETED, adminNote);
        return appointment;
    }

    @Transactional
    public Appointment cancelByAdmin(Long id, String adminNote) {
        Appointment appointment = find(id);
        transition(appointment, STATUS_CANCELLED, adminNote);
        return appointment;
    }

    private Therapist lockTherapist(Long therapistId) {
        if (therapistId == null) {
            throw new BadRequestException("请选择技师");
        }
        return therapistRepository.lockById(therapistId)
                .orElseThrow(() -> new BadRequestException("技师不存在或已下架"));
    }

    private void validateTherapist(AppointmentCreateRequest request, Therapist therapist) {
        if (!request.storeId().equals(therapist.getStoreId())
                || !ACTIVE.equals(therapist.getStatus())
                || !therapist.isVisible()
                || !therapist.isBookable()
                || therapistRepository.countServiceAssignments(request.therapistId(), request.serviceItemId()) <= 0) {
            throw new BadRequestException("技师不存在或已下架");
        }
    }

    private Appointment find(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("预约不存在"));
    }

    private void transition(Appointment appointment, String nextStatus, String adminNote) {
        if (!canTransition(appointment.getStatus(), nextStatus)) {
            throw new BadRequestException("当前订单状态不允许此操作");
        }
        appointment.transitionTo(nextStatus, adminNote);
    }

    private boolean canTransition(String currentStatus, String nextStatus) {
        return (STATUS_BOOKED.equals(currentStatus)
                && (STATUS_ARRIVED.equals(nextStatus) || STATUS_CANCELLED.equals(nextStatus)))
                || (STATUS_ARRIVED.equals(currentStatus)
                && (STATUS_IN_SERVICE.equals(nextStatus) || STATUS_CANCELLED.equals(nextStatus)))
                || (STATUS_IN_SERVICE.equals(currentStatus) && STATUS_COMPLETED.equals(nextStatus));
    }
}

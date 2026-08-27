package com.clinic.dental.service;

import com.clinic.dental.entity.Appointment;
import com.clinic.dental.entity.DoctorSchedule;
import com.clinic.dental.repository.AppointmentRepository;
import com.clinic.dental.repository.PatientRepository;
import com.clinic.dental.repository.DoctorRepository;
import com.clinic.dental.repository.DoctorScheduleRepository;
import com.clinic.dental.security.AuthorizationService;
import com.clinic.dental.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AppointmentService {

    public static final String STATUS_PENDING = "待就诊";
    public static final String STATUS_WAITING = "候诊中";
    public static final String STATUS_CALLING = "就诊中";
    public static final String STATUS_PASSED = "已过号";
    public static final String STATUS_COMPLETED = "已完成";
    public static final String STATUS_CANCELLED = "已取消";
    public static final String SLOT_CONFLICT_MESSAGE = "该时段已被抢占，请选择其他时段";

    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AuthorizationService authorizationService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorScheduleRepository scheduleRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              AuthorizationService authorizationService) {
        this.appointmentRepository = appointmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment createAppointment(Appointment appointment) {
        validateAppointment(appointment);
        DoctorSchedule schedule = getSchedule(appointment);
        reserveSlot(schedule);

        appointment.setId(null);
        appointment.setCode(null);
        appointment.setStatus(STATUS_PENDING);
        Appointment saved = appointmentRepository.saveAndFlush(appointment);
        saved.setCode("AP" + (20260000 + saved.getId()));
        return appointmentRepository.save(saved);
    }

    /**
     * 改期时先占用新时段，再释放旧时段，二者与预约更新处于同一事务。
     * 仅待就诊或已过号预约允许改期。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment updateAppointment(Long id, Appointment body) {
        Appointment appointment = loadAppointment(id);
        String status = appointment.getStatus();
        if (!STATUS_PENDING.equals(status) && !STATUS_PASSED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待就诊或已过号预约可以改期");
        }
        validateAppointment(body);

        boolean slotChanged = !body.getDoctorId().equals(appointment.getDoctorId())
                || !body.getDate().equals(appointment.getDate())
                || !body.getTime().equals(appointment.getTime());
        if (slotChanged) {
            DoctorSchedule oldSchedule = getSchedule(appointment);
            DoctorSchedule newSchedule = getSchedule(body);
            updateChangedSchedules(oldSchedule, newSchedule);
        }

        appointment.setPatientId(body.getPatientId());
        appointment.setDoctorId(body.getDoctorId());
        appointment.setType(body.getType());
        appointment.setDate(body.getDate());
        appointment.setTime(body.getTime());
        appointment.setVisitType(body.getVisitType());
        appointment.setNote(body.getNote());
        return appointmentRepository.save(appointment);
    }

    /**
     * 患者到店签到：待就诊或已过号 → 候诊中。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment checkIn(Long id) {
        Appointment appointment = loadAppointment(id);
        String status = appointment.getStatus();
        if (!STATUS_PENDING.equals(status) && !STATUS_PASSED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待就诊或已过号预约可以签到");
        }
        appointment.setStatus(STATUS_WAITING);
        return appointmentRepository.save(appointment);
    }

    /**
     * 医生叫号接诊：候诊中 → 就诊中。
     * DOCTOR 角色仅可叫号本人名下预约，非本人抛出 403。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment callPatient(Long id) {
        Appointment appointment = loadAppointment(id);
        CurrentUser user = authorizationService.currentUser();
        if (user.isDoctor() && (user.doctorId() == null || !user.doctorId().equals(appointment.getDoctorId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "医生仅可叫号本人名下预约");
        }
        if (!STATUS_WAITING.equals(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅候诊中预约可以叫号");
        }
        appointment.setStatus(STATUS_CALLING);
        return appointmentRepository.save(appointment);
    }

    /**
     * 标记过号：候诊中或就诊中 → 已过号。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment passNumber(Long id) {
        Appointment appointment = loadAppointment(id);
        String status = appointment.getStatus();
        if (!STATUS_WAITING.equals(status) && !STATUS_CALLING.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅候诊中或就诊中预约可以过号");
        }
        appointment.setStatus(STATUS_PASSED);
        return appointmentRepository.save(appointment);
    }

    /**
     * 完成就诊：仅就诊中 → 已完成。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment completeAppointment(Long id) {
        Appointment appointment = loadAppointment(id);
        if (!STATUS_CALLING.equals(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅就诊中预约可以完成就诊");
        }
        appointment.setStatus(STATUS_COMPLETED);
        return appointmentRepository.save(appointment);
    }

    /**
     * 退号并回滚号源：待就诊/候诊中/已过号允许退号并释放排班配额；
     * 就诊中或已完成禁止退号。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment cancelAppointment(Long id) {
        Appointment appointment = loadAppointment(id);
        String status = appointment.getStatus();
        if (STATUS_CANCELLED.equals(status)) {
            return appointment;
        }
        if (!STATUS_PENDING.equals(status) && !STATUS_WAITING.equals(status) && !STATUS_PASSED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "就诊中或已完成预约不能退号");
        }

        DoctorSchedule schedule = getSchedule(appointment);
        releaseSlot(schedule);
        appointment.setStatus(STATUS_CANCELLED);
        return appointmentRepository.save(appointment);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteAppointment(Long id) {
        Appointment appointment = loadAppointment(id);
        if (slotHeld(appointment.getStatus())) {
            releaseSlot(getSchedule(appointment));
        }
        appointmentRepository.delete(appointment);
    }

    private Appointment loadAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "预约不存在"));
    }

    /**
     * 是否仍占用号源：待就诊/候诊中/就诊中/已过号均持有号源，
     * 已完成与已取消的号源已释放或消耗，不再回滚。
     */
    private boolean slotHeld(String status) {
        return STATUS_PENDING.equals(status)
                || STATUS_WAITING.equals(status)
                || STATUS_CALLING.equals(status)
                || STATUS_PASSED.equals(status);
    }

    private DoctorSchedule getSchedule(Appointment appointment) {
        return scheduleRepository.findByDoctorIdAndWorkDateAndTimeSlot(
                        appointment.getDoctorId(), appointment.getDate(), appointment.getTime())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "未找到对应医生排班，请先查询可预约时段"));
    }

    private void updateChangedSchedules(DoctorSchedule oldSchedule, DoctorSchedule newSchedule) {
        if (oldSchedule.getId().compareTo(newSchedule.getId()) < 0) {
            releaseSlot(oldSchedule);
            reserveSlot(newSchedule);
        } else {
            reserveSlot(newSchedule);
            releaseSlot(oldSchedule);
        }
    }

    private void reserveSlot(DoctorSchedule schedule) {
        if (!DoctorSchedule.STATUS_AVAILABLE.equals(schedule.getStatus())
                || schedule.getRemainingQuota() == null
                || schedule.getRemainingQuota() <= 0) {
            throw slotConflict();
        }
        schedule.setRemainingQuota(schedule.getRemainingQuota() - 1);
        schedule.setStatus(schedule.getRemainingQuota() > 0
                ? DoctorSchedule.STATUS_AVAILABLE
                : DoctorSchedule.STATUS_BOOKED);
        scheduleRepository.saveAndFlush(schedule);
    }

    private void releaseSlot(DoctorSchedule schedule) {
        int maxQuota = schedule.getMaxQuota() == null ? 1 : schedule.getMaxQuota();
        int remainingQuota = schedule.getRemainingQuota() == null ? 0 : schedule.getRemainingQuota();
        schedule.setRemainingQuota(Math.min(maxQuota, remainingQuota + 1));
        schedule.setStatus(DoctorSchedule.STATUS_AVAILABLE);
        scheduleRepository.saveAndFlush(schedule);
    }

    private void validateAppointment(Appointment appointment) {
        if (appointment.getPatientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "患者不能为空");
        }
        if (!patientRepository.existsById(appointment.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "患者不存在");
        }
        if (appointment.getDoctorId() == null
                || appointment.getDate() == null
                || appointment.getTime() == null
                || appointment.getTime().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "医生、日期和就诊时段不能为空");
        }
        if (!doctorRepository.existsById(appointment.getDoctorId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "医生不存在");
        }
        if (!DoctorScheduleService.DEFAULT_TIME_SLOTS.contains(appointment.getTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "就诊时段必须为半小时时间片");
        }
    }

    private ResponseStatusException slotConflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, SLOT_CONFLICT_MESSAGE);
    }
}

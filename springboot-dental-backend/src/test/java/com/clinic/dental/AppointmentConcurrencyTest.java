package com.clinic.dental;

import com.clinic.dental.entity.Appointment;
import com.clinic.dental.entity.Doctor;
import com.clinic.dental.entity.DoctorSchedule;
import com.clinic.dental.entity.Patient;
import com.clinic.dental.repository.AppointmentRepository;
import com.clinic.dental.repository.DoctorRepository;
import com.clinic.dental.repository.DoctorScheduleRepository;
import com.clinic.dental.repository.PatientRepository;
import com.clinic.dental.service.AppointmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用真实 Spring 事务与独立测试数据库连接验证同一号源不会超卖。
 */
@SpringBootTest
@ActiveProfiles("test")
class AppointmentConcurrencyTest {

    private static final int THREAD_COUNT = 10;
    private static final String TIME_SLOT = "09:00";

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    private LocalDate workDate;
    private Long doctorId;
    private Long scheduleId;
    private final List<Long> patientIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        workDate = LocalDate.now().plusDays(30);

        // 1. 初始化并发测试医生
        Doctor doctor = new Doctor();
        doctor.setName("并发测试医生");
        doctor.setTitle("测试医师");
        doctor.setYears(1);
        doctor.setSpec("口腔检查");
        doctor.setWork(0);
        doctorId = doctorRepository.saveAndFlush(doctor).getId();

        // 2. 初始化排班号源（配额为 1）
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorId(doctorId);
        schedule.setWorkDate(workDate);
        schedule.setTimeSlot(TIME_SLOT);
        schedule.setMaxQuota(1);
        schedule.setRemainingQuota(1);
        schedule.setStatus(DoctorSchedule.STATUS_AVAILABLE);
        scheduleId = scheduleRepository.saveAndFlush(schedule).getId();

        // 3. 初始化 10 位测试患者，供 10 个并发抢号线程使用
        patientIds.clear();
        for (int i = 0; i < THREAD_COUNT; i++) {
            Patient patient = new Patient();
            patient.setName("测试患者" + i);
            patient.setGender("男");
            patient.setAge(20 + i);
            patient.setPhone("1380000000" + i);
            patient.setCreated(LocalDate.now());
            patientIds.add(patientRepository.saveAndFlush(patient).getId());
        }
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.findByDoctorId(doctorId).forEach(appointmentRepository::delete);
        appointmentRepository.flush();
        scheduleRepository.findById(scheduleId).ifPresent(scheduleRepository::delete);
        scheduleRepository.flush();
        doctorRepository.deleteById(doctorId);
        doctorRepository.flush();
        patientIds.forEach(patientRepository::deleteById);
        patientRepository.flush();
    }

    @Test
    void tenConcurrentRequestsOnlyOneCanBookTheSameSlot() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    try {
                        appointmentService.createAppointment(newAppointment(index));
                        successCount.incrementAndGet();
                    } catch (OptimisticLockingFailureException exception) {
                        conflictCount.incrementAndGet();
                    } catch (ResponseStatusException exception) {
                        if (exception.getStatusCode().value() == 409) {
                            conflictCount.incrementAndGet();
                        } else {
                            throw exception;
                        }
                    }
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        DoctorSchedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
        long appointments = appointmentRepository.findByDate(workDate).stream()
                .filter(appointment -> doctorId.equals(appointment.getDoctorId()))
                .filter(appointment -> TIME_SLOT.equals(appointment.getTime()))
                .filter(appointment -> !AppointmentService.STATUS_CANCELLED.equals(appointment.getStatus()))
                .count();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(THREAD_COUNT - 1);
        assertThat(appointments).isEqualTo(1);
        assertThat(schedule.getRemainingQuota()).isZero();
        assertThat(schedule.getStatus()).isEqualTo(DoctorSchedule.STATUS_BOOKED);
    }

    private Appointment newAppointment(int index) {
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientIds.get(index));
        appointment.setDoctorId(doctorId);
        appointment.setType("口腔检查");
        appointment.setDate(workDate);
        appointment.setTime(TIME_SLOT);
        appointment.setVisitType("初诊");
        appointment.setNote("并发抢号测试-" + index);
        return appointment;
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("并发测试线程被中断", exception);
        }
    }
}
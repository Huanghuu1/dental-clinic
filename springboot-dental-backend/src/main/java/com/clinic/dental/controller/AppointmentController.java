package com.clinic.dental.controller;

import com.clinic.dental.entity.Appointment;
import com.clinic.dental.repository.AppointmentRepository;
import com.clinic.dental.security.AuthorizationService;
import com.clinic.dental.security.CurrentUser;
import com.clinic.dental.service.AppointmentService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository repo;
    private final AppointmentService appointmentService;
    private final AuthorizationService authorizationService;

    public AppointmentController(AppointmentRepository repo,
                                 AppointmentService appointmentService,
                                 AuthorizationService authorizationService) {
        this.repo = repo;
        this.appointmentService = appointmentService;
        this.authorizationService = authorizationService;
    }

    /** 列表，医生只能获得本人名下预约，其他角色可按条件查看全院预约。 */
    @GetMapping
    public List<Appointment> list(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) Long patientId,
                                  @RequestParam(required = false) Long doctorId,
                                  @RequestParam(required = false) String date) {
        CurrentUser user = authorizationService.currentUser();
        Stream<Appointment> appointments;
        if (user.isDoctor()) {
            appointments = repo.findByDoctorId(authorizationService.requireDoctorId()).stream();
        } else {
            appointments = repo.findAll(Sort.by(Sort.Direction.DESC, "date")).stream();
            if (doctorId != null) {
                appointments = appointments.filter(a -> doctorId.equals(a.getDoctorId()));
            }
        }
        if (status != null && !status.isBlank()) appointments = appointments.filter(a -> status.equals(a.getStatus()));
        if (patientId != null) appointments = appointments.filter(a -> patientId.equals(a.getPatientId()));
        if (date != null && !date.isBlank()) appointments = appointments.filter(a -> date.equals(a.getDate().toString()));
        return appointments.toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> get(@PathVariable Long id) {
        return repo.findById(id).map(appointment -> {
            if (!canAccess(appointment)) {
                throw authorizationService.forbidden();
            }
            return ResponseEntity.ok(appointment);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Appointment create(@RequestBody Appointment appointment) {
        authorizationService.requireAppointmentOperations();
        enforceDoctorAssignment(appointment);
        return appointmentService.createAppointment(appointment);
    }

    @PutMapping("/{id}")
    public Appointment update(@PathVariable Long id, @RequestBody Appointment body) {
        Appointment existing = ownedAppointment(id);
        enforceDoctorAssignment(body);
        if (authorizationService.currentUser().isDoctor()) {
            body.setDoctorId(existing.getDoctorId());
        }
        return appointmentService.updateAppointment(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        verifyOwnership(id);
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    /** 完成就诊。 */
    @PostMapping("/{id}/complete")
    public Appointment complete(@PathVariable Long id) {
        verifyOwnership(id);
        return appointmentService.completeAppointment(id);
    }

    /** 取消预约并恢复号源。 */
    @PostMapping("/{id}/cancel")
    public Appointment cancel(@PathVariable Long id) {
        verifyOwnership(id);
        return appointmentService.cancelAppointment(id);
    }

    @GetMapping("/count/today")
    public long todayCount() {
        CurrentUser user = authorizationService.currentUser();
        if (user.isDoctor()) {
            return repo.findByDoctorId(authorizationService.requireDoctorId()).stream()
                    .filter(a -> LocalDate.now().equals(a.getDate()))
                    .count();
        }
        return repo.findByDate(LocalDate.now()).size();
    }

    private void enforceDoctorAssignment(Appointment appointment) {
        CurrentUser user = authorizationService.currentUser();
        if (user.isDoctor()) {
            appointment.setDoctorId(authorizationService.requireDoctorId());
        }
    }

    private void verifyOwnership(Long appointmentId) {
        ownedAppointment(appointmentId);
    }

    private Appointment ownedAppointment(Long appointmentId) {
        Appointment appointment = repo.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "预约不存在"));
        if (!canAccess(appointment)) {
            throw authorizationService.forbidden();
        }
        return appointment;
    }

    private boolean canAccess(Appointment appointment) {
        CurrentUser user = authorizationService.currentUser();
        return !user.isDoctor() || authorizationService.requireDoctorId().equals(appointment.getDoctorId());
    }
}

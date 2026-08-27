package com.clinic.dental.controller;

import com.clinic.dental.dto.DoctorScheduleSlotResponse;
import com.clinic.dental.entity.Doctor;
import com.clinic.dental.repository.DoctorRepository;
import com.clinic.dental.security.AuthorizationService;
import com.clinic.dental.security.CurrentUser;
import com.clinic.dental.service.DoctorScheduleService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorRepository repo;
    private final DoctorScheduleService scheduleService;
    private final AuthorizationService authorizationService;

    public DoctorController(DoctorRepository repo,
                            DoctorScheduleService scheduleService,
                            AuthorizationService authorizationService) {
        this.repo = repo;
        this.scheduleService = scheduleService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<Doctor> list() {
        CurrentUser user = authorizationService.currentUser();
        if (user.isDoctor()) {
            return repo.findById(authorizationService.requireDoctorId()).stream().toList();
        }
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> get(@PathVariable Long id) {
        authorizationService.requireDoctorOwns(id);
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /** 查询医生某日的 12 个半小时时间片，不完整时自动补全。 */
    @GetMapping("/{doctorId}/slots")
    public List<DoctorScheduleSlotResponse> slots(@PathVariable Long doctorId,
                                                   @RequestParam LocalDate date) {
        authorizationService.requireDoctorOwns(doctorId);
        return scheduleService.getOrCreateDailySlots(doctorId, date).stream()
                .map(DoctorScheduleSlotResponse::from)
                .toList();
    }

    @PostMapping
    public Doctor create(@RequestBody Doctor doctor) {
        authorizationService.requireAdmin();
        return repo.save(doctor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Doctor> update(@PathVariable Long id, @RequestBody Doctor body) {
        authorizationService.requireAdmin();
        return repo.findById(id).map(doctor -> {
            Long keepId = doctor.getId();
            BeanUtils.copyProperties(body, doctor, "id");
            doctor.setId(keepId);
            return ResponseEntity.ok(repo.save(doctor));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorizationService.requireAdmin();
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

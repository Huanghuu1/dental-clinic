package com.clinic.dental.controller;

import com.clinic.dental.entity.Patient;
import com.clinic.dental.repository.PatientRepository;
import com.clinic.dental.security.AuthorizationService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository repo;
    private final AuthorizationService authorizationService;

    public PatientController(PatientRepository repo, AuthorizationService authorizationService) {
        this.repo = repo;
        this.authorizationService = authorizationService;
    }

    /** 列表，支持按姓名/电话关键字过滤。医生可为接诊查阅患者档案。 */
    @GetMapping
    public List<Patient> list(@RequestParam(required = false) String keyword) {
        authorizationService.currentUser();
        if (keyword != null && !keyword.isBlank()) {
            return repo.findByNameContaining(keyword.trim());
        }
        return repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> get(@PathVariable Long id) {
        authorizationService.currentUser();
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Patient create(@RequestBody Patient patient) {
        authorizationService.requireAdminOrStaff();
        if (patient.getCreated() == null) patient.setCreated(LocalDate.now());
        return repo.save(patient);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> update(@PathVariable Long id, @RequestBody Patient body) {
        authorizationService.requireAdminOrStaff();
        return repo.findById(id).map(patient -> {
            Long keepId = patient.getId();
            BeanUtils.copyProperties(body, patient, "id", "created");
            patient.setId(keepId);
            if (patient.getCreated() == null) patient.setCreated(LocalDate.now());
            return ResponseEntity.ok(repo.save(patient));
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

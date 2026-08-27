package com.clinic.dental.controller;

import com.clinic.dental.entity.TreatmentRecord;
import com.clinic.dental.repository.PatientRepository;
import com.clinic.dental.repository.TreatmentRecordRepository;
import com.clinic.dental.security.AuthorizationService;
import com.clinic.dental.security.CurrentUser;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/records")
public class TreatmentRecordController {

    private final TreatmentRecordRepository repo;
    private final PatientRepository patientRepository;
    private final AuthorizationService authorizationService;

    public TreatmentRecordController(TreatmentRecordRepository repo,
                                     PatientRepository patientRepository,
                                     AuthorizationService authorizationService) {
        this.repo = repo;
        this.patientRepository = patientRepository;
        this.authorizationService = authorizationService;
    }

    /** 医生只能查询本人编写的诊疗记录，前台仅可查看。 */
    @GetMapping
    public List<TreatmentRecord> list(@RequestParam(required = false) Long patientId) {
        CurrentUser user = authorizationService.currentUser();
        Stream<TreatmentRecord> records;
        if (user.isDoctor()) {
            records = repo.findByDoctorId(authorizationService.requireDoctorId()).stream();
        } else {
            records = repo.findAll(Sort.by(Sort.Direction.DESC, "date")).stream();
        }
        if (patientId != null) {
            records = records.filter(record -> patientId.equals(record.getPatientId()));
        }
        return records.toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreatmentRecord> get(@PathVariable Long id) {
        return repo.findById(id).map(record -> {
            if (!canAccess(record)) {
                throw authorizationService.forbidden();
            }
            return ResponseEntity.ok(record);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TreatmentRecord create(@RequestBody TreatmentRecord record) {
        CurrentUser user = authorizationService.currentUser();
        if (!user.isAdmin() && !user.isDoctor()) {
            throw authorizationService.forbidden();
        }
        if (user.isDoctor()) {
            record.setDoctorId(authorizationService.requireDoctorId());
            record.setPaid(false);
        }
        if (record.getPatientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "患者不能为空");
        }
        if (!patientRepository.existsById(record.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "患者不存在");
        }
        if (record.getDate() == null) record.setDate(LocalDate.now());
        if (record.getPaid() == null) record.setPaid(false);
        record.setCode(null);
        TreatmentRecord saved = repo.save(record);
        saved.setCode("RC" + (20261000 + saved.getId()));
        return repo.save(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TreatmentRecord> update(@PathVariable Long id, @RequestBody TreatmentRecord body) {
        return repo.findById(id).map(record -> {
            if (!canAccess(record)) {
                throw authorizationService.forbidden();
            }
            if (!authorizationService.currentUser().isAdmin()) {
                throw authorizationService.forbidden();
            }
            Long keepId = record.getId();
            Long keepDoctorId = record.getDoctorId();
            Long keepPatientId = record.getPatientId();
            BeanUtils.copyProperties(body, record, "id", "doctorId", "patientId");
            record.setId(keepId);
            record.setDoctorId(keepDoctorId);
            record.setPatientId(keepPatientId);
            return ResponseEntity.ok(repo.save(record));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repo.findById(id).map(record -> {
            if (!canAccess(record)) {
                throw authorizationService.forbidden();
            }
            if (!authorizationService.currentUser().isAdmin()) {
                throw authorizationService.forbidden();
            }
            repo.delete(record);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private boolean canAccess(TreatmentRecord record) {
        CurrentUser user = authorizationService.currentUser();
        if (user.isAdmin() || user.isStaff()) {
            return true;
        }
        return user.isDoctor() && authorizationService.requireDoctorId().equals(record.getDoctorId());
    }
}

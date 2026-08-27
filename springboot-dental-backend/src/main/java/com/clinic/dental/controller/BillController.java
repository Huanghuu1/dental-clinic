package com.clinic.dental.controller;

import com.clinic.dental.entity.Bill;
import com.clinic.dental.repository.BillRepository;
import com.clinic.dental.security.AuthorizationService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillRepository repo;
    private final AuthorizationService authorizationService;

    public BillController(BillRepository repo, AuthorizationService authorizationService) {
        this.repo = repo;
        this.authorizationService = authorizationService;
    }

    /** 列表，支持按状态/患者过滤。收费仅由前台和管理员办理。 */
    @GetMapping
    public List<Bill> list(@RequestParam(required = false) String status,
                           @RequestParam(required = false) Long patientId) {
        authorizationService.requireAdminOrStaff();
        List<Bill> all = repo.findAll(Sort.by(Sort.Direction.DESC, "date"));
        return all.stream()
                .filter(bill -> status == null || status.isBlank() || status.equals(bill.getStatus()))
                .filter(bill -> patientId == null || patientId.equals(bill.getPatientId()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bill> get(@PathVariable Long id) {
        authorizationService.requireAdminOrStaff();
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Bill create(@RequestBody Bill bill) {
        authorizationService.requireAdminOrStaff();
        if (bill.getStatus() == null) bill.setStatus("待支付");
        bill.setCode(null);
        Bill saved = repo.save(bill);
        saved.setCode("BL" + (20260000 + saved.getId()));
        return repo.save(saved);
    }

    /** 确认收款。 */
    @PostMapping("/{id}/pay")
    public ResponseEntity<Bill> pay(@PathVariable Long id, @RequestBody(required = false) Bill body) {
        authorizationService.requireAdminOrStaff();
        return repo.findById(id).map(bill -> {
            bill.setStatus("已支付");
            bill.setMethod(body != null && body.getMethod() != null && !body.getMethod().isBlank()
                    ? body.getMethod() : "微信支付");
            return ResponseEntity.ok(repo.save(bill));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 退款。 */
    @PostMapping("/{id}/refund")
    public ResponseEntity<Bill> refund(@PathVariable Long id) {
        authorizationService.requireAdminOrStaff();
        return repo.findById(id).map(bill -> {
            bill.setStatus("已退款");
            bill.setMethod("原路退回");
            return ResponseEntity.ok(repo.save(bill));
        }).orElse(ResponseEntity.notFound().build());
    }
}

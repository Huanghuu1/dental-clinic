package com.clinic.dental.controller;

import com.clinic.dental.entity.Appointment;
import com.clinic.dental.entity.Bill;
import com.clinic.dental.entity.Patient;
import com.clinic.dental.entity.TreatmentRecord;
import com.clinic.dental.repository.AppointmentRepository;
import com.clinic.dental.repository.BillRepository;
import com.clinic.dental.repository.PatientRepository;
import com.clinic.dental.repository.TreatmentRecordRepository;
import com.clinic.dental.security.AuthorizationService;
import com.clinic.dental.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 仪表盘聚合统计接口。医生只能获得本人诊疗范围的统计。 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final TreatmentRecordRepository recordRepository;
    private final BillRepository billRepository;
    private final AuthorizationService authorizationService;

    public DashboardController(AppointmentRepository appointmentRepository,
                               PatientRepository patientRepository,
                               TreatmentRecordRepository recordRepository,
                               BillRepository billRepository,
                               AuthorizationService authorizationService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.recordRepository = recordRepository;
        this.billRepository = billRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        CurrentUser user = authorizationService.currentUser();
        LocalDate today = LocalDate.now();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        String month = today.format(monthFmt);
        String todayStr = today.toString();

        List<Appointment> appointments = user.isDoctor()
                ? appointmentRepository.findByDoctorId(authorizationService.requireDoctorId())
                : appointmentRepository.findAll();
        List<TreatmentRecord> records = user.isDoctor()
                ? recordRepository.findByDoctorId(authorizationService.requireDoctorId())
                : recordRepository.findAll();
        List<Bill> bills = user.isDoctor() ? List.of() : billRepository.findAll();
        List<Patient> patients = user.isDoctor() ? List.of() : patientRepository.findAll();

        long todayAppointments = appointments.stream().filter(a -> todayStr.equals(a.getDate().toString())).count();
        long todayPending = appointments.stream().filter(a -> todayStr.equals(a.getDate().toString()) && "待就诊".equals(a.getStatus())).count();
        long todayDone = appointments.stream().filter(a -> todayStr.equals(a.getDate().toString()) && "已完成".equals(a.getStatus())).count();
        long newPatients = patients.stream().filter(p -> p.getCreated() != null && month.equals(p.getCreated().format(monthFmt))).count();
        long unpaidCount = bills.stream().filter(b -> "待支付".equals(b.getStatus())).count();

        BigDecimal monthRevenue = bills.stream()
                .filter(b -> "已支付".equals(b.getStatus()) && b.getDate() != null && month.equals(b.getDate().format(monthFmt)))
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaidAmount = bills.stream()
                .filter(b -> "待支付".equals(b.getStatus()))
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("date", todayStr);
        response.put("month", month);
        response.put("todayAppointments", todayAppointments);
        response.put("todayPending", todayPending);
        response.put("todayDone", todayDone);
        response.put("newPatientsThisMonth", newPatients);
        response.put("totalPatients", patients.size());
        response.put("monthRevenue", monthRevenue);
        response.put("unpaidCount", unpaidCount);
        response.put("unpaidAmount", unpaidAmount);
        response.put("totalRecords", records.size());
        response.put("totalBills", bills.size());
        return response;
    }
}

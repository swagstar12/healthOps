package com.healthops.api;

import com.healthops.api.dto.DoctorDtos.AvailabilityRequest;
import com.healthops.api.dto.DoctorDtos.HolidayRequest;
import com.healthops.doctor.*;
import com.healthops.patient.Patient;
import com.healthops.patient.PatientRepository;
import com.healthops.visit.Visit;
import com.healthops.visit.VisitRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
public class DoctorController {
  private final DoctorRepository doctorRepo;
  private final AvailabilityRepository availRepo;
  private final HolidayRepository holidayRepo;
  private final PatientRepository patientRepo;
  private final VisitRepository visitRepo;

  public DoctorController(DoctorRepository doctorRepo, AvailabilityRepository availRepo, HolidayRepository holidayRepo, PatientRepository patientRepo, VisitRepository visitRepo) {
    this.doctorRepo = doctorRepo; this.availRepo = availRepo; this.holidayRepo = holidayRepo; this.patientRepo = patientRepo; this.visitRepo = visitRepo;
  }

  @GetMapping("/patients")
  public List<Patient> listPatients() { return patientRepo.findAll(); }

  @PutMapping("/patients/{id}")
  public Patient editPatient(@PathVariable Long id, @RequestBody Patient updated) {
    var p = patientRepo.findById(id).orElseThrow();
    p.setFullName(updated.getFullName());
    p.setDob(updated.getDob());
    p.setPhone(updated.getPhone());
    p.setAddress(updated.getAddress());
    return patientRepo.save(p);
  }

  @PostMapping("/availability/{doctorId}")
  public Availability addAvailability(@PathVariable Long doctorId, @RequestBody AvailabilityRequest req) {
    var d = doctorRepo.findById(doctorId).orElseThrow();
    var a = Availability.builder().doctor(d).dayOfWeek(req.dayOfWeek()).startTime(req.startTime()).endTime(req.endTime()).build();
    return availRepo.save(a);
  }

  @PostMapping("/holidays/{doctorId}")
  public Holiday addHoliday(@PathVariable Long doctorId, @RequestBody HolidayRequest req) {
    var d = doctorRepo.findById(doctorId).orElseThrow();
    var h = Holiday.builder().doctor(d).date(req.date()).reason(req.reason()).build();
    return holidayRepo.save(h);
  }

  @PostMapping("/visits")
  public Visit createVisit(@RequestBody Visit v) { return visitRepo.save(v); }

  @GetMapping("/visits/patient/{patientId}")
  public List<Visit> getPatientVisits(@PathVariable Long patientId) {
    return visitRepo.findByPatientIdOrderByVisitAtDesc(patientId);
  }
}

package com.healthops.api;

import com.healthops.api.dto.AppointmentDtos.*;
import com.healthops.api.dto.PatientDtos.*;
import com.healthops.appointment.Appointment;
import com.healthops.appointment.AppointmentRepository;
import com.healthops.doctor.DoctorRepository;
import com.healthops.patient.Patient;
import com.healthops.patient.PatientRepository;
import com.healthops.user.Role;
import com.healthops.user.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reception")
@PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
public class ReceptionistController {

  private final PatientRepository patientRepo;
  private final AppointmentRepository apptRepo;
  private final DoctorRepository doctorRepo;
  private final UserService userService;

  public ReceptionistController(PatientRepository patientRepo, AppointmentRepository apptRepo, DoctorRepository doctorRepo, UserService userService) {
    this.patientRepo = patientRepo; this.apptRepo = apptRepo; this.doctorRepo = doctorRepo; this.userService = userService;
  }

  // Patients CRUD
  @PostMapping("/patients")
  public Patient createPatient(@RequestBody CreatePatientRequest req) {
    var p = Patient.builder().code(req.code()).fullName(req.fullName()).dob(req.dob()).phone(req.phone()).address(req.address()).build();
    return patientRepo.save(p);
  }

  @GetMapping("/patients")
  public List<Patient> listPatients() { return patientRepo.findAll(); }

  @PutMapping("/patients/{id}")
  public Patient updatePatient(@PathVariable Long id, @RequestBody UpdatePatientRequest req) {
    var p = patientRepo.findById(id).orElseThrow();
    p.setFullName(req.fullName()); p.setDob(req.dob()); p.setPhone(req.phone()); p.setAddress(req.address());
    return patientRepo.save(p);
  }

  @DeleteMapping("/patients/{id}")
  public void deletePatient(@PathVariable Long id) { patientRepo.deleteById(id); }

  // Doctors CRUD (basic add via creating DOCTOR user)
  @PostMapping("/doctors")
  public com.healthops.doctor.Doctor addDoctor(@RequestBody com.healthops.api.dto.DoctorDtos.CreateDoctorRequest req) {
    var u = userService.register(req.email(), req.fullName(), req.password(), Role.DOCTOR);
    var d = com.healthops.doctor.Doctor.builder().user(u).specialization(req.specialization()).phone(req.phone()).build();
    return doctorRepo.save(d);
  }

  @DeleteMapping("/doctors/{id}")
  public void deleteDoctor(@PathVariable Long id) { doctorRepo.deleteById(id); }

  // Appointments
  @PostMapping("/appointments")
  public Appointment createAppointment(@RequestBody CreateAppointmentRequest req) {
    var p = patientRepo.findById(req.patientId()).orElseThrow();
    var d = doctorRepo.findById(req.doctorId()).orElseThrow();
    var a = Appointment.builder().patient(p).doctor(d).scheduledAt(req.scheduledAt()).status("SCHEDULED").reason(req.reason()).build();
    return apptRepo.save(a);
  }

  @GetMapping("/appointments")
  public List<Appointment> listAppointments() { return apptRepo.findAll(); }

  @PutMapping("/appointments/{id}/status")
  public Appointment updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest req) {
    var a = apptRepo.findById(id).orElseThrow();
    a.setStatus(req.status());
    return apptRepo.save(a);
  }
}

package com.healthops.api;

import com.healthops.api.dto.DoctorDtos.CreateDoctorRequest;
import com.healthops.doctor.Doctor;
import com.healthops.doctor.DoctorRepository;
import com.healthops.user.Role;
import com.healthops.user.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final UserService userService;
  private final DoctorRepository doctorRepo;

  public AdminController(UserService userService, DoctorRepository doctorRepo) {
    this.userService = userService; this.doctorRepo = doctorRepo;
  }

  @PostMapping("/doctors")
  public Doctor createDoctor(@RequestBody CreateDoctorRequest req) {
    var u = userService.register(req.email(), req.fullName(), req.password(), Role.DOCTOR);
    Doctor d = Doctor.builder().user(u).specialization(req.specialization()).phone(req.phone()).build();
    return doctorRepo.save(d);
  }

  @GetMapping("/doctors")
  public List<Doctor> listDoctors() { return doctorRepo.findAll(); }
}

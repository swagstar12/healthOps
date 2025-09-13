package com.healthops.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
  List<Appointment> findByDoctorId(Long doctorId);
  List<Appointment> findByPatientId(Long patientId);
}

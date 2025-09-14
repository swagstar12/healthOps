package com.healthops.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
  List<Appointment> findByDoctorId(Long doctorId);
  List<Appointment> findByPatientId(Long patientId);
  
  long countByDoctorId(Long doctorId);
  long countByStatus(String status);
  
  @Query("SELECT COUNT(a) FROM Appointment a WHERE DATE(a.scheduledAt) = CURRENT_DATE")
  long countTodayAppointments();
  
  @Query("SELECT a FROM Appointment a WHERE a.patient.fullName ILIKE %:query% OR a.patient.code ILIKE %:query%")
  List<Appointment> findByPatientFullNameContainingIgnoreCaseOrPatientCodeContainingIgnoreCase(@Param("query") String query1, @Param("query") String query2);
}
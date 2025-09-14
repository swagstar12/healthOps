package com.healthops.visit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
  List<Visit> findByPatientIdOrderByVisitAtDesc(Long patientId);
  List<Visit> findByDoctorIdOrderByVisitAtDesc(Long doctorId);
  
  long countByPatientId(Long patientId);
  long countByDoctorId(Long doctorId);
  
  @Query("SELECT COUNT(v) FROM Visit v WHERE v.doctor.id = :doctorId AND DATE(v.visitAt) = CURRENT_DATE")
  long countTodayVisitsByDoctor(@Param("doctorId") Long doctorId);
}
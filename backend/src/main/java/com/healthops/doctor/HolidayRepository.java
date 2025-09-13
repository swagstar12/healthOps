package com.healthops.doctor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
  List<Holiday> findByDoctorId(Long doctorId);
  boolean existsByDoctorIdAndDate(Long doctorId, LocalDate date);
}

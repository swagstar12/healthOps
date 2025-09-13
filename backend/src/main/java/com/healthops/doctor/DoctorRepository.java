package com.healthops.doctor;

import com.healthops.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
  Optional<Doctor> findByUser(User user);
}

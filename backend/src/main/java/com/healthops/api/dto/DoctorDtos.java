package com.healthops.api.dto;

import java.time.LocalTime;

public class DoctorDtos {
  public record CreateDoctorRequest(String email, String fullName, String password, String specialization, String phone) {}
  public record AvailabilityRequest(int dayOfWeek, LocalTime startTime, LocalTime endTime) {}
  public record HolidayRequest(java.time.LocalDate date, String reason) {}
}

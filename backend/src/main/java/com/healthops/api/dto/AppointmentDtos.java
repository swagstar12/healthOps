package com.healthops.api.dto;

import java.time.Instant;

public class AppointmentDtos {
  public record CreateAppointmentRequest(Long patientId, Long doctorId, Instant scheduledAt, String reason) {}
  public record UpdateStatusRequest(String status) {}
}

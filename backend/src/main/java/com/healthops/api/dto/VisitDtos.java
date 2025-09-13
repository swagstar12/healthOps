package com.healthops.api.dto;

public class VisitDtos {
  public record CreateVisitRequest(Long appointmentId, Long patientId, Long doctorId, String notes, String diagnosis, String prescription) {}
}

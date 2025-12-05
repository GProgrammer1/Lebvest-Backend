package com.lebvest.model.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveProjectRequest {
    @Size(max = 1000, message = "Review notes must not exceed 1000 characters")
    private String reviewNotes;
}


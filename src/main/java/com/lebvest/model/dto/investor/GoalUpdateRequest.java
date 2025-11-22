package com.lebvest.model.dto.investor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalUpdateRequest {

    @Size(min = 1, message = "Goal name cannot be blank")
    private String name;

    @DecimalMin(value = "0.0", inclusive = false, message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    private LocalDate deadline;

    @JsonIgnore
    private boolean deadlineProvided;

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
        this.deadlineProvided = true;
    }

    public boolean hasUpdates() {
        return name != null || targetAmount != null || deadlineProvided;
    }
}


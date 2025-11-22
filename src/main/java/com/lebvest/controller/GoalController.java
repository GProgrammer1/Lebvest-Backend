package com.lebvest.controller;

import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.dto.investor.GoalRequest;
import com.lebvest.model.dto.investor.GoalUpdateRequest;
import com.lebvest.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/investors/me/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<ResponsePayload> createGoal(@Valid @RequestBody GoalRequest request) {
        var goal = goalService.createGoal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponsePayload.builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Goal created successfully")
                        .data(Map.of("goal", goal))
                        .build());
    }

    @PatchMapping("/{goalId}")
    public ResponseEntity<ResponsePayload> updateGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody GoalUpdateRequest request
    ) {
        var goal = goalService.updateGoal(goalId, request);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(HttpStatus.OK.value())
                        .message("Goal updated successfully")
                        .data(Map.of("goal", goal))
                        .build()
        );
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<ResponsePayload> deleteGoal(@PathVariable Long goalId) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(HttpStatus.OK.value())
                        .message("Goal deleted successfully")
                        .data(Map.of("goalId", goalId))
                        .build()
        );
    }
}


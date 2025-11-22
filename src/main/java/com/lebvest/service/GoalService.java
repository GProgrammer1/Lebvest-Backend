package com.lebvest.service;

import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.dto.investor.GoalRequest;
import com.lebvest.model.dto.investor.GoalResponse;
import com.lebvest.model.dto.investor.GoalUpdateRequest;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.InvestorGoal;
import com.lebvest.repository.GoalRepository;
import com.lebvest.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final InvestorRepository investorRepository;

    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        Investor investor = getCurrentInvestor();

        InvestorGoal goal = new InvestorGoal();
        goal.setInvestor(investor);
        goal.setName(normalizeName(request.getName()));
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setDeadline(request.getDeadline());

        InvestorGoal savedGoal = goalRepository.save(goal);
        return toResponse(savedGoal);
    }

    @Transactional
    public GoalResponse updateGoal(Long goalId, GoalUpdateRequest request) {
        if (!request.hasUpdates()) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        Investor investor = getCurrentInvestor();
        InvestorGoal goal = findGoalForInvestor(goalId, investor.getId());

        if (request.getName() != null) {
            goal.setName(normalizeName(request.getName()));
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.isDeadlineProvided()) {
            goal.setDeadline(request.getDeadline());
        }

        InvestorGoal savedGoal = goalRepository.save(goal);
        return toResponse(savedGoal);
    }

    @Transactional
    public void deleteGoal(Long goalId) {
        Investor investor = getCurrentInvestor();
        InvestorGoal goal = findGoalForInvestor(goalId, investor.getId());
        goalRepository.delete(goal);
    }

    private InvestorGoal findGoalForInvestor(Long goalId, Long investorId) {
        return goalRepository.findByIdAndInvestorId(goalId, investorId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found for current investor"));
    }

    private Investor getCurrentInvestor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Unable to determine authenticated investor");
        }
        String email = authentication.getName();
        return investorRepository.findOneWithDetailsByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found for current user"));
    }

    private GoalResponse toResponse(InvestorGoal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .deadline(goal.getDeadline())
                .build();
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Goal name cannot be blank");
        }
        return name.trim();
    }
}


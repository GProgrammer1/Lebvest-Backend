package com.lebvest.service;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.repository.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentRepository investmentRepo;

    public List<Investment> searchInvestments(String query) {
        return investmentRepo.searchInvestments(query);
    }
}

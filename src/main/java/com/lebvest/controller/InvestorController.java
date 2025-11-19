package com.lebvest.controller;

import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.service.InvestorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/investors/me")
public class InvestorController {

    private final InvestorService investorService;

    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ResponsePayload> getDashboard() {
        var dashboard = investorService.getCurrentInvestorDashboard();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor dashboard fetched successfully")
                        .data(Map.of("dashboard", dashboard))
                        .build()
        );
    }

    @GetMapping("/investments")
    public ResponseEntity<ResponsePayload> getInvestments() {
        var investments = investorService.getCurrentInvestorInvestments();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor investments fetched successfully")
                        .data(Map.of("investments", investments))
                        .build()
        );
    }

    @GetMapping("/watchlist")
    public ResponseEntity<ResponsePayload> getWatchlist() {
        var watchlist = investorService.getCurrentInvestorWatchlist();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor watchlist fetched successfully")
                        .data(Map.of("watchlist", watchlist))
                        .build()
        );
    }

    @GetMapping("/goals")
    public ResponseEntity<ResponsePayload> getGoals() {
        var goals = investorService.getCurrentInvestorGoals();
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investor goals fetched successfully")
                        .data(Map.of("goals", goals))
                        .build()
        );
    }
}


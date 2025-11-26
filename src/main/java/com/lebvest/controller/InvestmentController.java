package com.lebvest.controller;

import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @GetMapping("/search")
    public ResponseEntity<ResponsePayload> searchInvestments(@RequestParam("q") String query) {
        List<Investment> investments = investmentService.searchInvestments(query);
        return ResponseEntity.ok(ResponsePayload.builder()
                .message("Investments found")
                .status(200)
                .data(Map.of("investments", investments))
                .build());
    }
}

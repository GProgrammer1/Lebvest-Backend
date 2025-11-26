package com.lebvest.controller;

import com.lebvest.model.dto.investment.InvestRequest;
import com.lebvest.model.dto.investment.InvestmentDetailResponse;
import com.lebvest.model.entities.investment.InvestmentDocument;
import com.lebvest.model.entities.investment.InvestmentUpdate;
import com.lebvest.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @GetMapping("/{id}")
    public ResponseEntity<InvestmentDetailResponse> getInvestmentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(investmentService.getInvestmentDetail(id));
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<InvestmentDocument>> getInvestmentDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(investmentService.getInvestmentDocuments(id));
    }

    @GetMapping("/{id}/updates")
    public ResponseEntity<List<InvestmentUpdate>> getInvestmentUpdates(@PathVariable Long id) {
        return ResponseEntity.ok(investmentService.getInvestmentUpdates(id));
    }

    @PostMapping("/{id}/invest")
    public ResponseEntity<Void> invest(@PathVariable Long id, @RequestBody InvestRequest request, Authentication authentication) {
        investmentService.invest(id, request, authentication.getName());
        return ResponseEntity.ok().build();
    }
}

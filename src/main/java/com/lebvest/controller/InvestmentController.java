package com.lebvest.controller;

import com.lebvest.model.dto.InvestmentDto;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.dto.investment.InvestRequest;
import com.lebvest.model.dto.investment.InvestmentDetailResponse;
import com.lebvest.model.entities.investment.InvestmentDocument;
import com.lebvest.model.entities.investment.InvestmentUpdate;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import com.lebvest.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/investments")
@CrossOrigin("http://localhost:3000")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    // Endpoints from dev branch
    @GetMapping
    public ResponseEntity<ResponsePayload> getInvestments(
            @RequestParam(required = false) InvestmentCategory category,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) BigDecimal minReturn,
            @RequestParam(required = false) Location location,
            @RequestParam(required = false) CompanySector sector,
            @RequestParam(required = false) InvestmentType investmentType,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false, defaultValue = "created:DESC") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        Page<InvestmentDto> investments = investmentService.getInvestments(
                category, riskLevel, minReturn, location, sector, investmentType,
                minAmount, maxAmount, sort, page, size
        );

        Map<String, Object> data = new HashMap<>();
        data.put("investments", investments.getContent());
        data.put("totalElements", investments.getTotalElements());
        data.put("totalPages", investments.getTotalPages());
        data.put("currentPage", investments.getNumber());
        data.put("pageSize", investments.getSize());
        data.put("hasNext", investments.hasNext());
        data.put("hasPrevious", investments.hasPrevious());

        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investments retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/featured")
    public ResponseEntity<ResponsePayload> getFeaturedInvestments(
            @RequestParam(required = false, defaultValue = "6") int limit) {

        List<InvestmentDto> investments = investmentService.getFeaturedInvestments(limit);

        Map<String, Object> data = new HashMap<>();
        data.put("investments", investments);

        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Featured investments retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/{id}/watchlist")
    public ResponseEntity<ResponsePayload> addToWatchlist(@PathVariable Long id) {
        investmentService.addToWatchlist(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment added to watchlist successfully")
                        .data(Map.of("investmentId", id))
                        .build()
        );
    }

    @DeleteMapping("/{id}/watchlist")
    public ResponseEntity<ResponsePayload> removeFromWatchlist(@PathVariable Long id) {
        investmentService.removeFromWatchlist(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment removed from watchlist successfully")
                        .data(Map.of("investmentId", id))
                        .build()
        );
    }

    // Endpoints from yahya branch
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

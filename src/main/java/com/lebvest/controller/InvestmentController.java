package com.lebvest.controller;

import com.lebvest.model.dto.*;
import com.lebvest.model.entities.investment.InvestorInvestment;
import jakarta.validation.Valid;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import com.lebvest.service.InvestmentService;
import com.lebvest.service.InvestmentRequestService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/investments")
@CrossOrigin("http://localhost:3000")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final InvestmentRequestService investmentRequestService;

    public InvestmentController(InvestmentService investmentService, InvestmentRequestService investmentRequestService) {
        this.investmentService = investmentService;
        this.investmentRequestService = investmentRequestService;
    }

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

    @GetMapping("/{id}/stats")
    public ResponseEntity<ResponsePayload> getInvestmentStats(@PathVariable Long id) {
        InvestmentStatsDto stats = investmentService.getInvestmentStats(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment statistics fetched successfully")
                        .data(Map.of("stats", stats))
                        .build()
        );
    }

    @GetMapping("/{id}/watchlist/status")
    public ResponseEntity<ResponsePayload> getWatchlistStatus(@PathVariable Long id) {
        com.lebvest.model.dto.WatchlistStatusDto status = investmentService.getWatchlistStatus(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Watchlist status retrieved successfully")
                        .data(Map.of("watchlistStatus", status))
                        .build()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ResponsePayload> searchInvestments(
            @RequestParam("q") String query,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Page<InvestmentDto> investments = investmentService.searchInvestments(query, page, size);
        
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
                        .message("Investments search completed successfully")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsePayload> getInvestmentById(@PathVariable Long id) {
        InvestmentDto investment = investmentService.getInvestmentById(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment details retrieved successfully")
                        .data(Map.of("investment", investment))
                        .build()
        );
    }

    @PostMapping("/{id}/invest")
    public ResponseEntity<ResponsePayload> makeInvestment(
            @PathVariable Long id,
            @RequestBody @Valid MakeInvestmentRequest request) {
        // Create investment request instead of direct investment
        com.lebvest.model.entities.investment.InvestmentRequest investmentRequest = 
                investmentRequestService.createInvestmentRequest(id, 
                        new CreateInvestmentRequestRequest() {{
                            setAmount(request.getAmount());
                        }});
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("id", investmentRequest.getId());
        requestData.put("amount", investmentRequest.getAmount());
        requestData.put("status", investmentRequest.getStatus().toString());
        requestData.put("createdAt", investmentRequest.getCreatedAt());
        
        return ResponseEntity.status(201).body(
                ResponsePayload.builder()
                        .status(201)
                        .message("Investment request submitted successfully. The company will review your request.")
                        .data(Map.of("investmentRequest", requestData))
                        .build()
        );
    }

    @GetMapping("/{id}/updates")
    public ResponseEntity<ResponsePayload> getInvestmentUpdates(@PathVariable Long id) {
        List<InvestmentDto.UpdateDto> updates = investmentService.getInvestmentUpdates(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Investment updates retrieved successfully")
                        .data(Map.of("updates", updates))
                        .build()
        );
    }
}




package com.lebvest.controller;

import com.lebvest.model.dto.CompanyProfileDto;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.Location;
import com.lebvest.service.CompanyService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/companies")
@CrossOrigin("http://localhost:3000")
public class PublicCompanyController {

    private final CompanyService companyService;

    public PublicCompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsePayload> getCompanyProfile(@PathVariable Long id) {
        CompanyProfileDto profile = companyService.getCompanyProfileById(id);
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Company profile retrieved successfully")
                        .data(Map.of("profile", profile))
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ResponsePayload> getAllCompanies(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) CompanySector sector,
            @RequestParam(required = false) Location location) {
        Page<CompanyProfileDto> companies = companyService.getAllCompanies(page, size, sector, location);
        
        Map<String, Object> data = new HashMap<>();
        data.put("companies", companies.getContent());
        data.put("totalElements", companies.getTotalElements());
        data.put("totalPages", companies.getTotalPages());
        data.put("currentPage", companies.getNumber());
        data.put("pageSize", companies.getSize());
        data.put("hasNext", companies.hasNext());
        data.put("hasPrevious", companies.hasPrevious());
        
        return ResponseEntity.ok(
                ResponsePayload.builder()
                        .status(200)
                        .message("Companies retrieved successfully")
                        .data(data)
                        .build()
        );
    }
}


package com.lebvest.controller;

import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.entities.company.CompanyFinancial;
import com.lebvest.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/me/financials")
    public ResponseEntity<ResponsePayload> addFinancials(@RequestBody CompanyFinancial financial) {
        CompanyFinancial savedFinancial = companyService.addFinancials(financial);
        return ResponseEntity.ok(ResponsePayload.builder()
                .message("Financial data added successfully")
                .status(200)
                .data(Map.of("financial", savedFinancial))
                .build());
    }

    @PostMapping("/me/documents")
    public ResponseEntity<ResponsePayload> addDocument(@RequestParam("file") MultipartFile file) {
        String documentUrl = companyService.addDocument(file);
        return ResponseEntity.ok(ResponsePayload.builder()
                .message("Document uploaded successfully")
                .status(200)
                .data(Map.of("documentUrl", documentUrl))
                .build());
    }
}

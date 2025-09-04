package com.lebvest.model.dto;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@Embeddable
@SuperBuilder
public class CompanyDocumentEntry {
    private String document;

    // Required by JPA
    public CompanyDocumentEntry() {}

    public CompanyDocumentEntry(String document) {
        this.document = document;
    }

}

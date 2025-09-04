package com.lebvest.model.dto;

// define a simple DTO
public record Attachment(String filename, byte[] data, String contentType) {}


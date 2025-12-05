package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistStatusDto {
    private boolean isWatchlisted;
    private LocalDateTime addedAt; // When it was added to watchlist, if watchlisted
}


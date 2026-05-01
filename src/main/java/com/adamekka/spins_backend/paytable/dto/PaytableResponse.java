package com.adamekka.spins_backend.paytable.dto;

import java.util.List;

public record PaytableResponse(
    Long id,
    String name,
    int reelCount,
    int rowCount,
    List<PaytableSymbolResponse> symbols
) {}

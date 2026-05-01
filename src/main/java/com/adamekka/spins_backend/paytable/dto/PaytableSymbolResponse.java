package com.adamekka.spins_backend.paytable.dto;

import com.adamekka.spins_backend.paytable.model.SymbolType;

public record PaytableSymbolResponse(
    String code, SymbolType symbolType, PayoutsResponse payouts
) {}

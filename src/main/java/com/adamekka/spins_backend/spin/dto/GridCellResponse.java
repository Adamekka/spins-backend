package com.adamekka.spins_backend.spin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GridCellResponse(String code, Integer multiplierValue) {}

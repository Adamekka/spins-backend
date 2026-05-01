package com.adamekka.spins_backend.paytable.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record PayoutsResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal low,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal mid,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal high
) {}

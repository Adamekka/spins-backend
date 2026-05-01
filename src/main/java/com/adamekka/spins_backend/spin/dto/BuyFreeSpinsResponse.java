package com.adamekka.spins_backend.spin.dto;

import com.adamekka.spins_backend.spin.model.SpinType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record BuyFreeSpinsResponse(
    Long spinId,
    SpinType spinType,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cost,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal newBalance,
    int freeSpinsAwarded,
    Long parentSpinId
) {}

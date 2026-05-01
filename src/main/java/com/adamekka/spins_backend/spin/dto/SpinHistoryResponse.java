package com.adamekka.spins_backend.spin.dto;

import com.adamekka.spins_backend.spin.model.SpinType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SpinHistoryResponse(
    Long id,
    SpinType spinType,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal bet,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalWin,
    LocalDateTime spunAt,
    int tumbleCount
) {}

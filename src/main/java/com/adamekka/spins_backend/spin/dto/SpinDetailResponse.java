package com.adamekka.spins_backend.spin.dto;

import com.adamekka.spins_backend.spin.model.SpinType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpinDetailResponse(
    Long id,
    SpinType spinType,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal bet,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalWin,
    LocalDateTime spunAt,
    Long parentSpinId,
    int accumulatedMultiplier,
    int remainingFreeSpins,
    List<TumbleResponse> tumbles
) {}

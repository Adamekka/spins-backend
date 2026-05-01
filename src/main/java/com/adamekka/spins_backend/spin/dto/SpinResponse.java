package com.adamekka.spins_backend.spin.dto;

import com.adamekka.spins_backend.spin.model.SpinType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpinResponse(
    Long spinId,
    SpinType spinType,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal bet,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalWin,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal newBalance,
    List<TumbleResponse> tumbles,
    Boolean freeSpinsTriggered,
    Integer freeSpinsAwarded,
    Integer accumulatedMultiplier,
    Integer remainingFreeSpins,
    Boolean retriggered,
    Integer retriggerAwarded,
    Boolean sessionComplete
) {}

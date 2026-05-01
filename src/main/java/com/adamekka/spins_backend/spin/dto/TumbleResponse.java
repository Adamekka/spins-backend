package com.adamekka.spins_backend.spin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.List;

public record TumbleResponse(
    int sequenceIndex,
    List<List<GridCellResponse>> grid,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal winAmount,
    int multiplierOnGrid
) {}

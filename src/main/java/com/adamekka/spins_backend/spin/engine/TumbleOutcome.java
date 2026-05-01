package com.adamekka.spins_backend.spin.engine;

import com.adamekka.spins_backend.spin.dto.GridCellResponse;
import java.math.BigDecimal;
import java.util.List;

public record TumbleOutcome(
    int sequenceIndex,
    List<List<GridCellResponse>> grid,
    BigDecimal winAmount,
    int multiplierOnGrid
) {}

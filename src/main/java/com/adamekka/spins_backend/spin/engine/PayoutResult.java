package com.adamekka.spins_backend.spin.engine;

import java.math.BigDecimal;
import java.util.Set;

public record PayoutResult(
    BigDecimal winAmount, Set<String> winningRegularCodes, int scatterCount
) {}

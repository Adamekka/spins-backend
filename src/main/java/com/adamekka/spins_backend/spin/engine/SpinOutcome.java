package com.adamekka.spins_backend.spin.engine;

import java.math.BigDecimal;
import java.util.List;

public record SpinOutcome(
    List<TumbleOutcome> tumbles,
    BigDecimal baseWin,
    int finalMultiplier,
    int maxScatterCount
) {}

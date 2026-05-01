package com.adamekka.spins_backend.spin.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SpinRequest(@NotNull Long paytableId, @NotNull BigDecimal bet) {}

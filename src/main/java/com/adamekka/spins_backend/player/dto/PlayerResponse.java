package com.adamekka.spins_backend.player.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record PlayerResponse(
    Long id,
    String username,
    @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance
) {}

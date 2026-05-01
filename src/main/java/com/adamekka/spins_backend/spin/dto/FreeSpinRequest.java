package com.adamekka.spins_backend.spin.dto;

import jakarta.validation.constraints.NotNull;

public record FreeSpinRequest(@NotNull Long parentSpinId) {}

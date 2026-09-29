package com.example.caseplatform.dto;

import com.example.caseplatform.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record OrderUpdateRequest(@NotNull @PositiveOrZero Long version, @NotNull OrderStatus status) {}

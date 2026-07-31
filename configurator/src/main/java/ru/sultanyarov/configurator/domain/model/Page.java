package ru.sultanyarov.configurator.domain.model;

import java.util.List;

public record Page<T>(
        List<T> items,
        int page,
        int size,
        long totalItems
) {
}

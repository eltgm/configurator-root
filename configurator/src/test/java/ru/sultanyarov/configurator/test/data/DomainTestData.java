package ru.sultanyarov.configurator.test.data;

import ru.sultanyarov.configurator.domain.model.Domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

public class DomainTestData {
    private static final AtomicLong idGenerator = new AtomicLong(1);

    public static Domain domain() {
        return new Domain(
                idGenerator.getAndIncrement(),
                "Test Domain",
                "Test Description",
                1L,
                Collections.emptyList(),
                LocalDateTime.now()
        );
    }

    public static Domain domainWithName(String name) {
        return new Domain(
                idGenerator.getAndIncrement(),
                name,
                "Test Description",
                1L,
                Collections.emptyList(),
                LocalDateTime.now()
        );
    }

    public static Domain domainWithId(Long id) {
        return new Domain(
                id,
                "Test Domain",
                "Test Description",
                1L,
                Collections.emptyList(),
                LocalDateTime.now()
        );
    }

    public static Domain domainWithIdAndName(Long id, String name) {
        return new Domain(
                id,
                name,
                "Test Description",
                1L,
                Collections.emptyList(),
                LocalDateTime.now()
        );
    }
}

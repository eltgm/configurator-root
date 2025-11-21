package ru.sultanyarov.configurator.test.data;

import ru.sultanyarov.configurator.domain.model.Domain;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class DomainTestData {
    private static final AtomicLong idGenerator = new AtomicLong(1);

    public static Domain domain() {
        return new Domain(
                idGenerator.getAndIncrement(),
                "Test Domain",
                "Test Description",
                1L,
                OffsetDateTime.now()
        );
    }

    public static Domain domainWithName(String name) {
        return new Domain(
                idGenerator.getAndIncrement(),
                name,
                "Test Description",
                1L,
                OffsetDateTime.now()
        );
    }

    public static Domain domainWithId(Long id) {
        return new Domain(
                id,
                "Test Domain",
                "Test Description",
                1L,
                OffsetDateTime.now()
        );
    }

    public static Domain domainWithIdAndName(Long id, String name) {
        return new Domain(
                id,
                name,
                "Test Description",
                1L,
                OffsetDateTime.now()
        );
    }
}

package ru.sultanyarov.configurator.test.data;

import ru.sultanyarov.configurator.domain.model.ComponentType;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class ComponentTypeTestData {
    private static final AtomicLong idGenerator = new AtomicLong(1);

    public static ComponentType componentType() {
        return ComponentType.builder()
                .id(idGenerator.getAndIncrement())
                .domainId(1L)
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)
                .domain(DomainTestData.domainWithId(1L))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ComponentType componentTypeWithName(String name) {
        return ComponentType.builder()
                .id(idGenerator.getAndIncrement())
                .domainId(1L)
                .name(name)
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)
                .domain(DomainTestData.domainWithId(1L))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ComponentType componentTypeWithId(Long id) {
        return ComponentType.builder()
                .id(id)
                .domainId(1L)
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)
                .domain(DomainTestData.domainWithId(1L))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ComponentType componentTypeWithIdAndName(Long id, String name) {
        return ComponentType.builder()
                .id(id)
                .domainId(1L)
                .name(name)
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)
                .domain(DomainTestData.domainWithId(1L))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ComponentType componentTypeWithDomainId(Long domainId) {
        return ComponentType.builder()
                .id(idGenerator.getAndIncrement())
                .domainId(domainId)
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)
                .domain(DomainTestData.domainWithId(domainId))
                .createdAt(LocalDateTime.now())
                .build();
    }
}

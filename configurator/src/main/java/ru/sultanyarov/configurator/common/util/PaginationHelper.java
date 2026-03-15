package ru.sultanyarov.configurator.common.util;

import lombok.experimental.UtilityClass;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.SelectSeekStepN;
import org.jooq.Table;
import ru.sultanyarov.configurator.domain.model.Page;

@UtilityClass
public class PaginationHelper {
    public static <T> Page<T> jooqPage(
            DSLContext dsl,
            SelectSeekStepN<?> query,
            Table<?> table,
            int page,
            int size,
            RecordMapper<? super Record, T> mapper
    ) {
        var items = query.limit(size)
                .offset(page * size)
                .fetch(mapper);
        var count = dsl.selectCount()
                .from(table)
                .fetchOptional(0, long.class)
                .orElse(0L);

        return new Page<>(items, page, size, count);
    }
}

package com.orderdesk.hidden;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Tests that create, pay and ship orders share one migrated copy of the snapshot. */
abstract class HiddenT3MutableSupport extends HiddenT3Support {

    private static final java.nio.file.Path DATABASE = copySnapshot();

    @DynamicPropertySource
    static void mutableDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> urlOf(DATABASE));
    }
}

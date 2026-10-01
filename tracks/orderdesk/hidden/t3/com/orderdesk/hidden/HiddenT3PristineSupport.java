package com.orderdesk.hidden;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Read-only checks of the migrated snapshot run in their own application context on their own copy of the
 * database, so that tests which ship or pay orders cannot change what they observe.
 */
abstract class HiddenT3PristineSupport extends HiddenT3Support {

    private static final java.nio.file.Path PRISTINE = copySnapshot();

    @DynamicPropertySource
    static void pristineDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> urlOf(PRISTINE));
    }
}

package com.orderdesk.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** The application starts on the v1 production data: existing migrations untouched, new ones applied. */
class HiddenT3StartupTest extends HiddenT3PristineSupport {

    @Autowired
    private DataSource dataSource;

    @Test
    void newMigrationsAppliedOnTopOfExistingHistory() {
        List<Map<String, Object>> history = new JdbcTemplate(dataSource).queryForList(
                "SELECT \"version\", \"success\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"");
        assertThat(history).allMatch(row -> Boolean.TRUE.equals(row.get("success")));
        assertThat(history.get(0).get("version")).isEqualTo("1");
        assertThat(history.get(1).get("version")).isEqualTo("2");
        assertThat(history.size()).isGreaterThan(2);
    }

    @Test
    void existingOrdersStillReadable() {
        for (var o : MANIFEST.get("orders")) {
            assertThat(call("GET", "/api/orders/" + o.get("id").asLong(), null).status()).isEqualTo(200);
        }
    }
}

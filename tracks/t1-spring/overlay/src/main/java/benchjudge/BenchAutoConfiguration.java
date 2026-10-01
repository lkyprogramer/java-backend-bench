package benchjudge;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import javax.sql.DataSource;

import com.sun.net.httpserver.HttpServer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import jakarta.persistence.EntityManagerFactory;

/**
 * Judge overlay, added to the classpath only by the judge. Wraps the application's DataSource
 * and, once the application is ready, serves instrumentation on a judge-only loopback port
 * (BENCH_JUDGE_PORT), independent of the candidate's web stack.
 */
@AutoConfiguration
public class BenchAutoConfiguration {

    @Bean
    static BeanPostProcessor benchDataSourceWrapper() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                return bean instanceof DataSource ds ? BenchJdbc.wrap(ds) : bean;
            }
        };
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> benchReady(ObjectProvider<DataSource> dataSource,
            ObjectProvider<EntityManagerFactory> emf) {
        return event -> {
            BenchJdbc.ARMED.set(true);
            String port = System.getenv("BENCH_JUDGE_PORT");
            if (port == null) {
                return;
            }
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", Integer.parseInt(port)), 16);
                server.createContext("/jdbc", exchange -> {
                    String body = json(Map.of(
                            "statements", BenchJdbc.STATEMENTS.get(),
                            "writeCommits", BenchJdbc.WRITE_COMMITS.get(),
                            "injected", BenchJdbc.INJECTED.get()));
                    send(exchange.getResponseBody(), exchange, body);
                });
                server.createContext("/schema", exchange -> {
                    send(exchange.getResponseBody(), exchange, schema(dataSource.getIfAvailable(), emf.getIfAvailable()));
                });
                server.start();
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("bench overlay could not start", e);
            }
        };
    }

    private static String schema(DataSource ds, EntityManagerFactory emf) {
        Object ddl = emf == null ? "none" : emf.getProperties().getOrDefault("hibernate.hbm2ddl.auto", "none");
        long migrations = -1;
        if (ds != null) {
            try (Connection c = ds.getConnection(); Statement s = c.createStatement();
                    ResultSet r = s.executeQuery("SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\" = TRUE AND \"version\" IS NOT NULL")) {
                r.next();
                migrations = r.getLong(1);
            } catch (Exception e) {
                migrations = -1;
            }
        }
        return json(Map.of("ddlAuto", String.valueOf(ddl), "flywayMigrations", migrations));
    }

    private static void send(OutputStream out, com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        out.write(bytes);
        out.close();
    }

    private static String json(Map<String, ?> map) {
        StringBuilder b = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (b.length() > 1) {
                b.append(',');
            }
            b.append('"').append(k).append("\":");
            b.append(v instanceof String s ? '"' + s.replace("\"", "") + '"' : String.valueOf(v));
        });
        return b.append('}').toString();
    }
}

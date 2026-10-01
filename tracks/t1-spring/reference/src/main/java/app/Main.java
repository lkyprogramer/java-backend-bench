package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** ReserveLedger on Spring Boot: reference implementation (private; not part of the candidate kit). */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        if (System.getenv("BENCH_MUTANT") != null) {
            // Controlled faults must show their business effect, not be stopped by V2's constraints.
            System.setProperty("spring.flyway.target", "1");
        }
        SpringApplication.run(Main.class, args);
    }
}

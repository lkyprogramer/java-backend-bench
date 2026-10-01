package app.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClockRepository extends JpaRepository<ClockState, Integer> {
}

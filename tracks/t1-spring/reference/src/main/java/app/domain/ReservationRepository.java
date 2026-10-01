package app.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findFirstByRequestIdOrderById(String requestId);

    List<Reservation> findByStateAndExpiresAtLessThanEqualOrderById(State state, long now);

    List<Reservation> findByIdGreaterThanOrderById(long after, Limit limit);

    List<Reservation> findByIdGreaterThanAndStateOrderById(long after, State state, Limit limit);

    @Query("select r.state, count(r) from Reservation r group by r.state")
    List<Object[]> countByState();
}

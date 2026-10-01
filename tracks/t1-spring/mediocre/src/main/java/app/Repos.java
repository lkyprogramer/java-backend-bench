package app;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ItemRepo extends JpaRepository<Entities.ItemEntity, String> {
}

interface ReservationRepo extends JpaRepository<Entities.ReservationEntity, Long> {
    Optional<Entities.ReservationEntity> findByRequestId(String requestId);

    List<Entities.ReservationEntity> findByStateAndExpiresAtLessThanEqual(String state, Long now);
}

interface SettingRepo extends JpaRepository<Entities.Setting, String> {
}

package com.adamekka.spins_backend.spin.repository;

import com.adamekka.spins_backend.player.model.Player;
import com.adamekka.spins_backend.spin.model.Spin;
import com.adamekka.spins_backend.spin.model.SpinType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpinRepository extends JpaRepository<Spin, Long> {
    @EntityGraph(
        attributePaths = {"tumbles", "player", "paytable", "parentSpin"}
    )
    @Query("select s from Spin s where s.id = :id")
    Optional<Spin> findByIdWithTumbles(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"player", "paytable", "paytable.symbols"})
    @Query("select s from Spin s where s.id = :id")
    Optional<Spin> findSessionByIdForUpdate(@Param("id") Long id);

    @Query(
        "select s from Spin s where s.player = :player order by s.spunAt desc"
    )
    List<Spin>
    findRecentByPlayer(@Param("player") Player player, Pageable pageable);

    @Modifying
    @Query(
        "update Spin s set s.remainingFreeSpins = 0, "
        + "s.accumulatedMultiplier = 0 where s.player = :player "
        + "and s.spinType <> :freeSpinType and s.remainingFreeSpins > 0"
    )
    int
    clearActiveFreeSpinSessions(
        @Param("player") Player player,
        @Param("freeSpinType") SpinType freeSpinType
    );
}

package com.adamekka.spins_backend.paytable.repository;

import com.adamekka.spins_backend.paytable.model.Paytable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaytableRepository extends JpaRepository<Paytable, Long> {
    @EntityGraph(attributePaths = "symbols")
    List<Paytable> findAllByOrderByIdAsc();

    Optional<Paytable> findByName(String name);

    @EntityGraph(attributePaths = "symbols")
    @Query("select p from Paytable p where p.id = :id")
    Optional<Paytable> findByIdWithSymbols(@Param("id") Long id);
}

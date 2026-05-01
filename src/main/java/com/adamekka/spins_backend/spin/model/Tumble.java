package com.adamekka.spins_backend.spin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tumbles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tumble {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spin_id", nullable = false)
    private Spin spin;

    @Column(nullable = false) private int sequenceIndex;

    // Stored as JSON so the frontend can replay a tumble without a per-cell
    // table.
    @Lob @Column(nullable = false) private String gridState;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal winAmount;

    @Column(nullable = false) private int multiplierOnGrid;

    public Tumble(
        int sequenceIndex,
        String gridState,
        BigDecimal winAmount,
        int multiplierOnGrid
    ) {
        this.sequenceIndex = sequenceIndex;
        this.gridState = gridState;
        this.winAmount = winAmount;
        this.multiplierOnGrid = multiplierOnGrid;
    }
}

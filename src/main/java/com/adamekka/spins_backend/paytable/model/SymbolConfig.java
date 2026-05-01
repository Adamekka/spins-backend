package com.adamekka.spins_backend.paytable.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "symbol_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_symbol_configs_paytable_code",
        columnNames = {"paytable_id", "code"}
    )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SymbolConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paytable_id", nullable = false)
    private Paytable paytable;

    @Column(nullable = false) private String code;

    @Column(nullable = false) private int weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SymbolType symbolType;

    @Column(precision = 10, scale = 2) private BigDecimal payoutLow;

    @Column(precision = 10, scale = 2) private BigDecimal payoutMid;

    @Column(precision = 10, scale = 2) private BigDecimal payoutHigh;

    public SymbolConfig(
        String code,
        int weight,
        SymbolType symbolType,
        BigDecimal payoutLow,
        BigDecimal payoutMid,
        BigDecimal payoutHigh
    ) {
        this.code = code;
        this.weight = weight;
        this.symbolType = symbolType;
        this.payoutLow = payoutLow;
        this.payoutMid = payoutMid;
        this.payoutHigh = payoutHigh;
    }
}

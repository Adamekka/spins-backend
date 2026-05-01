package com.adamekka.spins_backend.paytable.model;

import com.adamekka.spins_backend.spin.model.Spin;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "paytables",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_paytables_name", columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paytable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false) private String name;

    @Column(nullable = false) private int reelCount;

    @Column(nullable = false) private int rowCount;

    @Column(nullable = false) private int minMatchCount;

    @Column(nullable = false) private int scatterTriggerCount;

    @Column(nullable = false) private int freeSpinsAwarded;

    @Column(nullable = false) private int freeSpinsRetrigger;

    @Column(nullable = false) private int buyFreeSpinsMultiplier;

    @OneToMany(
        mappedBy = "paytable", cascade = CascadeType.ALL, orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<SymbolConfig> symbols = new ArrayList<>();

    @OneToMany(mappedBy = "paytable", cascade = CascadeType.ALL)
    private List<Spin> spins = new ArrayList<>();

    public Paytable(
        String name,
        int reelCount,
        int rowCount,
        int minMatchCount,
        int scatterTriggerCount,
        int freeSpinsAwarded,
        int freeSpinsRetrigger,
        int buyFreeSpinsMultiplier
    ) {
        this.name = name;
        this.reelCount = reelCount;
        this.rowCount = rowCount;
        this.minMatchCount = minMatchCount;
        this.scatterTriggerCount = scatterTriggerCount;
        this.freeSpinsAwarded = freeSpinsAwarded;
        this.freeSpinsRetrigger = freeSpinsRetrigger;
        this.buyFreeSpinsMultiplier = buyFreeSpinsMultiplier;
    }

    public void addSymbol(SymbolConfig symbol) {
        symbols.add(symbol);
        symbol.setPaytable(this);
    }
}

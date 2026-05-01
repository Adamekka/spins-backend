package com.adamekka.spins_backend.spin.model;

import com.adamekka.spins_backend.paytable.model.Paytable;
import com.adamekka.spins_backend.player.model.Player;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "spins")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paytable_id", nullable = false)
    private Paytable paytable;

    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal bet;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalWin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpinType spinType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_spin_id")
    private Spin parentSpin;

    @OneToMany(mappedBy = "parentSpin")
    private List<Spin> childSpins = new ArrayList<>();

    @Column(nullable = false) private int accumulatedMultiplier;

    @Column(nullable = false) private int remainingFreeSpins;

    @Column(nullable = false) private LocalDateTime spunAt;

    @OneToMany(
        mappedBy = "spin", cascade = CascadeType.ALL, orphanRemoval = true
    )
    @OrderBy("sequenceIndex ASC")
    private List<Tumble> tumbles = new ArrayList<>();

    public Spin(
        Player player,
        Paytable paytable,
        BigDecimal bet,
        BigDecimal totalWin,
        SpinType spinType,
        Spin parentSpin,
        int accumulatedMultiplier,
        int remainingFreeSpins,
        LocalDateTime spunAt
    ) {
        this.player = player;
        this.paytable = paytable;
        this.bet = bet;
        this.totalWin = totalWin;
        this.spinType = spinType;
        this.parentSpin = parentSpin;
        this.accumulatedMultiplier = accumulatedMultiplier;
        this.remainingFreeSpins = remainingFreeSpins;
        this.spunAt = spunAt;
    }

    public void addTumble(Tumble tumble) {
        tumbles.add(tumble);
        tumble.setSpin(this);
    }
}

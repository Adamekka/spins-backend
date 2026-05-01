package com.adamekka.spins_backend.player.model;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "players",
    uniqueConstraints
    = @UniqueConstraint(name = "uk_players_username", columnNames = "username")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false) private String username;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false) private LocalDateTime createdAt;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    @OrderBy("spunAt DESC")
    private List<Spin> spins = new ArrayList<>();

    public Player(
        String username, BigDecimal balance, LocalDateTime createdAt
    ) {
        this.username = username;
        this.balance = balance;
        this.createdAt = createdAt;
    }
}

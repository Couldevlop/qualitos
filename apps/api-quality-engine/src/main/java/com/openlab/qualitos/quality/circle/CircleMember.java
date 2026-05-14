package com.openlab.qualitos.quality.circle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "circle_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_circle_member_user",
                columnNames = {"circle_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CircleMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false, updatable = false)
    private QualityCircle circle;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CircleRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    void prePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = Instant.now();
        }
        if (this.role == null) {
            this.role = CircleRole.MEMBER;
        }
    }
}

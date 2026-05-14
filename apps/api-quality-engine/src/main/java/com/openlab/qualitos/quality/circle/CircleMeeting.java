package com.openlab.qualitos.quality.circle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "circle_meetings")
@Getter
@Setter
@NoArgsConstructor
public class CircleMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false, updatable = false)
    private QualityCircle circle;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String agenda;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /** Salle physique ou URL Jitsi/Teams/Meet. */
    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingStatus status;

    /** Compte-rendu (rédigé après la tenue, ou auto-généré par LLM). */
    @Column(columnDefinition = "TEXT")
    private String minutes;

    @Column(name = "held_at")
    private Instant heldAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = MeetingStatus.PLANNED;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

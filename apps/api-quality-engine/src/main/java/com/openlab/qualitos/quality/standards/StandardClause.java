package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "standard_clauses",
        uniqueConstraints = @UniqueConstraint(name = "uk_clause_section_code",
                columnNames = {"section_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class StandardClause {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false, updatable = false)
    private StandardSection section;

    /** Code de clause (ex: "4.1", "8.5.1"). */
    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @OneToMany(mappedBy = "clause", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<StandardRequirement> requirements = new ArrayList<>();
}

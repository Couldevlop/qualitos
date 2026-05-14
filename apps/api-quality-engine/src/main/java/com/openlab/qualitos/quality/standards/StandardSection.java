package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "standard_sections",
        uniqueConstraints = @UniqueConstraint(name = "uk_section_standard_code",
                columnNames = {"standard_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class StandardSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id", nullable = false, updatable = false)
    private Standard standard;

    /** Code de section (ex: "4", "5"). */
    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<StandardClause> clauses = new ArrayList<>();
}

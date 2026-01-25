package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "test_definition")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testName;

    private String shortCode;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Department department;

    private String unit;

    private BigDecimal minRange;
    private BigDecimal maxRange;
    private BigDecimal price;

    private Boolean active = true;

    // Many-to-many with Panel
    @ManyToMany(mappedBy = "tests")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Panel> panels;

    // One-to-many with ReferenceRange
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ReferenceRange> ranges;
}

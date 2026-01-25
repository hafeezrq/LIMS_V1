package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "panel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Panel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String panelName;

    @ManyToOne
    @JoinColumn(name = "department_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Department department;

    private Boolean active = true;

    // Many-to-many relationship with tests
    @ManyToMany
    @JoinTable(name = "panel_test", joinColumns = @JoinColumn(name = "panel_id"), inverseJoinColumns = @JoinColumn(name = "test_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<TestDefinition> tests;
}

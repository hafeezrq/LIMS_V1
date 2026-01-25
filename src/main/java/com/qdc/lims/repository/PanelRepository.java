package com.qdc.lims.repository;

import com.qdc.lims.entity.Panel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository for {@link Panel} entities.
 */
public interface PanelRepository extends JpaRepository<Panel, Integer> {

    /**
     * Loads active panels and eagerly fetches their tests to avoid N+1 lookups in
     * the UI.
     *
     * @return active panels with their tests preloaded
     */
    @Query("SELECT DISTINCT p FROM Panel p LEFT JOIN FETCH p.tests WHERE p.active = true")
    List<Panel> findAllWithTests();
}

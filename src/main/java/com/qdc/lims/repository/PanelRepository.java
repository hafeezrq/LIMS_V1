package com.qdc.lims.repository;

import com.qdc.lims.entity.Panel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PanelRepository extends JpaRepository<Panel, Integer> {
    @Query("SELECT DISTINCT p FROM Panel p LEFT JOIN FETCH p.tests WHERE p.active = true")
    List<Panel> findAllWithTests();
}
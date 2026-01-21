package com.qdc.lims.service;

import com.qdc.lims.entity.TestDefinition;
import com.qdc.lims.repository.TestDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TestDefinitionService {

    @Autowired
    private TestDefinitionRepository testDefinitionRepository;

    public List<TestDefinition> findAll() {
        return testDefinitionRepository.findAll();
    }

    public Optional<TestDefinition> findById(Long id) {
        return testDefinitionRepository.findById(id);
    }

    public TestDefinition save(TestDefinition test) {
        return testDefinitionRepository.save(test);
    }

    public void deleteById(Long id) {
        testDefinitionRepository.deleteById(id);
    }

    public List<TestDefinition> searchTests(String query) {
        // Basic search implementation, relying on repository method if available or
        // filtering via stream if not.
        // Ideally we would add a search method to the repository.
        // For now, let's just return all and filter in memory or assume similar logic.
        // But let's check if repository has a search method or just use findAll for
        // now.
        return testDefinitionRepository.findAll().stream()
                .filter(t -> t.getTestName().toLowerCase().contains(query.toLowerCase()) ||
                        (t.getShortCode() != null && t.getShortCode().toLowerCase().contains(query.toLowerCase())))
                .toList();
    }
}

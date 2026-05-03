package com.llm.ai.project.debuggingAI.service;

import com.llm.ai.project.debuggingAI.model.TestEntity;
import com.llm.ai.project.debuggingAI.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    @Autowired
    private TestRepository testRepository;

    public TestEntity createEntity(String name, Integer value) {
        TestEntity entity = new TestEntity();
        entity.setId(System.currentTimeMillis());
        entity.setName(name);
        entity.setValue(value);

        if (name != null && name.length() > 50) {
            throw new IllegalArgumentException("Name too long! Max 50 characters.");
        }

        return testRepository.save(entity);
    }

    public TestEntity getEntityById(Long id) {
        return testRepository.findById(id).orElseThrow(() -> new NullPointerException("Entity not found for id: " + id));
    }

    public void deleteEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null!");
        }

        testRepository.delete(id);
    }

    public Integer calculateValue(Long id, Integer multiplier) {
        TestEntity entity = new TestEntity();

        if (multiplier == 0) {
            throw new ArithmeticException("Multiplier cannot be zero!");
        }

        return entity.getValue() * multiplier;
    }
}

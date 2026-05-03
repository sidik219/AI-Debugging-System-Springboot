package com.llm.ai.project.debuggingAI.repository;

import com.llm.ai.project.debuggingAI.model.TestEntity;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class TestRepository {

    private final Map<Long, TestEntity> db = new HashMap<>();

    public TestRepository() {
        // Buat Data Dummy
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setName("Test");
        entity.setValue(100);

        db.put(1L, entity);
    }

    public Optional<TestEntity> findById(Long id) {
        if (id > 100) {
            throw new RuntimeException("Database connection timeout for id: " + id);
        }

        return Optional.ofNullable(db.get(id));
    }

    public TestEntity save(TestEntity entity) {
        if (entity.getName() == null || entity.getName().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty!");
        }

        if (entity.getValue() < 0) {
            throw new ArithmeticException("Value cannot be negative: " + entity.getValue());
        }

        db.put(entity.getId(), entity);
        return entity;
    }

    public void delete(Long id) {
        if (!db.containsKey(id)) {
            throw new NullPointerException("Entity not found with id: " + id);
        }

        db.remove(id);
    }
}

package com.loenan.tools.jpa.entityexplorer;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class EntitySchema {

    private final Map<Class<?>, Entity> entityMap = new HashMap<>();

    public EntitySchema(Collection<Class<?>> entities) {
        entities.forEach(this::addEntity);
        resolveMappedProperties();
        detectCycles();
        resolveRelationships();
    }

    public Collection<Entity> getEntities() {
        return entityMap.values();
    }

    private Entity addEntity(Class<?> entityClass) {
        if (entityClass == null || entityClass.equals(Object.class)) {
            return null;
        }
        Entity entity = entityMap.get(entityClass);
        if (entity != null) {
            return entity;
        }
        Entity parent = addEntity(entityClass.getSuperclass());
        entity = new Entity(parent, entityClass);
        entityMap.put(entityClass, entity);
        return entity;
    }

    private void resolveMappedProperties() {
        entityMap.values().stream()
            .flatMap(entity -> entity.getProperties().stream())
            .forEach(this::findAndSetMappedProperty);
    }

    private void findAndSetMappedProperty(Property property) {
        Optional.ofNullable(property.getMappedBy())
            .flatMap(mappedBy -> Optional.ofNullable(entityMap.get(property.getType()))
                .map(targetEntity -> targetEntity.getPropertyByName(mappedBy)))
            .ifPresent(property::setMappedByProperty);
    }

    private void detectCycles() {
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> stack = new ArrayDeque<>();
        for (Entity entity : entityMap.values()) {
            detectCycles(entity, visited, stack);
        }
    }

    private boolean detectCycles(Entity entity, Set<Class<?>> visited, Deque<Class<?>> stack) {
        Class<?> entityClass = entity.getEntityClass();
        if (stack.contains(entityClass)) {
            return true; // cycle detected
        }
        if (visited.contains(entityClass)) {
            return false;
        }
        visited.add(entityClass);
        stack.addLast(entityClass);
        for (Property outgoingRelationship : entity.getOutgoingRelationships()) {
            Entity targetEntity = entityMap.get(outgoingRelationship.getType());
            if (targetEntity != null && detectCycles(targetEntity, visited, stack)) {
                // fix the cycle by marking the relationship to be ignored
                outgoingRelationship.ignoreRelationship();
            }
        }
        stack.removeLast();
        return false;
    }

    private void resolveRelationships() {
        entityMap.values().stream()
            .flatMap(entity -> entity.getOutgoingRelationships().stream())
            .forEach(property -> Optional.ofNullable(entityMap.get(property.getType()))
                .ifPresent(targetEntity -> {
                    property.setTargetEntity(targetEntity);
                    targetEntity.getIncomingRelationships().add(property);
                }));
    }
}

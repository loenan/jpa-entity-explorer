package com.loenan.tools.jpa.entityexplorer;

import javax.persistence.Transient;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class Entity {
    private final Entity parent;
    private final List<Entity> children = new ArrayList<>();
    private final Class<?> entityClass;
    private final String name;
    private final String packageName;
    private final Map<String, Property> properties = new LinkedHashMap<>();
    private final List<Property> incomingRelationships = new ArrayList<>();

    public Entity(Entity parent, Class<?> entityClass) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.children.add(this);
        }
        this.entityClass = entityClass;
        name = entityClass.getSimpleName();
        packageName = entityClass.getPackage().getName();

        stream(entityClass.getDeclaredFields())
            .map(field -> new Property(this, field))
            .filter(property -> property.isNot(Modifier.STATIC, Modifier.FINAL, Modifier.TRANSIENT))
            .filter(property -> property.isNot(Transient.class))
            .forEach(property -> properties.put(property.getName(), property));
    }

    public Entity getParent() {
        return parent;
    }

    public boolean isChild() {
        return parent != null;
    }

    public List<Entity> getChildren() {
        return children;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public Collection<Property> getProperties() {
        return properties.values();
    }

    public Property getPropertyByName(String name) {
        return properties.get(name);
    }

    public List<Property> getOutgoingRelationships() {
        return getOutgoingRelationshipStream()
            .collect(toList());
    }

    private Stream<Property> getOutgoingRelationshipStream() {
        return properties.values().stream()
            .filter(Property::isOutgoingRelationship);
    }

    public List<Property> getIncomingRelationships() {
        return incomingRelationships;
    }

    public boolean isSingleSubEntity() {
        return getIncomingRelationships().size() == 1;
    }

    public int getSubGraphSize() {
        Set<Class<?>> subGraphClasses = new HashSet<>();
        Queue<Entity> queue = new ArrayDeque<>();
        queue.add(this);
        Entity current;
        while ((current = queue.poll()) != null) {
            if (subGraphClasses.add(current.getEntityClass())) {
                queue.addAll(current.getOutgoingRelationshipStream()
                    .map(Property::getTargetEntity)
                    .filter(Objects::nonNull)
                    .collect(toList()));
                Optional.ofNullable(current.getParent())
                    .ifPresent(queue::add);
            }
        }
        return subGraphClasses.size();
    }

    @Override
    public String toString() {
        return getName()
            + (getParent() == null ? "" : ": " + getParent().getName())
            + buildAnnotationsString();
    }

    private String buildAnnotationsString() {
        List<String> annotations = Util.getAnnotations(entityClass);
        return annotations.isEmpty() ? "" : annotations.stream().collect(joining(", ", " (", ")"));
    }}

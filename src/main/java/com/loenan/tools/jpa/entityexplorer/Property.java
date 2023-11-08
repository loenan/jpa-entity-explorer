package com.loenan.tools.jpa.entityexplorer;

import org.apache.commons.lang3.reflect.TypeUtils;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class Property {
    private final Entity entity;
    private final Field field;
    private final String name;
    private final Class<?> type;
    private Property mappedByProperty;
    private Property mappingProperty;
    private Entity targetEntity;
    private boolean ignoreRelationship;

    public Property(Entity entity, Field field) {
        this.entity = entity;
        this.field = field;
        name = field.getName();
        type = field.getType();
    }

    public void setMappedByProperty(Property mappedByProperty) {
        this.mappedByProperty = mappedByProperty;
        this.mappedByProperty.mappingProperty = this;
    }

    public void setTargetEntity(Entity targetEntity) {
        this.targetEntity = targetEntity;
    }

    public void ignoreRelationship() {
        ignoreRelationship = true;
    }

    public Entity getEntity() {
        return entity;
    }

    public Field getField() {
        return field;
    }

    public String getName() {
        return name;
    }

    public String getCollectionType() {
        if (isCollection()) {
            return type.isArray() ? "array" : type.getSimpleName();
        }
        return null;
    }

    public Class<?> getType() {
        if (isCollection()) {
            if (type.isArray()) {
                return type.getComponentType();
            }
            if (field.getGenericType() instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                return TypeUtils.getRawType(typeArguments[typeArguments.length - 1], null);
            }
        }
        return type;
    }

    public boolean isCollection() {
        return Collection.class.isAssignableFrom(type)
            || Map.class.isAssignableFrom(type)
            || type.isArray();
    }

    public String getTypeName() {
        return getType().getSimpleName();
    }

    public boolean isRelationship() {
        return isAny(OneToOne.class, OneToMany.class, ManyToOne.class, ManyToMany.class);
    }

    public boolean isOutgoingRelationship() {
        return isRelationship() && getMappingProperty() == null && !ignoreRelationship;
    }

    public String getMappedBy() {
        return Stream.of(OneToOne.class, OneToMany.class, ManyToOne.class, ManyToMany.class)
            .map(field::getAnnotation)
            .map(annotation -> {
                if (annotation instanceof OneToOne) {
                    return ((OneToOne) annotation).mappedBy();
                } else if (annotation instanceof OneToMany) {
                    return ((OneToMany) annotation).mappedBy();
                } else if (annotation instanceof ManyToMany) {
                    return ((ManyToMany) annotation).mappedBy();
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
    }

    public Property getMappedByProperty() {
        return mappedByProperty;
    }

    public Property getMappingProperty() {
        return mappingProperty;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public boolean isNot(int... modifiers) {
        return IntStream.of(modifiers).allMatch(modifier -> (field.getModifiers() & modifier) == 0);
    }

    public boolean isNot(Class<? extends Annotation>... annotationClasses) {
        return stream(annotationClasses).noneMatch(field::isAnnotationPresent);
    }

    public boolean isAny(Class<? extends Annotation>... annotationClasses) {
        return stream(annotationClasses).anyMatch(field::isAnnotationPresent);
    }

    @Override
    public String toString() {
        if (isRelationship()) {
            if (getMappingProperty() != null) {
                return "<--[" + getName() + "]-- " + buildCollectionTypeString() + getTypeName() + buildAnnotationsString();
            }
            return "--[" + getName() + "]--> " + buildCollectionTypeString() + getTypeName() + buildAnnotationsString();
        }
        return getName() + ": " + buildCollectionTypeString() + getTypeName() + buildAnnotationsString();
    }

    private String buildCollectionTypeString() {
        return isCollection() ? "<" + getCollectionType() + "> " : "";
    }

    private String buildAnnotationsString() {
        List<String> annotations = Util.getAnnotations(field);
        return annotations.isEmpty() ? "" : annotations.stream().collect(joining(", ", " (", ")"));
    }
}

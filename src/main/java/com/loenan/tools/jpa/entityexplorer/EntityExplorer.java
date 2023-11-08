package com.loenan.tools.jpa.entityexplorer;

import org.reflections.Reflections;

import java.util.Set;

import static java.util.Comparator.comparing;

public class EntityExplorer {

    private static final String INDENT = "    ";

    public static void main(String[] args) {
        explore(args);
    }

    public static void explore(Object... params) {
        Reflections reflections = new Reflections(params);
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(javax.persistence.Entity.class);

        EntitySchema schema = new EntitySchema(entityClasses);

        schema.getEntities().stream()
            .filter(entity -> ! entity.isSingleSubEntity())
            .filter(entity -> ! entity.isChild())
            .sorted(comparing(Entity::getSubGraphSize).reversed())
            .forEach(entity -> printEntity("", entity));
    }

    private static void printEntity(String indentation, Entity entity) {
        String prefix = entity.isChild() ? "\\\\ " : "## ";
        System.out.println(indentation + prefix + entity);
        if (! entity.isSingleSubEntity()) {
            for (Property incomingRelationship : entity.getIncomingRelationships()) {
                System.out.println(indentation + INDENT + "...[" + incomingRelationship.getName() + "]... " + incomingRelationship.getEntity().getName());
            }
        }
        for (Property outgoingRelationship : entity.getOutgoingRelationships()) {
            System.out.println(indentation + INDENT + outgoingRelationship);
            Entity targetEntity = outgoingRelationship.getTargetEntity();
            if (targetEntity != null && targetEntity.isSingleSubEntity()) {
                printEntity(indentation + INDENT + INDENT, targetEntity);
            } else {
                System.out.println(indentation + INDENT + INDENT + "...");
            }
        }
        entity.getProperties().stream()
            .filter(property -> ! property.isOutgoingRelationship())
            .forEach(property -> System.out.println(indentation + INDENT + property));
        for (Entity child : entity.getChildren()) {
            printEntity(indentation, child);
        }
    }
}

package com.loenan.tools.jpa.entityexplorer;

import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class Util {

    private static final Set<Class<? extends Annotation>> ANNOTATIONS_TO_EXCLUDE = new HashSet<>(asList(
        NamedEntityGraph.class,
        NamedEntityGraphs.class,
        NamedNativeQueries.class,
        NamedNativeQuery.class,
        NamedQueries.class,
        NamedQuery.class,
        NamedStoredProcedureQueries.class,
        NamedStoredProcedureQuery.class
    ));

    public static List<String> getAnnotationTypes(AnnotatedElement annotatedElement) {
        return stream(annotatedElement.getAnnotations())
            .map(Annotation::annotationType)
            .filter(Util::isPersistenceAnnotation)
            .map(Class::getSimpleName)
            .collect(toList());
    }

    public static List<String> getAnnotations(AnnotatedElement annotatedElement) {
        return stream(annotatedElement.getAnnotations())
            .filter(annotation -> isPersistenceAnnotation(annotation.annotationType()))
            .map(Util::toString)
            .sorted()
            .collect(toList());
    }

    private static boolean isPersistenceAnnotation(Class<? extends Annotation> annotationType) {
        return annotationType.getPackage().getName().startsWith("javax.persistence")
            && ! ANNOTATIONS_TO_EXCLUDE.contains(annotationType);
    }

    private static <A extends Annotation> String toString(A annotation) {
        Class<? extends Annotation> type = annotation.annotationType();
        String attributes = stream(type.getDeclaredMethods())
            .map(method -> toString(annotation, method))
            .filter(Objects::nonNull)
            .collect(joining(", ", "(", ")"));
        return "@" + type.getSimpleName() + (attributes.equals("()") ? "" : attributes);
    }

    private static <A extends Annotation> String toString(A annotation, Method method) {
        try {
            String name = method.getName();
            Object value = method.invoke(annotation);
            if (isEmpty(value) || Objects.equals(method.getDefaultValue(), value)) {
                return null;
            }
            if (method.getReturnType().equals(boolean.class)) {
                return (Objects.equals(Boolean.TRUE, value) ? "" : "!") + name;
            }
            if (name.equals("value") || name.equals("name")) {
                return toString(value);
            }
            return name + "=" + toString(value);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static String toString(Object value) {

        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof Annotation) {
            return toString((Annotation) value);
        }
        if (value.getClass().isArray()) {
            return stream(((Object[]) value)).map(Util::toString).collect(joining(",", "[", "]"));
        }
        return Objects.toString(value);
    }

    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return ((Object[]) value).length == 0;
        }
        return false;
    }
}

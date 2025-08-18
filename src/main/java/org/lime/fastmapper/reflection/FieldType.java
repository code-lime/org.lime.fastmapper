package org.lime.fastmapper.reflection;

import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Objects;

public record FieldType(
        Type declaringType,
        Field field,
        Type fieldType) {
    public static FieldType of(Type declaringType, Field field) {
        var typeArguments = TypeUtils.getTypeArguments(declaringType, field.getDeclaringClass());

        Type fieldType = Objects.requireNonNull(TypeUtils.unrollVariables(typeArguments, field.getGenericType()));
        return new FieldType(declaringType, field, fieldType);
    }
}

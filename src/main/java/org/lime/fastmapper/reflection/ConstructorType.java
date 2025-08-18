package org.lime.fastmapper.reflection;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record ConstructorType(
        Type declaringType,
        Constructor<?> constructor,
        @Unmodifiable List<Type> arguments) {
    public static ConstructorType of(Type declaringType, Constructor<?> constructor) {
        var typeArguments = GenericUtils.getExecutableTypeArguments(declaringType, constructor);

        List<Type> arguments = Arrays.stream(constructor.getGenericParameterTypes())
                .map(v -> Objects.requireNonNull(TypeUtils.unrollVariables(typeArguments, v)))
                .toList();
        return new ConstructorType(declaringType, constructor, arguments);
    }
}

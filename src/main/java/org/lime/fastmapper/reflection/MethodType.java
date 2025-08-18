package org.lime.fastmapper.reflection;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MethodType(
        Type declaringType,
        Method method,
        Type returnType,
        @Unmodifiable List<Type> arguments) {
    public static MethodType of(Type declaringType, Method method) {
        Map<TypeVariable<?>, Type> typeArguments = GenericUtils.getExecutableTypeArguments(declaringType, method);

        Type returnType = Objects.requireNonNull(TypeUtils.unrollVariables(typeArguments, method.getGenericReturnType()));
        List<Type> arguments = Arrays.stream(method.getGenericParameterTypes())
                .map(v -> Objects.requireNonNull(TypeUtils.unrollVariables(typeArguments, v)))
                .toList();
        return new MethodType(declaringType, method, returnType, arguments);
    }
}

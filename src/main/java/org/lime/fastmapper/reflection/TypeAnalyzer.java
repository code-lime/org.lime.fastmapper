package org.lime.fastmapper.reflection;

import org.lime.core.common.system.Lazy;

import java.lang.reflect.Type;
import java.util.List;

public class TypeAnalyzer {
    private final Type declaringType;
    private final Class<?> rawClass;
    private final Lazy<List<ConstructorType>> constructors;
    private final Lazy<List<FieldType>> fields;
    private final Lazy<List<MethodType>> methods;

    public TypeAnalyzer(Type declaringType) {
        this.declaringType = declaringType;
        this.rawClass = GenericUtils.readRawClass(declaringType);

        this.constructors = Lazy.of(() -> GenericUtils.getAllConstructors(rawClass)
                .stream()
                .map(v -> ConstructorType.of(declaringType, v))
                .toList());
        this.fields = Lazy.of(() -> GenericUtils.getAllFields(rawClass)
                .stream()
                .map(v -> FieldType.of(declaringType, v))
                .toList());
        this.methods = Lazy.of(() -> GenericUtils.getAllMethods(rawClass)
                .stream()
                .map(v -> MethodType.of(declaringType, v))
                .toList());
    }

    public Type declaringType() {
        return declaringType;
    }
    public Class<?> rawClass() {
        return rawClass;
    }

    public List<ConstructorType> constructors() {
        return constructors.value();
    }
    public List<FieldType> fields() {
        return fields.value();
    }
    public List<MethodType> methods() {
        return methods.value();
    }

    public static TypeAnalyzer of(Type declaringType) {
        return new TypeAnalyzer(declaringType);
    }
}

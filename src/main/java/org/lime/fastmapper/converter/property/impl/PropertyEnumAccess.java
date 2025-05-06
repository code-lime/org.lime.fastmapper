package org.lime.fastmapper.converter.property.impl;

import org.lime.core.common.system.Lazy;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyAccess;
import org.lime.fastmapper.converter.property.PropertyContent;

import java.util.stream.Stream;

public class PropertyEnumAccess<T extends Enum<T>>
        implements PropertyAccess<T> {
    private final Class<T> tClass;
    public PropertyEnumAccess(Class<T> tClass) {
        this.tClass = tClass;
    }

    private static final String Key = "enum#value";

    @Override
    public Class<T> accessClass() {
        return tClass;
    }
    @Override
    public Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
        return Stream.of(new PropertyContent<>(Key, String.class, String.class, Lazy.of(value.name())));
    }

    @Override
    public T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
        return properties.filter(v -> v.name().equals(Key))
                .findAny()
                .map(v -> Enum.valueOf(tClass, (String) v.value()))
                .orElseThrow(() -> new IllegalArgumentException("Property is not enum"));
    }
}

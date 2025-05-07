package org.lime.fastmapper.converter.property;

import org.lime.core.common.system.Lazy;
import org.lime.core.common.system.execute.Func1;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class PropertyContent<T> {
    private final String name;
    private final Type genType;
    private final Class<T> type;
    private final Lazy<T> value;

    public PropertyContent(String name, Type genType, Class<T> type, Lazy<T> value) {
        this.name = name;
        this.genType = genType;
        this.type = type;
        this.value = value;
    }

    public String name() {
        return name;
    }
    public T value() {
        return value.value();
    }

    public Type genType() {
        return genType;
    }
    public Class<T> type() {
        return type;
    }

    public <J>PropertyContent<J> map(ParameterizedType genType, Func1<T, J> map) {
        return new PropertyContent<>(name, genType, (Class<J>) genType.getRawType(), Lazy.of(() -> map.invoke(value())));
    }
    public PropertyContent<T> update(Func1<T, T> map) {
        return new PropertyContent<>(name, genType, type, Lazy.of(() -> map.invoke(value())));
    }
    public PropertyContent<T> rename(String name) {
        return new PropertyContent<>(name, genType, type, value);
    }
}

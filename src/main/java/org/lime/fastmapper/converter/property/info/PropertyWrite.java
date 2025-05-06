package org.lime.fastmapper.converter.property.info;

import org.lime.core.common.system.execute.Action2;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

public record PropertyWrite<T, E extends Member>(
        String name,
        boolean prefix,
        E member,
        Action2<Object, T> write,
        boolean optional,
        Type genType,
        Class<T> type) implements PropertyInfo.Write<T, E> {
    @Override
    public void writeTo(Object target, T value) {
        write().invoke(target, value);
    }
}

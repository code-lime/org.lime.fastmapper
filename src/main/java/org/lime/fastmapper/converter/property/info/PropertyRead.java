package org.lime.fastmapper.converter.property.info;

import org.lime.core.common.system.Lazy;
import org.lime.core.common.system.execute.Func1;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Optional;

public record PropertyRead<T, E extends Member>(
        String name,
        boolean prefix,
        E member,
        Func1<Object, Optional<Lazy<T>>> reader,
        boolean optionalType,
        Type genType,
        Class<T> type) implements PropertyInfo.Read<T, E> {
    @Override
    public Optional<Lazy<T>> getFrom(Object target) {
        return reader.invoke(target);
    }
}

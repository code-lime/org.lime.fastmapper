package org.lime.fastmapper.converter.property.info;

import org.lime.system.Lazy;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Optional;

public interface PropertyInfo<T, E extends Member> {
    String name();

    boolean getter();
    boolean prefix();

    E member();
    Type genType();
    Class<T> type();

    record Impl<T, E extends Member>(
            String name,
            boolean getter,
            boolean prefix,
            E member,
            Type genType,
            Class<T> type)
            implements PropertyInfo<T, E> { }

    interface Write<T, E extends Member> extends PropertyInfo<T, E> {
        boolean optional();
        void writeTo(Object target, T value);

        @Override
        default boolean getter() {
            return false;
        }
    }
    interface Read<T, E extends Member> extends PropertyInfo<T, E> {
        boolean optionalType();
        Optional<Lazy<T>> getFrom(Object target);

        @Override
        default boolean getter() {
            return true;
        }
    }
}

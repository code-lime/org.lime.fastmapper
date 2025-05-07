package org.lime.fastmapper.converter.property.info;

import org.lime.core.common.system.Lazy;

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

        default Write<T,E> mapOptional(boolean optional) {
            return new Shadow<>(this) {
                @Override
                public boolean optional() {
                    return optional;
                }
            };
        }

        class Shadow<T,E extends Member>
                implements Write<T,E> {
            private final Write<T,E> owner;
            public Shadow(Write<T,E> owner) {
                this.owner = owner;
            }
            @Override
            public boolean optional() {
                return owner.optional();
            }
            @Override
            public void writeTo(Object target, T value) {
                owner.writeTo(target, value);
            }
            @Override
            public String name() {
                return owner.name();
            }
            @Override
            public boolean prefix() {
                return owner.prefix();
            }
            @Override
            public E member() {
                return owner.member();
            }
            @Override
            public Type genType() {
                return owner.genType();
            }
            @Override
            public Class<T> type() {
                return owner.type();
            }
            @Override
            public boolean getter() {
                return owner.getter();
            }
        }
    }
    interface Read<T, E extends Member> extends PropertyInfo<T, E> {
        boolean optionalType();
        Optional<Lazy<T>> getFrom(Object target);

        @Override
        default boolean getter() {
            return true;
        }

        default Read<T,E> mapOptionalType(boolean optionalType) {
            return new Shadow<>(this) {
                @Override
                public boolean optionalType() {
                    return optionalType;
                }
            };
        }

        class Shadow<T,E extends Member>
                implements Read<T,E> {
            private final Read<T,E> owner;
            public Shadow(Read<T,E> owner) {
                this.owner = owner;
            }
            @Override
            public boolean optionalType() {
                return owner.optionalType();
            }
            @Override
            public Optional<Lazy<T>> getFrom(Object target) {
                return owner.getFrom(target);
            }
            @Override
            public String name() {
                return owner.name();
            }
            @Override
            public boolean prefix() {
                return owner.prefix();
            }
            @Override
            public E member() {
                return owner.member();
            }
            @Override
            public Type genType() {
                return owner.genType();
            }
            @Override
            public Class<T> type() {
                return owner.type();
            }
            @Override
            public boolean getter() {
                return owner.getter();
            }
        }
    }
}

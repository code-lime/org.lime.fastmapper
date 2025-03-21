package org.lime.fastmapper.converter.property;

import org.lime.fastmapper.converter.property.info.PropertyInfo;

import java.lang.reflect.Member;
import java.util.stream.Stream;

public interface PropertyAccessOptions<A extends PropertyAccessOptions<A, T, E>, T, E extends Member>
        extends PropertyAccess<T> {
    boolean isPrefixOnly();

    Stream<PropertyInfo.Read<?, E>> reads();
    Stream<PropertyInfo.Write<?, E>> writes();
    default Stream<PropertyInfo<?, E>> properties() {
        return Stream.concat(reads(), writes());
    }

    A withPrefixOnly(boolean enable);
    default A withPrefixAuto() {
        return withPrefixOnly(properties().anyMatch(PropertyInfo::prefix));
    }

    default <I>boolean filterProperty(PropertyInfo.Read<I, E> property) {
        return !isPrefixOnly() || property.prefix();
    }
    default <I>boolean filterProperty(PropertyInfo.Write<I, E> property) {
        return !isPrefixOnly() || property.prefix();
    }
}

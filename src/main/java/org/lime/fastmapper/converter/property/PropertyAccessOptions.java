package org.lime.fastmapper.converter.property;

import com.google.common.collect.ImmutableList;
import org.lime.core.common.reflection.LambdaInfo;
import org.lime.core.common.system.execute.Callable;
import org.lime.core.common.system.execute.Func1;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.info.PropertyContents;
import org.lime.fastmapper.converter.property.info.PropertyInfo;
import org.lime.fastmapper.converter.property.info.PropertyLoader;
import org.lime.fastmapper.reflection.MethodType;

import java.lang.reflect.Member;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public interface PropertyAccessOptions<T, E extends Member>
        extends PropertyAccess<T> {
    Stream<PropertyInfo.Read<?, E>> readProperties();
    Stream<PropertyInfo.Write<?, E>> writeProperties();

    record Modify<T, E extends Member>(
            PropertyAccessOptions<T,E> root,
            Optional<Boolean> prefixOnly,
            ImmutableList<Func1<Stream<PropertyInfo.Read<?, E>>, Stream<PropertyInfo.Read<?, E>>>> modifyReadProperties,
            ImmutableList<Func1<Stream<PropertyInfo.Write<?, E>>, Stream<PropertyInfo.Write<?, E>>>> modifyWriteProperties)
            implements PropertyAccessOptions<T,E> {
        @Override
        public boolean isPrefixOnly() {
            return prefixOnly.orElseGet(root::isPrefixOnly);
        }

        @Override
        public Stream<PropertyInfo.Read<?, E>> readProperties() {
            Stream<PropertyInfo.Read<?, E>> reads = root.readProperties();
            if (isPrefixOnly())
                reads = reads.filter(PropertyInfo::prefix);
            for (var modify : modifyReadProperties)
                reads = modify.invoke(reads);
            return reads;
        }
        @Override
        public Stream<PropertyInfo.Write<?, E>> writeProperties() {
            Stream<PropertyInfo.Write<?, E>> writes = root.writeProperties();
            if (isPrefixOnly())
                writes = writes.filter(PropertyInfo::prefix);
            for (var modify : modifyWriteProperties)
                writes = modify.invoke(writes);
            return writes;
        }

        @Override
        public Class<T> accessClass() {
            return root.accessClass();
        }

        @Override
        public Stream<PropertyContent<?>> readOf(FastMapper mapper, T value, Stream<PropertyInfo.Read<?, E>> readProperties) {
            return root.readOf(mapper, value, readProperties);
        }
        @Override
        public T writeOf(FastMapper mapper, Stream<PropertyContent<?>> properties, Stream<PropertyInfo.Write<?, E>> writeProperties) {
            return root.writeOf(mapper, properties, writeProperties);
        }
    }

    default Stream<PropertyInfo<?, E>> properties() {
        return Stream.concat(readProperties(), writeProperties());
    }

    default boolean isPrefixOnly() {
        return false;
    }
    default PropertyAccessOptions<T,E> withPrefixOnly(boolean prefixOnly) {
        return isPrefixOnly() == prefixOnly
                ? this
                : new Modify<>(this, Optional.of(prefixOnly), ImmutableList.of(), ImmutableList.of());
    }
    default PropertyAccessOptions<T,E> withPrefixAuto() {
        return withPrefixOnly(properties().anyMatch(PropertyInfo::prefix));
    }

    default PropertyAccessOptions<T,E> modifyReadProperties(Callable method, Func1<PropertyInfo.Read<?, E>, PropertyInfo.Read<?, E>> modify) {
        return PropertyLoader.extractInfo(MethodType.of(accessClass(), LambdaInfo.getMethod(method)))
                .map(PropertyInfo::name)
                .map(name -> modifyReadProperties(name, modify))
                .orElseThrow(() -> new IllegalArgumentException("Property from '"+modify+"' not found"));
    }
    default PropertyAccessOptions<T,E> modifyReadProperties(String name, Func1<PropertyInfo.Read<?, E>, PropertyInfo.Read<?, E>> modify) {
        return modifyReadProperties(stream -> stream
                .map(v -> v.name().equals(name) ? modify.invoke(v) : v));
    }
    default PropertyAccessOptions<T,E> modifyReadProperties(Func1<Stream<PropertyInfo.Read<?, E>>, Stream<PropertyInfo.Read<?, E>>> modify) {
        return this instanceof Modify<T,E> modifyAccess
                ? new Modify<>(
                modifyAccess.root(),
                modifyAccess.prefixOnly(),
                ImmutableList.<Func1<Stream<PropertyInfo.Read<?, E>>, Stream<PropertyInfo.Read<?, E>>>>builder()
                        .addAll(modifyAccess.modifyReadProperties())
                        .add(modify)
                        .build(),
                modifyAccess.modifyWriteProperties())
                : new Modify<>(this, Optional.empty(), ImmutableList.of(modify), ImmutableList.of());
    }

    default PropertyAccessOptions<T,E> modifyWriteProperties(Callable method, Func1<PropertyInfo.Write<?, E>, PropertyInfo.Write<?, E>> modify) {
        return PropertyLoader.extractInfo(MethodType.of(accessClass(), LambdaInfo.getMethod(method)))
                .map(PropertyInfo::name)
                .map(name -> modifyWriteProperties(name, modify))
                .orElseThrow(() -> new IllegalArgumentException("Property from '"+modify+"' not found"));
    }
    default PropertyAccessOptions<T,E> modifyWriteProperties(String name, Func1<PropertyInfo.Write<?, E>, PropertyInfo.Write<?, E>> modify) {
        return modifyWriteProperties(stream -> stream
                .map(v -> v.name().equals(name) ? modify.invoke(v) : v));
    }
    default PropertyAccessOptions<T,E> modifyWriteProperties(Func1<Stream<PropertyInfo.Write<?, E>>, Stream<PropertyInfo.Write<?, E>>> modify) {
        return this instanceof Modify<T,E> modifyAccess
                ? new Modify<>(
                modifyAccess.root(),
                modifyAccess.prefixOnly(),
                modifyAccess.modifyReadProperties(),
                ImmutableList.<Func1<Stream<PropertyInfo.Write<?, E>>, Stream<PropertyInfo.Write<?, E>>>>builder()
                        .addAll(modifyAccess.modifyWriteProperties())
                        .add(modify)
                        .build())
                : new Modify<>(this, Optional.empty(), ImmutableList.of(), ImmutableList.of(modify));
    }

    Stream<PropertyContent<?>> readOf(FastMapper mapper, T value, Stream<PropertyInfo.Read<?, E>> readProperties);
    T writeOf(FastMapper mapper, Stream<PropertyContent<?>> properties, Stream<PropertyInfo.Write<?, E>> writeProperties);

    @Override
    default Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
        return readOf(mapper, value, readProperties());
    }
    @Override
    default T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
        return writeOf(mapper, properties, writeProperties());
    }
}

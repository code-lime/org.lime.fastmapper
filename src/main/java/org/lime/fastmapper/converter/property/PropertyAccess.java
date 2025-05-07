package org.lime.fastmapper.converter.property;

import com.google.common.collect.ImmutableList;
import org.lime.core.common.reflection.LambdaInfo;
import org.lime.core.common.system.execute.Callable;
import org.lime.core.common.system.execute.Func1;
import org.lime.core.common.system.execute.Func2;
import org.lime.core.common.system.execute.Func3;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.info.PropertyInfo;
import org.lime.fastmapper.converter.property.info.PropertyLoader;

import java.util.stream.Stream;

public interface PropertyAccess<T> extends
        PropertyReader<T>,
        PropertyWriter<T> {
    Class<T> accessClass();

    record Modify<T>(
            PropertyAccess<T> root,
            ImmutableList<Func3<FastMapper, T, Stream<PropertyContent<?>>, Stream<PropertyContent<?>>>> modifyRead,
            ImmutableList<Func2<FastMapper, Stream<PropertyContent<?>>, Stream<PropertyContent<?>>>> modifyWrite)
            implements PropertyAccess<T> {
        @Override
        public Class<T> accessClass() {
            return root.accessClass();
        }
        @Override
        public Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
            var stream = root().read(mapper, value);
            for (var modify : modifyRead)
                stream = modify.invoke(mapper, value, stream);
            return stream;
        }
        @Override
        public T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
            for (var modify : modifyWrite)
                properties = modify.invoke(mapper, properties);
            return root().write(mapper, properties);
        }
    }

    default <In>PropertyAccess<T> modifyRead(Callable method, Func1<PropertyContent<In>, PropertyContent<?>> modify) {
        return this.<In>modifyRead(method, (_, _, v) -> modify.invoke(v));
    }
    default <In>PropertyAccess<T> modifyRead(Callable method, Func3<FastMapper, T, PropertyContent<In>, PropertyContent<?>> modify) {
        return PropertyLoader.extractInfo(LambdaInfo.getMethod(method))
                .map(PropertyInfo::name)
                .map(name -> modifyRead(name, modify))
                .orElseThrow(() -> new IllegalArgumentException("Property from '"+modify+"' not found"));
    }
    default <In>PropertyAccess<T> modifyRead(String name, Func1<PropertyContent<In>, PropertyContent<?>> modify) {
        return this.<In>modifyRead(name, (_, _, v) -> modify.invoke(v));
    }
    default <In>PropertyAccess<T> modifyRead(String name, Func3<FastMapper, T, PropertyContent<In>, PropertyContent<?>> modify) {
        return modifyRead((mapper, value, stream) -> stream
                .map(v -> v.name().equals(name)
                        ? modify.invoke(mapper, value, (PropertyContent<In>) v)
                        : v));
    }
    default PropertyAccess<T> modifyRead(Func3<FastMapper, T, Stream<PropertyContent<?>>, Stream<PropertyContent<?>>> modify) {
        return this instanceof Modify<T> modifyAccess
                ? new Modify<>(
                modifyAccess.root(),
                ImmutableList.<Func3<FastMapper, T, Stream<PropertyContent<?>>, Stream<PropertyContent<?>>>>builder()
                        .addAll(modifyAccess.modifyRead())
                        .add(modify)
                        .build(),
                modifyAccess.modifyWrite())
                : new Modify<>(this, ImmutableList.of(modify), ImmutableList.of());
    }

    default <In>PropertyAccess<T> modifyWrite(Callable method, Func1<PropertyContent<In>, PropertyContent<?>> modify) {
        return this.<In>modifyWrite(method, (_, v) -> modify.invoke(v));
    }
    default <In>PropertyAccess<T> modifyWrite(Callable method, Func2<FastMapper, PropertyContent<In>, PropertyContent<?>> modify) {
        return PropertyLoader.extractInfo(LambdaInfo.getMethod(method))
                .map(PropertyInfo::name)
                .map(name -> modifyWrite(name, modify))
                .orElseThrow(() -> new IllegalArgumentException("Property from '"+modify+"' not found"));
    }
    default <In>PropertyAccess<T> modifyWrite(String name, Func1<PropertyContent<In>, PropertyContent<?>> modify) {
        return this.<In>modifyWrite(name, (_, v) -> modify.invoke(v));
    }
    default <In>PropertyAccess<T> modifyWrite(String name, Func2<FastMapper, PropertyContent<In>, PropertyContent<?>> modify) {
        return modifyWrite((mapper, stream) -> stream
                .map(v -> v.name().equals(name)
                        ? modify.invoke(mapper, (PropertyContent<In>) v)
                        : v));
    }
    default PropertyAccess<T> modifyWrite(Func2<FastMapper, Stream<PropertyContent<?>>, Stream<PropertyContent<?>>> modify) {
        return this instanceof Modify<T> modifyAccess
                ? new Modify<>(
                modifyAccess.root(),
                modifyAccess.modifyRead(),
                ImmutableList.<Func2<FastMapper, Stream<PropertyContent<?>>, Stream<PropertyContent<?>>>>builder()
                        .addAll(modifyAccess.modifyWrite())
                        .add(modify)
                        .build())
                : new Modify<>(this, ImmutableList.of(), ImmutableList.of(modify));
    }
}

package org.lime.fastmapper.converter.property;

import com.google.common.collect.ImmutableList;
import org.lime.reflection.LambdaInfo;
import org.lime.system.execute.Func1;
import org.lime.system.execute.Func2;
import org.lime.system.execute.Func3;
import org.lime.system.execute.ICallable;
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

    default <In>PropertyAccess<T> modifyRead(ICallable method, Func1<PropertyContent<In>, PropertyContent<?>> modify) {
        return this.<In>modifyRead(method, (_, _, v) -> modify.invoke(v));
    }
    default <In>PropertyAccess<T> modifyRead(ICallable method, Func3<FastMapper, T, PropertyContent<In>, PropertyContent<?>> modify) {
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
}

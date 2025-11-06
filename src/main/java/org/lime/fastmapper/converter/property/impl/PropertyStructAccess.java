package org.lime.fastmapper.converter.property.impl;

import com.google.common.primitives.Primitives;
import org.lime.core.common.reflection.Lambda;
import org.lime.core.common.reflection.ReflectionConstructor;
import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.tuple.Tuple;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyAccess;
import org.lime.fastmapper.converter.property.PropertyContent;
import org.lime.fastmapper.converter.property.info.PropertyContents;
import org.lime.fastmapper.converter.property.info.PropertyInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

public class PropertyStructAccess<T>
        implements PropertyAccess<T> {
    private final Class<T> tClass;
    private final Map<String, StructField<?>> fields = new HashMap<>();
    private final Func0<T> constructor;

    public PropertyStructAccess(Class<T> tClass) {
        this.tClass = tClass;
        List<Field> fields = Arrays.stream(tClass.getFields())
                .filter(v -> {
                    int mod = v.getModifiers();
                    return !Modifier.isStatic(mod) && Modifier.isPublic(mod);
                })
                .toList();
        if (fields.isEmpty())
            throw new IllegalArgumentException("Struct class not have public fields");
        for (var component : fields) {
            String name = component.getName();
            StructField<?> field = new StructField<>(name,
                    component,
                    Lambda.getter(component, Func1.class),
                    Lambda.setter(component, Action2.class),
                    component.getGenericType(),
                    Primitives.wrap(component.getType()));
            this.fields.put(name, field);
        }
        constructor = ReflectionConstructor.of(tClass).lambda(Func0.class);
    }

    @Override
    public Class<T> accessClass() {
        return tClass;
    }

    private record StructField<T>(
            String name,
            Field member,
            Func1<Object, T> read,
            Action2<Object, T> write,
            Type genType,
            Class<T> type)
            implements PropertyInfo.Read<T, Field>, PropertyInfo.Write<T, Field> {
        @Override
        public boolean optionalType() {
            return false;
        }
        @Override
        public boolean optional() {
            return false;
        }
        @Override
        public boolean prefix() {
            return false;
        }

        @Override
        public Optional<Lazy<T>> getFrom(Object target) {
            return Optional.of(Lazy.of(() -> read.invoke(target)));
        }

        @Override
        public boolean getter() {
            return true;
        }

        @Override
        public void writeTo(Object target, T value) {
            write.invoke(target, value);
        }
    }

    @Override
    public Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
        return PropertyContents.readSingle(value, fields.values().stream());
    }

    @Override
    public T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
        T value = constructor.invoke();
        PropertyContents.writeSingle(mapper, value, fields.values().stream(), properties);
        return value;
    }
}

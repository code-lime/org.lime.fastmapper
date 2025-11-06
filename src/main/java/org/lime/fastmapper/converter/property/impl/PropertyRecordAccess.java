package org.lime.fastmapper.converter.property.impl;

import com.google.common.primitives.Primitives;
import org.lime.core.common.reflection.Lambda;
import org.lime.core.common.reflection.ReflectionConstructor;
import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Callable;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyAccess;
import org.lime.fastmapper.converter.property.PropertyContent;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class PropertyRecordAccess<T extends Record>
        implements PropertyAccess<T> {
    private final Class<T> tClass;
    private final int size;
    private final Map<String, RecordField<?>> fields = new HashMap<>();
    private final RecordField<?>[] fieldsArray;
    private final Callable constructor;

    public PropertyRecordAccess(Class<T> tClass) {
        this.tClass = tClass;
        RecordComponent[] recordComponents = tClass.getRecordComponents();
        this.size = recordComponents.length;
        Class<?>[] recordArgs = new Class[size];
        fieldsArray = new RecordField[size];
        for (int i = 0; i < size; i++) {
            var component = recordComponents[i];
            String name = component.getName();
            RecordField<?> field = new RecordField<>(i, name, Lambda.lambda(component.getAccessor(), Func1.class), component.getGenericType(), Primitives.wrap(component.getType()));
            fields.put(name, field);
            fieldsArray[i] = field;
            recordArgs[i] = component.getType();
        }
        constructor = ReflectionConstructor.of(tClass, recordArgs).lambda();
    }

    @Override
    public Class<T> accessClass() {
        return tClass;
    }

    private record RecordField<T>(
            int index,
            String name,
            Func1<Object, T> read,
            Type genType,
            Class<T> type) {}

    @Override
    public Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
        return fields.values()
                .stream()
                .map(v -> readContent(v, value));
    }
    private <J>PropertyContent<J> readContent(RecordField<J> field, T value) {
        return new PropertyContent<>(field.name(), field.genType(), field.type(), Lazy.of(() -> field.read().invoke(value)));
    }

    @Override
    public T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
        Object[] args = new Object[size];
        boolean[] markArgs = new boolean[size];

        properties.forEach(v -> {
            RecordField<?> field = fields.get(v.name());
            if (field == null)
                return;
            int i = field.index();
            var value = v.value();
            args[i] = mapper.map(value, field.type(), v.genType(), field.genType());
            markArgs[i] = true;
        });

        for (int i = 0; i < size; i++) {
            boolean mark = markArgs[i];
            if (!mark)
                throw new IllegalArgumentException("Arg #" + i + " (" + fieldsArray[i].name() + ") not set");
        }
        var vv = constructor.call(args);
        return (T)vv;
    }
}

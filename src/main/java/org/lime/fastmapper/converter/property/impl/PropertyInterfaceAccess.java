package org.lime.fastmapper.converter.property.impl;

import com.google.common.collect.ImmutableMap;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.execute.Func1;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyAccess;
import org.lime.fastmapper.converter.property.PropertyContent;
import org.lime.fastmapper.converter.property.info.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

public class PropertyInterfaceAccess<T>
        implements PropertyAccess<T> {
    private final Class<T> tClass;
    private final ImmutableMap<String, List<InterfaceField<?>>> reads;
    private final ImmutableMap<Method, InterfaceField<?>> writes;

    public PropertyInterfaceAccess(Class<T> tClass) {
        if (!tClass.isInterface())
            throw new IllegalArgumentException("Not interface " + tClass);
        this.tClass = tClass;
        PropertyReadContext<InterfaceField<?>, Method> readsContext = new PropertyReadContext<>(new HashMap<>(), new PropertyReadCreator<>() {
            @Override
            public <Out>InterfaceField<?> create(
                    PropertyInfo<Out, Method> info,
                    boolean isOptionalType,
                    Func1<Object, Optional<Lazy<Out>>> reader) {
                return new InterfaceField<>(info.name(), info.prefix(), info.member(), reader, isOptionalType, info.genType(), info.type());
            }
        });
        PropertyLoader.loadProperties(tClass, readsContext, null);
        reads = ImmutableMap.copyOf(readsContext.data());
        writes = readsContext.data()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(ImmutableMap.toImmutableMap(InterfaceField::member, v -> v));
        for (Method method : tClass.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || writes.containsKey(method) || method.isDefault())
                continue;
            throw new IllegalArgumentException("Error load interface method-property " + method);
        }
    }

    @Override
    public Class<T> accessClass() {
        return tClass;
    }

    private record InterfaceField<T>(
            String name,
            boolean prefix,
            Method member,
            Func1<Object, Optional<Lazy<T>>> reader,
            boolean optionalType,
            Type genType,
            Class<T> type)
            implements PropertyInfo.Read<T, Method> {
        @Override
        public Optional<Lazy<T>> getFrom(Object target) {
            return reader.invoke(target);
        }
    }

    @Override
    public Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
        return PropertyContents.read(value, reads.values().stream().flatMap(Collection::stream));
    }

    private static final Method toStringMethod = ReflectionMethod.of(Object.class, "toString").target();
    @Override
    public T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
        Map<Method, Object> values = new HashMap<>();
        properties.forEach(content -> {
            var fields = reads.get(content.name());
            if (fields == null)
                return;
            for (var field : fields)
                mapper.tryMap(content.value(), field.type(), content.genType(), field.genType())
                        .ifPresent(v -> {
                            if (field.optionalType())
                                values.put(field.member(), Optional.of(v.val0));
                            else
                                values.put(field.member(), v.val0);
                        });
        });
        writes.forEach((method, field) -> {
            if (values.containsKey(method) || method.isDefault())
                return;
            throw new IllegalArgumentException("Property " + tClass + "." + field.name() + " not optional");
        });
        var result = Proxy.newProxyInstance(
                tClass.getClassLoader(),
                new Class<?>[]{tClass}, (proxy, method, args) -> {
                    if (method.equals(toStringMethod)) {
                        List<String> pairs = new ArrayList<>();
                        values.forEach((m, value) -> pairs.add(m.getName() + ": `" + value + "`"));
                        return "{ " + String.join(", ", pairs) + " }";
                    }
                    if (values.containsKey(method))
                        return values.get(method);
                    return InvocationHandler.invokeDefault(proxy, method, args);
                });
        return (T)result;
    }
}

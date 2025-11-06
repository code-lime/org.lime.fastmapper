package org.lime.fastmapper.converter.property.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.lime.core.common.reflection.ReflectionConstructor;
import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.utils.tuple.Tuple;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyAccessOptions;
import org.lime.fastmapper.converter.property.PropertyContent;
import org.lime.fastmapper.converter.property.info.*;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class PropertyDefaultAccess<T>
        implements PropertyAccessOptions<T, Method> {
    protected final ImmutableMap<String, ImmutableList<PropertyInfo.Read<?, Method>>> reads;
    protected final ImmutableMap<String, ImmutableList<PropertyInfo.Write<?, Method>>> writes;
    protected final Func0<T> ctor;
    protected final Class<T> tClass;

    public PropertyDefaultAccess(Class<T> tClass) {
        this.tClass = tClass;
        ctor = ReflectionConstructor.of(tClass).lambda(Func0.class);

        var readsContext = PropertyReadContext.<Method>createDefault(new HashMap<>());
        var writesContext = PropertyWriteContext.<Method>createDefault(new HashMap<>());
        PropertyLoader.loadProperties(tClass, readsContext, writesContext);
        reads = readsContext.data().entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, kv -> ImmutableList.copyOf(kv.getValue())));
        writes = writesContext.data().entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, kv -> ImmutableList.copyOf(kv.getValue())));
    }

    @Override
    public Class<T> accessClass() {
        return tClass;
    }

    @Override
    public Stream<PropertyInfo.Read<?, Method>> readProperties() {
        return reads.values().stream().flatMap(Collection::stream);
    }

    @Override
    public Stream<PropertyInfo.Write<?, Method>> writeProperties() {
        return writes.values().stream().flatMap(Collection::stream);
    }

    @Override
    public Stream<PropertyContent<?>> readOf(FastMapper mapper, T value, Stream<PropertyInfo.Read<?, Method>> readProperties) {
        return PropertyContents.read(value, readProperties);
    }

    @Override
    public T writeOf(FastMapper mapper, Stream<PropertyContent<?>> properties, Stream<PropertyInfo.Write<?, Method>> writeProperties) {
        T value = ctor.invoke();
        PropertyContents.write(mapper, value, writeProperties, properties);
        return value;
    }
}

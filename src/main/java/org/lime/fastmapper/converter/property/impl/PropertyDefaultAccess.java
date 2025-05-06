package org.lime.fastmapper.converter.property.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.lime.core.common.reflection.ReflectionConstructor;
import org.lime.core.common.system.execute.Func0;
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
        implements PropertyAccessOptions<PropertyDefaultAccess<T>, T, Method> {
    protected final ImmutableMap<String, ImmutableList<PropertyInfo.Read<?, Method>>> reads;
    protected final ImmutableMap<String, ImmutableList<PropertyInfo.Write<?, Method>>> writes;
    protected final Func0<T> ctor;
    protected final Class<T> tClass;

    private boolean optionPrefixOnly = false;

    public PropertyDefaultAccess(Class<T> tClass) {
        this.tClass = tClass;
        ctor = ReflectionConstructor.of(tClass).lambda(Func0.class);

        var readsContext = PropertyReadContext.<Method>createDefault(new HashMap<>());
        var writesContext = PropertyWriteContext.<Method>createDefault(new HashMap<>());
        PropertyLoader.loadProperties(tClass, readsContext, writesContext);
        reads = readsContext.data().entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, kv -> ImmutableList.copyOf(kv.getValue())));
        writes = writesContext.data().entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, kv -> ImmutableList.copyOf(kv.getValue())));
    }
    protected PropertyDefaultAccess(PropertyDefaultAccess<T> other) {
        reads = other.reads;
        writes = other.writes;
        ctor = other.ctor;
        tClass = other.tClass;
        optionPrefixOnly = other.optionPrefixOnly;
    }

    @Override
    public Class<T> accessClass() {
        return tClass;
    }

    @Override
    public boolean isPrefixOnly() {
        return optionPrefixOnly;
    }

    @Override
    public Stream<PropertyInfo.Read<?, Method>> reads() {
        return reads.values().stream().flatMap(Collection::stream);
    }

    @Override
    public Stream<PropertyInfo.Write<?, Method>> writes() {
        return writes.values().stream().flatMap(Collection::stream);
    }

    @Override
    public PropertyDefaultAccess<T> withPrefixOnly(boolean enable) {
        var clone = new PropertyDefaultAccess<>(this);
        clone.optionPrefixOnly = enable;
        return clone;
    }


    @Override
    public Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
        return PropertyContents.read(value, this::filterProperty, reads);
    }
    @Override
    public T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
        T value = ctor.invoke();
        PropertyContents.write(mapper, value, writes, this::filterProperty, properties);
        return value;
    }
}

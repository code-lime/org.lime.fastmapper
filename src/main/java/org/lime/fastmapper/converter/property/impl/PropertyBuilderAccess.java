package org.lime.fastmapper.converter.property.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.lime.reflection.Lambda;
import org.lime.reflection.ReflectionMethod;
import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyAccessOptions;
import org.lime.fastmapper.converter.property.PropertyContent;
import org.lime.fastmapper.converter.property.info.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class PropertyBuilderAccess<T, B> implements
        PropertyAccessOptions<PropertyBuilderAccess<T, B>, T, Method> {
    private static <B>Class<B> findBuilderClass(Class<?> tClass) {
        for (Class<?> tBuilder : tClass.getDeclaredClasses())
            if (tBuilder.getSimpleName().equals("Builder"))
                return (Class<B>) tBuilder;
        throw new IllegalArgumentException("Builder clasa not found in " + tClass + " class");
    }

    protected final ImmutableMap<String, ImmutableList<PropertyInfo.Read<?, Method>>> reads;
    protected final ImmutableMap<String, ImmutableList<PropertyInfo.Write<?, Method>>> writes;

    public Class<B> tBuilder() {
        return tBuilder;
    }

    private final Class<T> tClass;
    private final Class<B> tBuilder;

    private final Func0<B> newBuilderFunc;
    private final Func1<B, T> buildFunc;

    private boolean optionPrefixOnly = false;

    public PropertyBuilderAccess(Class<T> tClass) {
        this.tClass = tClass;
        tBuilder = findBuilderClass(tClass);

        var builderRefMethod = ReflectionMethod.of(tClass, "newBuilder");
        var builderMethod = builderRefMethod.method();
        if (!Modifier.isStatic(builderMethod.getModifiers())
                || !builderMethod.getReturnType().equals(tBuilder)
                || builderMethod.getParameterCount() != 0)
            throw new IllegalArgumentException("Method static " + tClass + ".newBuilder() not found");

        var buildRefMethod = ReflectionMethod.of(tBuilder, "build");
        var buildMethod = buildRefMethod.method();
        if (Modifier.isStatic(buildMethod.getModifiers())
                || !buildMethod.getReturnType().equals(tClass)
                || buildMethod.getParameterCount() != 0)
            throw new IllegalArgumentException("Method instance " + tBuilder + ".build() not found");

        newBuilderFunc = builderRefMethod.lambda(Func0.class);
        buildFunc = buildRefMethod.lambda(Func1.class);

        var readsContext = PropertyReadContext.<Method>createDefault(new HashMap<>());
        var writesContext = PropertyWriteContext.<Method>createDefault(new HashMap<>());
        loadProperties(tClass, tBuilder, readsContext, writesContext);
        reads = readsContext.data().entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, kv -> ImmutableList.copyOf(kv.getValue())));
        writes = writesContext.data().entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, kv -> ImmutableList.copyOf(kv.getValue())));
    }

    @Override
    public Class<T> accessClass() {
        return tClass;
    }

    protected void loadProperties(
            Class<T> tClass,
            Class<B> tBuilder,
            PropertyReadContext<PropertyInfo.Read<?, Method>, Method> readsContext,
            PropertyWriteContext<PropertyInfo.Write<?, Method>, Method> writesContext) {
        PropertyLoader.loadProperties(tClass, readsContext, null);
        PropertyLoader.loadProperties(tBuilder, null, writesContext, tBuilder);
    }

    protected PropertyBuilderAccess(PropertyBuilderAccess<T, B> other) {
        reads = other.reads;
        writes = other.writes;
        tClass = other.tClass;
        tBuilder = other.tBuilder;
        newBuilderFunc = other.newBuilderFunc;
        buildFunc = other.buildFunc;
        optionPrefixOnly = other.optionPrefixOnly;
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
    public PropertyBuilderAccess<T, B> withPrefixOnly(boolean enable) {
        var clone = new PropertyBuilderAccess<>(this);
        clone.optionPrefixOnly = enable;
        return clone;
    }

    @Override
    public Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
        return PropertyContents.read(value, this::filterProperty, reads);
    }

    @Override
    public T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
        B builder = newBuilderFunc.invoke();
        PropertyContents.write(mapper, builder, writes, this::filterProperty, properties);
        return buildFunc.invoke(builder);
    }
}

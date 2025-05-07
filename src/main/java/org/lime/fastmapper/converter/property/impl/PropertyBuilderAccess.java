package org.lime.fastmapper.converter.property.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;
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
        PropertyAccessOptions<T, Method> {
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

    public PropertyBuilderAccess(Class<T> tClass) {
        this(tClass, null, null);
    }
    public PropertyBuilderAccess(Class<T> tClass, @Nullable Func0<B> newBuilderFunc, @Nullable Func1<B, T> buildFunc) {
        this.tClass = tClass;
        tBuilder = findBuilderClass(tClass);

        if (newBuilderFunc == null) {
            var builderRefMethod = ReflectionMethod.of(tClass, "newBuilder");
            var builderMethod = builderRefMethod.method();
            if (!Modifier.isStatic(builderMethod.getModifiers())
                    || !builderMethod.getReturnType().equals(tBuilder)
                    || builderMethod.getParameterCount() != 0)
                throw new IllegalArgumentException("Method static " + tClass + ".newBuilder() not found");
            newBuilderFunc = builderRefMethod.lambda(Func0.class);
        }
        this.newBuilderFunc = newBuilderFunc;

        if (buildFunc == null) {
            var buildRefMethod = ReflectionMethod.of(tBuilder, "build");
            var buildMethod = buildRefMethod.method();
            if (Modifier.isStatic(buildMethod.getModifiers())
                    || !buildMethod.getReturnType().equals(tClass)
                    || buildMethod.getParameterCount() != 0)
                throw new IllegalArgumentException("Method instance " + tBuilder + ".build() not found");
            buildFunc = buildRefMethod.lambda(Func1.class);
        }
        this.buildFunc = buildFunc;

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
        B builder = newBuilderFunc.invoke();
        PropertyContents.write(mapper, builder, writeProperties, properties);
        return buildFunc.invoke(builder);
    }
}

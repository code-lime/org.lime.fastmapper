package org.lime.fastmapper.converter.property.info;

import org.lime.core.common.utils.tuple.Tuple;
import org.lime.core.common.utils.IterableUtils;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyContent;

import java.lang.reflect.Member;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertyContents {
    public static <T> Collector<T, ?, List<T>> toSingletonList() {
        return Collector.of(
                () -> Tuple.<T>of(null),
                (a, v) -> { if (a.val0 == null) a.val0 = v; },
                (a, _) -> a,
                a -> a.val0 == null ? Collections.emptyList() : Collections.singletonList(a.val0));
    }

    public static <T, E extends Member>Stream<PropertyContent<?>> read(
            T value,
            Stream<? extends PropertyInfo.Read<?, E>> reads) {
        return reads
                .flatMap(v -> readContent(v, value).stream());
    }
    public static <T, E extends Member>Stream<PropertyContent<?>> readSingle(
            T value,
            Stream<? extends PropertyInfo.Read<?, E>> reads) {
        return reads
                .filter(IterableUtils.distinctBy(PropertyInfo::name))
                .flatMap(v -> readContent(v, value).stream());
    }
    private static <T, J, E extends Member>Optional<PropertyContent<?>> readContent(PropertyInfo.Read<J, E> property, T value) {
        return property.getFrom(value)
                .map(v -> new PropertyContent<>(property.name(), property.genType(), property.type(), v));
    }

    public static <T, E extends Member>void write(
            FastMapper mapper,
            T value,
            Stream<? extends PropertyInfo.Write<?, E>> writes,
            Stream<PropertyContent<?>> properties) {
        HashMap<String, List<PropertyInfo.Write<?, E>>> writesCopy = writes
                .collect(Collectors.groupingBy(PropertyInfo::name, HashMap::new, Collectors.toList()));
        writeCopy(mapper, value, properties, writesCopy);
    }
    public static <T, E extends Member>void writeSingle(
            FastMapper mapper,
            T value,
            Stream<? extends PropertyInfo.Write<?, E>> writes,
            Stream<PropertyContent<?>> properties) {
        HashMap<String, List<PropertyInfo.Write<?, E>>> writesCopy = writes
                .collect(Collectors.groupingBy(PropertyInfo::name, HashMap::new, toSingletonList()));
        writeCopy(mapper, value, properties, writesCopy);
    }

    private static <T, E extends Member> void writeCopy(
            FastMapper mapper,
            T value,
            Stream<PropertyContent<?>> properties,
            HashMap<String, ? extends Iterable<PropertyInfo.Write<?, E>>> writesCopy) {
        properties.forEach(v -> {
            var writeMap = writesCopy.get(v.name());
            if (writeMap == null)
                return;
            for (PropertyInfo.Write<?, E> write : writeMap) {
                if (!tryWriteContent(write, mapper, value, v))
                    continue;
                writesCopy.remove(v.name());
                return;
            }
        });
        writesCopy.forEach((name, writeMap) -> writeMap.forEach(write -> {
            if (write.optional())
                return;
            throw new IllegalArgumentException("Property " + value.getClass() + "." + name + " not optional");
        }));
    }

    private static <T, J, E extends Member>boolean tryWriteContent(
            PropertyInfo.Write<J, E> property,
            FastMapper mapper,
            T value,
            PropertyContent<?> content) {
        return mapper.tryMap(content.value(), property.type(), content.genType(), property.genType())
                .map(v -> {
                    property.writeTo(value, v.val0);
                    return true;
                })
                .orElse(false);
    }
}

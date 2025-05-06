package org.lime.fastmapper.converter.property.info;

import org.lime.core.common.system.execute.Func1;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyContent;

import java.lang.reflect.Member;
import java.util.*;
import java.util.stream.Stream;

public class PropertyContents {
    public static <T, E extends Member>Stream<PropertyContent<?>> read(
            T value,
            Func1<PropertyInfo.Read<?, E>, Boolean> filter,
            Map<String, ? extends List<? extends PropertyInfo.Read<?, E>>> reads) {
        return reads.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(filter::invoke)
                .flatMap(v -> readContent(v, value).stream());
    }
    public static <T, E extends Member>Stream<PropertyContent<?>> readSingle(
            T value,
            Func1<PropertyInfo.Read<?, E>, Boolean> filter,
            Map<String, ? extends PropertyInfo.Read<?, E>> reads) {
        return reads.values()
                .stream()
                .filter(filter::invoke)
                .flatMap(v -> readContent(v, value).stream());
    }
    private static <T, J, E extends Member>Optional<PropertyContent<?>> readContent(PropertyInfo.Read<J, E> property, T value) {
        return property.getFrom(value)
                .map(v -> new PropertyContent<>(property.name(), property.genType(), property.type(), v));
    }

    public static <T, E extends Member>void write(
            FastMapper mapper,
            T value,
            Map<String, ? extends List<? extends PropertyInfo.Write<?, E>>> writes,
            Func1<PropertyInfo.Write<?, E>, Boolean> filter,
            Stream<PropertyContent<?>> properties) {
        HashMap<String, ArrayList<PropertyInfo.Write<?, E>>> writesCopy = new HashMap<>();
        writes.forEach((k,v) -> {
            ArrayList<PropertyInfo.Write<?, E>> vv = new ArrayList<>(v);
            vv.removeIf(vvv -> !filter.invoke(vvv));
            if (vv.isEmpty())
                return;
            writesCopy.put(k, vv);
        });
        writeCopy(mapper, value, properties, writesCopy);
    }
    public static <T, E extends Member>void writeSingle(
            FastMapper mapper,
            T value,
            Map<String, ? extends PropertyInfo.Write<?, E>> writes,
            Func1<PropertyInfo.Write<?, E>, Boolean> filter,
            Stream<PropertyContent<?>> properties) {
        HashMap<String, ArrayList<PropertyInfo.Write<?, E>>> writesCopy = new HashMap<>();
        writes.forEach((k,v) -> {
            ArrayList<PropertyInfo.Write<?, E>> vv = new ArrayList<>();
            if (!filter.invoke(v))
                return;
            vv.add(v);
            writesCopy.put(k, vv);
        });
        writeCopy(mapper, value, properties, writesCopy);
    }

    private static <T, E extends Member> void writeCopy(
            FastMapper mapper,
            T value,
            Stream<PropertyContent<?>> properties,
            HashMap<String, ArrayList<PropertyInfo.Write<?, E>>> writesCopy) {
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

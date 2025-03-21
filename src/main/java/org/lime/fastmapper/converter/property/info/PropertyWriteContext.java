package org.lime.fastmapper.converter.property.info;

import org.lime.system.execute.Action2;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record PropertyWriteContext<T, E extends Member>(
        Map<String, List<T>> data,
        PropertyWriteCreator<T, E> creator) {
    public <In, Out>void add(PropertyInfo<Out, E> info, boolean optional, Action2<Object, Out> writer) {
        data.computeIfAbsent(info.name(), _ -> new ArrayList<>())
                .add(creator.create(info, optional, writer));
    }
    public static <E extends Member>PropertyWriteContext<PropertyInfo.Write<?, E>, E> createDefault(Map<String, List<PropertyInfo.Write<?, E>>> data) {
        return new PropertyWriteContext<>(data, new PropertyWriteCreator<>() {
            @Override
            public <Out> PropertyInfo.Write<?, E> create(PropertyInfo<Out, E> info, boolean optional, Action2<Object, Out> writer) {
                return new PropertyWrite<>(info.name(), info.prefix(), info.member(), writer, optional, info.genType(), info.type());
            }
        });
    }
}

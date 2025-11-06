package org.lime.fastmapper.converter.property.info;

import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.execute.Func1;

import javax.annotation.Nullable;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PropertyReadContext<T, E extends Member>(
        Map<String, List<T>> data,
        PropertyReadCreator<T, E> creator) {
    public <Out>void add(
            PropertyInfo<Out, E> info,
            @Nullable Func1<Object, Boolean> hasReader,
            Func1<Object, Out> reader) {
        data.computeIfAbsent(info.name(), _ -> new ArrayList<>())
                .add(creator.create(info, hasReader, reader));
    }

    public static <E extends Member>PropertyReadContext<PropertyInfo.Read<?, E>, E> createDefault(Map<String, List<PropertyInfo.Read<?, E>>> data) {
        return new PropertyReadContext<>(data, new PropertyReadCreator<>() {
            @Override
            public <Out> PropertyInfo.Read<?, E> create(
                    PropertyInfo<Out, E> info,
                    boolean isOptionalType,
                    Func1<Object, Optional<Lazy<Out>>> reader) {
                return new PropertyRead<>(info.name(), info.prefix(), info.member(), reader, isOptionalType, info.genType(), info.type());
            }
        });
    }
}

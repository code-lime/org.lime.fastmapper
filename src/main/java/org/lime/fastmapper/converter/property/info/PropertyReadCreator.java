package org.lime.fastmapper.converter.property.info;

import com.google.common.collect.ImmutableMap;
import org.lime.core.common.system.Lazy;
import org.lime.core.common.system.execute.Func1;
import org.lime.fastmapper.reflection.GenericUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public interface PropertyReadCreator<T, E extends Member> {
    interface OptionalReader {
        <Out>Func1<Object, Optional<Lazy<Out>>> createReader(Func1<Object, Out> reader);
        Type extractType(Type optionalType);

        static <T, Opt>OptionalReader createPrimitive(
                Class<T> wrapped,
                Func1<Opt, Boolean> isPresent,
                Func1<Opt, T> getAsValue) {
            return new OptionalReader() {
                @Override
                public <Out> Func1<Object, Optional<Lazy<Out>>> createReader(Func1<Object, Out> reader) {
                    Func1<Object, Opt> func = (Func1)reader;
                    return v -> {
                        var opt = func.invoke(v);
                        return isPresent.invoke(opt) ? Optional.of(Lazy.of(() -> (Out)getAsValue.invoke(opt))) : Optional.empty();
                    };
                }
                @Override
                public Type extractType(Type optionalType) {
                    return wrapped;
                }
            };
        }
    }

    ImmutableMap<Class<?>, OptionalReader> OPTIONAL_READERS = ImmutableMap.of(
            Optional.class, new OptionalReader() {
                @Override
                public <Out>Func1<Object, Optional<Lazy<Out>>> createReader(Func1<Object, Out> reader) {
                    Func1<Object, Optional<Out>> func = (Func1)reader;
                    return v -> func.invoke(v).map(Lazy::of);
                }
                @Override
                public Type extractType(Type optionalType) {
                    if (!(optionalType instanceof ParameterizedType pt)
                            || !pt.getRawType().equals(Optional.class)
                            || pt.getActualTypeArguments().length != 1)
                        throw new IllegalArgumentException("genType " + optionalType + " is not Optional generic");
                    return pt.getActualTypeArguments()[0];
                }
            },
            OptionalInt.class, OptionalReader.createPrimitive(Integer.class, OptionalInt::isPresent, OptionalInt::getAsInt),
            OptionalDouble.class, OptionalReader.createPrimitive(Double.class, OptionalDouble::isPresent, OptionalDouble::getAsDouble),
            OptionalLong.class, OptionalReader.createPrimitive(Long.class, OptionalLong::isPresent, OptionalLong::getAsLong)
    );
    default <Out>T create(
            PropertyInfo<Out, E> info,
            @Nullable Func1<Object, Boolean> hasReader,
            Func1<Object, Out> reader) {
        OptionalReader optionalReader = OPTIONAL_READERS.get(info.type());
        if (optionalReader != null) {
            if (hasReader != null)
                throw new IllegalArgumentException("hasReader is not null but type is Optional");
            var genType = optionalReader.extractType(info.genType());
            var optType = GenericUtils.readRawClass(genType);
            return (T)create(new PropertyInfo.Impl(
                    info.name(),
                    info.getter(),
                    info.prefix(),
                    info.member(),
                    genType,
                    optType), true, optionalReader.createReader(reader));
        }
        return create(info, false, hasReader == null
                ? v -> Optional.of(Lazy.of(() -> reader.invoke(v)))
                : v -> hasReader.invoke(v) ? Optional.of(Lazy.of(() -> reader.invoke(v))) : Optional.empty());
    }
    <Out>T create(
            PropertyInfo<Out, E> info,
            boolean isOptionalType,
            Func1<Object, Optional<Lazy<Out>>> reader);
}

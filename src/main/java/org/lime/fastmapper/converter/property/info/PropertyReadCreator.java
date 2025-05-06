package org.lime.fastmapper.converter.property.info;

import org.lime.core.common.system.Lazy;
import org.lime.core.common.system.execute.Func1;
import org.lime.fastmapper.GenTypePair;

import javax.annotation.Nullable;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public interface PropertyReadCreator<T, E extends Member> {
    default <Out>T create(
            PropertyInfo<Out, E> info,
            @Nullable Func1<Object, Boolean> hasReader,
            Func1<Object, Out> reader) {
        if (info.type().equals(Optional.class)) {
            if (hasReader != null)
                throw new IllegalArgumentException("hasReader is not null but type is Optional");
            if (!(info.genType() instanceof ParameterizedType pt)
                    || !pt.getRawType().equals(Optional.class)
                    || pt.getActualTypeArguments().length != 1)
                throw new IllegalArgumentException("genType "+info.genType()+" is not Optional generic");
            var genType = pt.getActualTypeArguments()[0];
            var optType = GenTypePair.readRawClass(genType);
            Func1<Object, Optional<Out>> func = (Func1)reader;
            return (T)create(new PropertyInfo.Impl(
                    info.name(),
                    info.getter(),
                    info.prefix(),
                    info.member(),
                    genType,
                    optType), true, v -> func.invoke(v).map(Lazy::of));
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

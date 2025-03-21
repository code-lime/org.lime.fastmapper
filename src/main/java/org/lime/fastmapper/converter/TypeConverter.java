package org.lime.fastmapper.converter;

import org.lime.fastmapper.FastMapper;

import java.lang.reflect.Type;

public interface TypeConverter<In, Out> {
    Out convert(FastMapper mapper, In value);
    default Out convert(FastMapper mapper, In value, Type inType, Type outType) {
        return convert(mapper, value);
    }
}

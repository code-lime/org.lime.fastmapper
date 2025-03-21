package org.lime.fastmapper.converter;

import org.lime.fastmapper.FastMapper;

public interface SimpleTypeConverter<In, Out>
        extends TypeConverter<In, Out> {
    Out convert(In value);
    @Override
    default Out convert(FastMapper mapper, In value) {
        return convert(value);
    }
}

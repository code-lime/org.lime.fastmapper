package org.lime.fastmapper.converter;

import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyContent;
import org.lime.fastmapper.converter.property.PropertyReader;
import org.lime.fastmapper.converter.property.PropertyWriter;

import java.util.stream.Stream;

public interface PropertyTypeConverter<In, Out>
        extends TypeConverter<In, Out> {
    PropertyReader<In> reader();
    PropertyWriter<Out> writer();

    @Override
    default Out convert(FastMapper mapper, In value) {
        Stream<PropertyContent<?>> props = reader().read(mapper, value);
        var cache = props.toList();
        props = cache.stream();
        return writer().write(mapper, props);
    }

    record Impl<In, Out>(PropertyReader<In> reader, PropertyWriter<Out> writer)
            implements PropertyTypeConverter<In, Out> { }
}
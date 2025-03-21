package org.lime.fastmapper.converter.property;

import org.lime.fastmapper.FastMapper;

import java.util.stream.Stream;

public interface PropertyWriter<Out> {
    Out write(FastMapper mapper, Stream<PropertyContent<?>> properties);
}

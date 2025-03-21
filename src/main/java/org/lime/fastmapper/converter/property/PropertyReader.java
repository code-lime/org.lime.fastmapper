package org.lime.fastmapper.converter.property;

import org.lime.fastmapper.FastMapper;

import java.util.stream.Stream;

public interface PropertyReader<In> {
    Stream<PropertyContent<?>> read(FastMapper mapper, In value);
}


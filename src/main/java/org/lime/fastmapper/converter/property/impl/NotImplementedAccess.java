package org.lime.fastmapper.converter.property.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyAccess;
import org.lime.fastmapper.converter.property.PropertyContent;

import java.util.stream.Stream;

public class NotImplementedAccess<T> implements PropertyAccess<T> {
    private final Class<T> tClass;

    public NotImplementedAccess(Class<T> tClass) {
        this.tClass = tClass;
    }
    @Override
    public Class<T> accessClass() {
        return tClass;
    }

    @Override
    public Stream<PropertyContent<?>> read(FastMapper mapper, T value) {
        throw new NotImplementedException();
    }

    @Override
    public T write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
        throw new NotImplementedException();
    }
}

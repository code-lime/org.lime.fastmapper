package org.lime.fastmapper.converter;

import org.lime.fastmapper.converter.property.PropertyAccess;

public interface PropertyAccessTypeConverter<In, Out> extends
        PropertyTypeConverter<In, Out>,
        ReverseTypeConverter<In, Out> {
    @Override
    PropertyAccess<In> reader();
    @Override
    PropertyAccess<Out> writer();

    record Impl<In, Out>(PropertyAccess<In> reader, PropertyAccess<Out> writer)
            implements PropertyAccessTypeConverter<In, Out> {
        @Override
        public PropertyAccessTypeConverter.Impl<Out, In> reverse() {
            return new PropertyAccessTypeConverter.Impl<>(writer, reader);
        }
    }
}

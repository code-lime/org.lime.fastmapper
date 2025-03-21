package org.lime.fastmapper.converter;

public interface ReverseTypeConverter<In, Out>
        extends TypeConverter<In, Out> {
    ReverseTypeConverter<Out, In> reverse();
}

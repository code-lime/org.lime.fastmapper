package org.lime.fastmapper.converter.property.info;

import org.lime.core.common.utils.execute.Action2;

import java.lang.reflect.Member;

public interface PropertyWriteCreator<T, E extends Member> {
    <Out> T create(PropertyInfo<Out, E> info, boolean optional, Action2<Object, Out> writer);
}

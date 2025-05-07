package org.lime.fastmapper.converter.property.impl;

import com.google.protobuf.Message;
import org.lime.fastmapper.converter.property.info.PropertyInfo;
import org.lime.fastmapper.converter.property.info.PropertyLoader;
import org.lime.fastmapper.converter.property.info.PropertyReadContext;
import org.lime.fastmapper.converter.property.info.PropertyWriteContext;

import java.lang.reflect.Method;

public class PropertyProtoAccess<T extends Message, B extends Message.Builder> extends PropertyBuilderAccess<T, B> {
    public PropertyProtoAccess(Class<T> tClass) {
        super(tClass);
    }

    @Override
    protected void loadProperties(Class<T> tClass, Class<B> tBuilder, PropertyReadContext<PropertyInfo.Read<?, Method>, Method> readsContext, PropertyWriteContext<PropertyInfo.Write<?, Method>, Method> writesContext) {
        PropertyLoader.loadProperties(tClass, tBuilder, readsContext, writesContext);
    }
}

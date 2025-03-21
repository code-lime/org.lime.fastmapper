package org.lime.fastmapper.config;

import org.lime.system.Lazy;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func1;
import org.lime.system.execute.Func2;
import org.lime.fastmapper.FastAccess;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.ReverseTypeConverter;
import org.lime.fastmapper.converter.property.PropertyAccess;
import patch.Native;

import java.lang.reflect.Method;

public class AutoConfig<In, Out> {
    private final FastMapper mapper;
    private final Class<In> inClass;
    private final Class<Out> outClass;
    private Lazy<PropertyAccess<In>> inAccess;
    private Lazy<PropertyAccess<Out>> outAccess;
    private Func2<PropertyAccess<In>, PropertyAccess<Out>, ReverseTypeConverter<In, Out>> converter;

    public AutoConfig(FastMapper mapper, Class<In> inClass, Class<Out> outClass) {
        this.mapper = mapper;
        this.inClass = inClass;
        this.outClass = outClass;
        this.inAccess = Lazy.of(() -> FastAccess.autoAccess(inClass));
        this.outAccess = Lazy.of(() -> FastAccess.autoAccess(outClass));
        this.converter = FastAccess::converter;
    }

    public FastMapper mapper() {
        return mapper;
    }
    public Class<In> inClass() {
        return inClass;
    }
    public Class<Out> outClass() {
        return outClass;
    }
    public Lazy<PropertyAccess<In>> inAccess() {
        return inAccess;
    }
    public Lazy<PropertyAccess<Out>> outAccess() {
        return outAccess;
    }
    public Func2<PropertyAccess<In>, PropertyAccess<Out>, ReverseTypeConverter<In, Out>> converter() {
        return converter;
    }

    public AutoConfig<In, Out> inModify(Func1<PropertyAccess<In>, PropertyAccess<In>> modify) {
        inAccess = Lazy.of(modify.invoke(inAccess().value()));
        return this;
    }
    public AutoConfig<In, Out> outModify(Func1<PropertyAccess<Out>, PropertyAccess<Out>> modify) {
        outAccess = Lazy.of(modify.invoke(outAccess().value()));
        return this;
    }

    public AutoConfig<In, Out> inOverride(PropertyAccess<In> inAccess) {
        this.inAccess = Lazy.of(inAccess);
        return this;
    }
    public AutoConfig<In, Out> outOverride(PropertyAccess<Out> outAccess) {
        this.outAccess = Lazy.of(outAccess);
        return this;
    }
    public AutoConfig<In, Out> converterOverride(Func2<PropertyAccess<In>, PropertyAccess<Out>, ReverseTypeConverter<In, Out>> converter) {
        this.converter = converter;
        return this;
    }

    public <E extends Enum<E>>AutoConfig<In, Out> oneOf(Func1<In, E> enumCase, Action1<OneOfConfig<In, Out, E>> config) {
        Method method = Native.getMethod(Native.infoFromLambda(enumCase));
        Class<E> eClass = (Class<E>)method.getReturnType();
        if (!eClass.isEnum())
            throw new IllegalArgumentException("Lambda return type "+eClass+" not enum");
        var cfg = new OneOfConfig<>(this, eClass);
        config.invoke(cfg);
        return cfg.apply();
    }
}

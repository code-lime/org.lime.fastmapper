package org.lime.fastmapper.config;

import com.google.common.base.CaseFormat;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Streams;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Execute;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.converter.property.PropertyAccess;
import org.lime.fastmapper.converter.property.PropertyContent;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

public class OneOfConfig<In, Out, E extends Enum<E>> {
    private final AutoConfig<In, Out> auto;
    private final Set<E> elements = new LinkedHashSet<>();
    private final Class<E> eClass;
    private final Map<E, PropertyEnum<Out, E, ?>> props = new HashMap<>();
    private final Map<Class<?>, E> typeToEnum = new HashMap<>();
    private final BiMap<String, E> nameBiEnum = HashBiMap.create();

    public OneOfConfig(AutoConfig<In, Out> auto, Class<E> eClass) {
        this.auto = auto;
        this.eClass = eClass;
        Arrays.stream(eClass.getEnumConstants())
                .filter(v -> !v.name().endsWith("_NOT_SET"))
                .forEach(this.elements::add);
    }

    public AutoConfig<In,Out> apply() {
        elements.forEach(v -> {
            if (props.containsKey(v))
                return;
            throw new IllegalArgumentException("Element "+v+" not register");
        });
        return auto
                .outOverride(new PropertyAccess<>() {
                    @Override
                    public Class<Out> accessClass() {
                        return auto.outClass();
                    }
                    @Override
                    public Stream<PropertyContent<?>> read(FastMapper mapper, Out value) {
                        var tClass = value.getClass();
                        E element = Streams.concat(
                                FastMapper.getAllSuperclasses(tClass),
                                FastMapper.getAllInterfaces(tClass))
                                .flatMap(v -> Optional.ofNullable(typeToEnum.get(v)).stream())
                                .findFirst()
                                .orElseThrow();
                        var prop = props.get(element);
                        return Stream.of(new PropertyContent<>(
                                prop.name(),
                                prop.destClass(),
                                prop.destClass(),
                                Lazy.of(value)));
                    }
                    @Override
                    public Out write(FastMapper mapper, Stream<PropertyContent<?>> properties) {
                        var content = properties.findFirst().orElseThrow();
                        E element = nameBiEnum.get(content.name());
                        var prop = props.get(element);
                        return mapper.map(content.value(), prop.destClass());
                    }
                });
    }

    private record PropertyEnum<Out, E extends Enum<E>, T>(
            String name,
            E element,
            Class<T> srcClass,
            Class<Out> destClass) {
    }

    private static final Func1<String, Class<?>> forNameLambda = Execute.<String, Class<?>>funcEx(Class::forName).throwable();

    public OneOfConfig<In, Out, E> withNamed(Func2<E, String, String> converter) {
        return withNamed(converter, null);
    }
    public OneOfConfig<In, Out, E> withNamed(Func2<E, String, String> converter, @Nullable Action2<Class<?>, Class<?>> callback) {
        String prefix = auto.outClass().getPackage().getName();
        final String resultPrefix = prefix.isEmpty() ? prefix : (prefix + ".");
        Class<Out> outClass = auto.outClass();
        return withConvert(v -> {
            String name = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, v.name());
            Class tClass = forNameLambda.invoke(resultPrefix + converter.invoke(v, name));
            if (!outClass.isAssignableFrom(tClass))
                throw new IllegalArgumentException("Class "+tClass+" not casting to "+outClass);
            return tClass;
        }, callback);
    }

    public OneOfConfig<In, Out, E> withConvert(Func1<E, Class<? extends Out>> converter) {
        return withConvert(converter, null);
    }
    public OneOfConfig<In, Out, E> withConvert(Func1<E, Class<? extends Out>> converter, @Nullable Action2<Class<?>, Class<?>> callback) {
        var cfg = this;
        for (E element : elements)
            cfg = cfg.with(converter.invoke(element), element, callback);
        return cfg;
    }

    public <T extends Out>OneOfConfig<In, Out, E> with(Class<T> tClass, E element) {
        return with(tClass, element, null);
    }
    public <T extends Out>OneOfConfig<In, Out, E> with(Class<T> tClass, E element, @Nullable Action2<Class<?>, Class<?>> callback) {
        if (!elements.contains(element))
            throw new IllegalArgumentException("Element '"+element+"' not allow");
        if (props.containsKey(element))
            throw new IllegalArgumentException("Already enum '"+element+"' exist");
        if (typeToEnum.containsKey(tClass))
            throw new IllegalArgumentException("Already type '"+tClass+"' exist");

        String name = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, element.name());
        if (nameBiEnum.containsKey(name))
            throw new IllegalArgumentException("Already name '"+name+"' exist");
        if (nameBiEnum.containsValue(element))
            throw new IllegalArgumentException("Already element '"+element+"' exist");

        var propertyEnum = createProperty(element, name, tClass);
        props.put(element, propertyEnum);
        typeToEnum.put(tClass, element);
        nameBiEnum.put(name, element);
        if (callback != null)
            callback.invoke(propertyEnum.srcClass, propertyEnum.destClass);
        return this;
    }

    private <T extends Out>PropertyEnum<Out, E, ?> createProperty(E element, String name, Class<T> tClass) {
        String javaName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, element.name());
        Class<In> accessClass = auto.inClass();
        var getMethod = ReflectionMethod.of(accessClass, "get" + javaName).target();
        var getMod = getMethod.getModifiers();
        Class<?> tRet = getMethod.getReturnType();
        if (Modifier.isStatic(getMod) || !Modifier.isPublic(getMod) || tRet.equals(void.class))
            throw new IllegalArgumentException("Method " + getMethod + " is not public instance getter");
        return new PropertyEnum(name, element, tRet, tClass);
    }
}

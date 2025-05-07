package org.lime.fastmapper;

import com.google.protobuf.Message;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;
import org.lime.core.common.system.tuple.Tuple;
import org.lime.core.common.system.tuple.Tuple2;
import org.lime.fastmapper.converter.PropertyAccessTypeConverter;
import org.lime.fastmapper.converter.PropertyTypeConverter;
import org.lime.fastmapper.converter.property.PropertyAccess;
import org.lime.fastmapper.converter.property.PropertyReader;
import org.lime.fastmapper.converter.property.PropertyWriter;
import org.lime.fastmapper.converter.property.impl.*;

import java.util.concurrent.ConcurrentHashMap;

public class FastAccess {
    public static <In, Out> PropertyTypeConverter<In, Out> converter(
            PropertyReader<In> reader,
            PropertyWriter<Out> writer) {
        return new PropertyTypeConverter.Impl<>(reader, writer);
    }
    public static <In, Out> PropertyAccessTypeConverter<In, Out> converter(
            PropertyAccess<In> reader,
            PropertyAccess<Out> writer) {
        return new PropertyAccessTypeConverter.Impl<>(reader, writer);
    }

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, PropertyAccess<?>>> cache = new ConcurrentHashMap<>();
    private static <T, A extends PropertyAccess<?>>A accessCache(
            Class<A> aClass,
            Class<T> tClass,
            Func1<Class<T>, A> creator) {
        var typeMap = cache.computeIfAbsent(aClass, _ -> new ConcurrentHashMap<>());
        return (A) typeMap.computeIfAbsent(tClass, _ -> creator.invoke(tClass));
    }

    public static <T extends Enum<T>> PropertyEnumAccess<T> enumAccess(Class<T> tClass) {
        return accessCache(PropertyEnumAccess.class, tClass, PropertyEnumAccess::new);
    }
    public static <T> PropertyStructAccess<T> structAccess(Class<T> tClass) {
        return accessCache(PropertyStructAccess.class, tClass, PropertyStructAccess::new);
    }
    public static <T extends Record> PropertyRecordAccess<T> recordAccess(Class<T> tClass) {
        return accessCache(PropertyRecordAccess.class, tClass, PropertyRecordAccess::new);
    }
    public static <T, B> PropertyBuilderAccess<T, B> builderAccess(Class<T> tClass) {
        return accessCache(PropertyBuilderAccess.class, tClass, PropertyBuilderAccess::new);
    }
    public static <T, B> PropertyBuilderAccess<T, B> builderAccess(Class<T> tClass, @Nullable Func0<B> newBuilderFunc, @Nullable Func1<B, T> buildFunc) {
        return accessCache(PropertyBuilderAccess.class, tClass, v -> new PropertyBuilderAccess<>(v, newBuilderFunc, buildFunc));
    }
    public static <T extends Message, B extends Message.Builder> PropertyProtoAccess<T, B> protoAccess(Class<T> tClass) {
        return accessCache(PropertyProtoAccess.class, tClass, PropertyProtoAccess::new);
    }
    public static <T> PropertyInterfaceAccess<T> interfaceAccess(Class<T> tClass) {
        return accessCache(PropertyInterfaceAccess.class, tClass, PropertyInterfaceAccess::new);
    }
    public static <T> PropertyDefaultAccess<T> defaultAccess(Class<T> tClass) {
        return accessCache(PropertyDefaultAccess.class, tClass, PropertyDefaultAccess::new);
    }

    public static <T> PropertyAccess<T> autoAccess(Class<T> tClass) {
        return accessCache(PropertyAccess.class, tClass, _ -> {
            if (tClass.isEnum())
                return enumAccess((Class<? extends Enum>)tClass);
            if (tClass.isRecord())
                return recordAccess((Class<? extends Record>)tClass);
            if (Message.class.isAssignableFrom(tClass))
                return protoAccess((Class<? extends Message>) tClass);
            if (tClass.isInterface())
                return interfaceAccess(tClass);
            try { return builderAccess(tClass).withPrefixAuto(); } catch (Throwable _) {}
            try { return structAccess(tClass); } catch (Throwable _) {}
            return defaultAccess(tClass).withPrefixAuto();
        });
    }

    public static <In, Out>Tuple2<PropertyAccess<In>, PropertyAccess<Out>> forOneOf(
            FastMapper mapper,
            PropertyAccess<In> inAccess,
            PropertyAccess<Out> outAccess) {
        return Tuple.of(inAccess, outAccess);
    }
}

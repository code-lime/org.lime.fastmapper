package org.lime.fastmapper.converter.property.info;

import com.google.common.base.CaseFormat;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.lime.reflection.Lambda;
import org.lime.reflection.ReflectionMethod;
import org.lime.system.execute.Action2;
import org.lime.system.execute.Execute;
import org.lime.system.execute.Func1;
import org.lime.system.execute.Func2;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple2;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PropertyLoader {
    private static final List<String> getterPrefix = List.of("is", "get");
    private static final List<String> setterPrefix = List.of("set", "addAll", "add", "putAll", "put");
    private static final List<Tuple2<String, Class<?>>> postfix = List.of(
            Tuple.of("Bytes", ByteString.class),
            Tuple.of("Map", Map.class),
            Tuple.of("List", List.class),
            Tuple.of("Value", Integer.TYPE)
    );
    private static String toFirstUpper(String text) {
        if (text.isEmpty())
            return text;
        char first = text.charAt(0);
        if (Character.isUpperCase(first))
            return text;
        first = Character.toUpperCase(first);
        return first + text.substring(1);
    }
    private static Optional<String> readPrefix(String text, String prefix) {
        int prefixLength = prefix.length();
        if (text.startsWith(prefix) && text.length() > prefixLength) {
            char first = text.charAt(prefixLength);
            if (!Character.isUpperCase(first))
                return Optional.empty();
            first = Character.toLowerCase(first);
            text = first + text.substring(prefixLength + 1);
            return Optional.of(text);
        } else {
            return Optional.empty();
        }
    }
    private static Optional<String> readPostfix(String text, Class<?> tClass, Tuple2<String, Class<?>> postfix) {
        return text.endsWith(postfix.val0) && postfix.val1.isAssignableFrom(tClass)
                ? Optional.of(text.substring(0, text.length() - postfix.val0.length()))
                : Optional.empty();
    }

    public static Optional<PropertyInfo<?, Method>> extractInfo(Method method) {
        return extractInfo(method, void.class);
    }
    public static Optional<PropertyInfo<?, Method>> extractInfo(Method method, Class<?> tReturn) {
        if (method.isBridge() || method.getDeclaringClass().equals(Object.class))
            return Optional.empty();
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers))
            return Optional.empty();
        String name = method.getName();
        boolean isGetter;
        switch (method.getParameterCount()) {
            case 0:
                if (method.getReturnType() == void.class || method.getReturnType() == tReturn)
                    return Optional.empty();
                isGetter = true;
                break;
            case 1:
                if (method.getReturnType() != tReturn)
                    return Optional.empty();
                isGetter = false;
                break;
            default:
                return Optional.empty();
        }
        boolean isPrefix = false;
        for (var prefix : getterPrefix) {
            var dat = readPrefix(name, prefix);
            if (dat.isEmpty())
                continue;
            if (!isGetter)
                return Optional.empty();
            isPrefix = true;
            name = dat.get();
            break;
        }
        if (!isPrefix) {
            for (var prefix : setterPrefix) {
                var dat = readPrefix(name, prefix);
                if (dat.isEmpty())
                    continue;
                if (isGetter)
                    return Optional.empty();
                isPrefix = true;
                name = dat.get();
                break;
            }
        }

        Class<?> mClass;
        Type mType;
        if (isGetter) {
            mClass = method.getReturnType();
            mType = method.getGenericReturnType();
        } else {
            mClass = method.getParameterTypes()[0];
            mType = method.getGenericParameterTypes()[0];
        }
        for (var postfix : postfix) {
            var dat = readPostfix(name, mClass, postfix);
            if (dat.isEmpty())
                continue;
            name = dat.get();
            break;
        }
        return Optional.of(new PropertyInfo.Impl(name, isGetter, isPrefix, method, mType, mClass));
    }

    public static <T, R, W>void loadProperties(
            Class<T> tClass,
            @Nullable PropertyReadContext<R, Method> reads,
            @Nullable PropertyWriteContext<W, Method> writes) {
        loadProperties(tClass, reads, writes, void.class);
    }
    public static <T, R, W>void loadProperties(
            Class<T> tClass,
            @Nullable PropertyReadContext<R, Method> reads,
            @Nullable PropertyWriteContext<W, Method> writes,
            Class<?> tReturn) {
        for (Method method : tClass.getMethods()) {
            extractInfo(method, tReturn)
                    .ifPresent(info -> {
                        ReflectionMethod hasReaderMethod = null;
                        try {
                            hasReaderMethod = ReflectionMethod.of(tClass, "has" + toFirstUpper(info.name()));
                        } catch (Throwable _) {

                        }

                        if (info.getter()) {
                            if (reads != null) {
                                Func1<Object, Boolean> hasReader = hasReaderMethod == null ? null : hasReaderMethod.lambda(Func1.class);
                                reads.add(info, hasReader, Lambda.lambda(method, Func1.class));
                            }
                        } else {
                            if (writes != null) {
                                Action2 write;
                                if (tReturn == void.class) {
                                    write = Lambda.lambda(method, Action2.class);
                                } else {
                                    Func2<Object, Object, Object> inv = Lambda.lambda(method, Func2.class);
                                    write = Execute.action(inv::invoke);
                                }
                                writes.add(info, hasReaderMethod != null, write);
                            }
                        }
                    });
        }
    }
    public static final Func1<Descriptors.FieldDescriptor, Boolean> hasOptionalKeyword = ReflectionMethod.of(Descriptors.FieldDescriptor.class, "hasOptionalKeyword").lambda(Func1.class);
    public static <T extends Message, B extends Message.Builder, R, W>void loadProperties(
            Class<T> tClass,
            Class<B> bClass,
            @Nullable PropertyReadContext<R, Method> reads,
            @Nullable PropertyWriteContext<W, Method> writes) {
        Descriptors.Descriptor descriptor = (Descriptors.Descriptor)ReflectionMethod
                .of(tClass, "getDescriptor")
                .call(new Object[0]);
        var fields = descriptor.getFields();

        for (var field : fields) {
            String javaName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, field.getName());
            String name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, field.getName());
            String postfix;
            if (field.isMapField()) postfix = "Map";
            else if (field.isRepeated()) postfix = "List";
            else postfix = "";
            Method getMethod = ReflectionMethod.of(tClass, "get" + javaName + postfix).method();
            Type genType = getMethod.getGenericReturnType();;
            Class<?> type = getMethod.getReturnType();

            Type genTypeSet;
            Class<?> typeSet = type;
            String setPrefix;
            if (field.isMapField()) setPrefix = "putAll";
            else if (field.isRepeated()) {
                typeSet = Iterable.class;
                setPrefix = "addAll";
            }
            else setPrefix = "set";
            Method setMethod = ReflectionMethod.of(bClass, setPrefix + javaName, typeSet).method();
            var getValue = Lambda.lambda(getMethod, Func1.class);
            var setValue = Lambda.lambda(setMethod, Func2.class);
            Func1<Object, Boolean> hasValue;
            if (hasOptionalKeyword.invoke(field) || field.getContainingOneof() != null) {
                Method hasMethod = ReflectionMethod.of(tClass, "has" + javaName).method();
                hasValue = Lambda.lambda(hasMethod, Func1.class);
            } else {
                hasValue = null;
            }
            genTypeSet = setMethod.getGenericParameterTypes()[0];
            boolean optional = hasValue != null;
            reads.add(new PropertyInfo.Impl(name, true, true, getMethod, genType, type), hasValue, getValue);
            writes.add(new PropertyInfo.Impl(name, false, true, setMethod, genTypeSet, typeSet), optional, (a,b) -> setValue.invoke(a,b));
        }
    }
}

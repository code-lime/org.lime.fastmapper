package org.lime.fastmapper.reflection;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.lime.core.common.system.execute.Func1;

import java.lang.reflect.*;
import java.util.*;

public class GenericUtils {
    private static <T>List<T> getAllRecursive(Class<?> tClass, Func1<Class<?>, T[]> extract) {
        final List<Class<?>> classes = getAllSuperclassesAndInterfaces(tClass);
        classes.addFirst(tClass);
        final List<T> resultElements = new ArrayList<>();
        classes.forEach(current -> {
            final T[] methods = extract.apply(current);
            Collections.addAll(resultElements, methods);
        });
        return resultElements;
    }
    public static List<Method> getAllMethods(Class<?> tClass) {
        return getAllRecursive(tClass, Class::getDeclaredMethods);
    }
    public static List<Field> getAllFields(Class<?> tClass) {
        return getAllRecursive(tClass, Class::getDeclaredFields);
    }
    public static List<Constructor<?>> getAllConstructors(Class<?> tClass) {
        return getAllRecursive(tClass, Class::getDeclaredConstructors);
    }

    public static List<Class<?>> getAllSuperclassesAndInterfaces(final Class<?> tClass) {
        if (tClass == null)
            return null;

        final List<Class<?>> allSuperClassesAndInterfaces = new ArrayList<>();
        final List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(tClass);
        int superClassIndex = 0;
        final List<Class<?>> allInterfaces = ClassUtils.getAllInterfaces(tClass);
        int interfaceIndex = 0;
        while (interfaceIndex < allInterfaces.size() ||
                superClassIndex < allSuperclasses.size()) {
            final Class<?> current;
            if (interfaceIndex >= allInterfaces.size()) {
                current = allSuperclasses.get(superClassIndex++);
            } else if (superClassIndex >= allSuperclasses.size() || !(superClassIndex < interfaceIndex)) {
                current = allInterfaces.get(interfaceIndex++);
            } else {
                current = allSuperclasses.get(superClassIndex++);
            }
            allSuperClassesAndInterfaces.add(current);
        }
        return allSuperClassesAndInterfaces;
    }

    public static Type resolveTypeInContext(Type toResolve, Class<?> contextClass, Class<?> declaringClass) {
        Map<TypeVariable<?>, Type> map = TypeUtils.getTypeArguments(contextClass, declaringClass);
        if (map.isEmpty())
            return toResolve;
        return TypeUtils.unrollVariables(map, toResolve);
    }
    public static Class<?> readRawClass(Type type) {
        return switch (type) {
            case ParameterizedType pt -> readRawClass(pt.getRawType());
            case TypeVariable<?> tv -> readRawClass(tv.getBounds()[0]);
            case GenericArrayType gat -> Array.newInstance(readRawClass(gat.getGenericComponentType()), 0).getClass();
            case WildcardType wt -> {
                Type[] lowerBounds = wt.getLowerBounds();
                yield lowerBounds.length > 0
                        ? readRawClass(lowerBounds[0])
                        : readRawClass(wt.getUpperBounds()[0]);
            }
            case Class<?> tc -> tc;
            default -> throw new IllegalArgumentException("Type '" + type + "' (" + type.getClass() + ") not supported");
        };
    }

    public static Map<TypeVariable<?>, Type> getExecutableTypeArguments(Type declaringType, Executable executable) {
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(declaringType, executable.getDeclaringClass());

        for (TypeVariable<?> typeParameter : executable.getTypeParameters()) {
            if (typeArguments.containsKey(typeParameter))
                continue;
            Type any = TypeUtils.unrollVariables(typeArguments, typeParameter);
            typeArguments.put(typeParameter, Objects.requireNonNullElse(any, Object.class));
        }

        return typeArguments;
    }
}

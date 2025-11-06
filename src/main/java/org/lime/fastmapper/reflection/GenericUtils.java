package org.lime.fastmapper.reflection;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.lime.core.common.utils.execute.Func1;

import java.lang.reflect.*;
import java.util.*;

public class GenericUtils {
    private static <T>Set<T> getAllRecursive(Class<?> tClass, Func1<Class<?>, T[]> extract) {
        final Set<Class<?>> classes = getAllSuperclassesAndInterfaces(tClass);
        classes.add(tClass);
        final Set<T> resultElements = new HashSet<>();
        classes.forEach(current -> {
            final T[] methods = extract.apply(current);
            Collections.addAll(resultElements, methods);
        });
        return resultElements;
    }
    public static Set<Method> getAllMethods(Class<?> tClass) {
        var methods = getAllRecursive(tClass, Class::getDeclaredMethods);
        Set<Method> removes = new HashSet<>();
        methods.forEach(v -> {
            var hierarchy = MethodUtils.getOverrideHierarchy(v, ClassUtils.Interfaces.INCLUDE);
            boolean first = true;
            for (var method : hierarchy) {
                if (first) first = false;
                else removes.add(method);
            }
        });
        methods.removeAll(removes);
        return methods;
    }
    public static Set<Field> getAllFields(Class<?> tClass) {
        return getAllRecursive(tClass, Class::getDeclaredFields);
    }
    public static Set<Constructor<?>> getAllConstructors(Class<?> tClass) {
        return getAllRecursive(tClass, Class::getDeclaredConstructors);
    }

    public static Set<Class<?>> getAllSuperclassesAndInterfaces(final Class<?> tClass) {
        if (tClass == null)
            return null;

        final Set<Class<?>> allSuperClassesAndInterfaces = new HashSet<>();
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

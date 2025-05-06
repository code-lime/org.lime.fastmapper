package org.lime.fastmapper;

import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.*;
import java.util.Map;
import java.util.Optional;

public record GenTypePair<In, Out>(TypePair<In, Out> pair, Type inGen, Type outGen) {
    public static GenTypePair<?, ?> of(Type in, Type out) {
        Class<?> tIn = readRawClass(in);
        Class<?> tOut = readRawClass(out);
        return new GenTypePair<>(TypePair.of(tIn, tOut), in, out);
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

    public Optional<GenTypePair<?, Out>> cast(Class<?> newInClass) {
        Class<?> currentInRaw = TypeUtils.getRawType(inGen, null);
        if (currentInRaw.equals(newInClass))
            return Optional.of(new GenTypePair<>(TypePair.of(newInClass, pair.tOut()), inGen, outGen));
        if (!newInClass.isAssignableFrom(currentInRaw)) {
            return Optional.empty();
        }
        Map<TypeVariable<?>, Type> typeArgMapping = TypeUtils.getTypeArguments(inGen, newInClass);
        Type resolvedInGen;
        if (newInClass.getTypeParameters().length > 0) {
            TypeVariable<?>[] typeVars = newInClass.getTypeParameters();
            Type[] resolvedArgs = new Type[typeVars.length];
            for (int i = 0; i < typeVars.length; i++) {
                Type arg = typeArgMapping.get(typeVars[i]);
                resolvedArgs[i] = (arg != null) ? arg : typeVars[i];
            }
            resolvedInGen = TypeUtils.parameterize(newInClass, resolvedArgs);
        } else {
            resolvedInGen = newInClass;
        }
        return Optional.of(new GenTypePair<>(TypePair.of(newInClass, pair.tOut()), resolvedInGen, outGen));
    }
}

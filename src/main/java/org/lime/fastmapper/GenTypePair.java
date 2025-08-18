package org.lime.fastmapper;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.lime.fastmapper.reflection.GenericUtils;

import java.lang.reflect.*;
import java.util.Optional;

public record GenTypePair<In, Out>(TypePair<In, Out> pair, Type inGen, Type outGen) {
    public static GenTypePair<?, ?> of(Type in, Type out) {
        Class<?> tIn = GenericUtils.readRawClass(in);
        Class<?> tOut = GenericUtils.readRawClass(out);
        return new GenTypePair<>(TypePair.of(tIn, tOut), in, out);
    }
    public Optional<GenTypePair<?, Out>> cast(Class<?> newInClass) {
        Class<?> currentInRaw = TypeUtils.getRawType(inGen, null);
        if (currentInRaw.equals(newInClass))
            return Optional.of(new GenTypePair<>(TypePair.of(newInClass, pair.tOut()), inGen, outGen));
        if (!newInClass.isAssignableFrom(currentInRaw))
            return Optional.empty();

        TypeVariable<?>[] typeVars = newInClass.getTypeParameters();

        Type resolvedInGen = typeVars.length == 0
                ? newInClass
                : TypeUtils.unrollVariables(TypeUtils.getTypeArguments(inGen, newInClass), TypeUtils.parameterize(newInClass, typeVars));

        return Optional.of(new GenTypePair<>(TypePair.of(newInClass, pair.tOut()), resolvedInGen, outGen));
    }
}

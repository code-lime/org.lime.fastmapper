package org.lime.fastmapper;

import com.google.common.primitives.Primitives;

import java.lang.reflect.Type;

public record TypePair<In, Out>(Class<In> tIn, Class<Out> tOut) {
    public static <In, Out> TypePair<In, Out> of(Class<In> tIn, Class<Out> tOut) {
        return new TypePair<>(Primitives.wrap(tIn), Primitives.wrap(tOut));
    }

    public TypePair<Out, In> reverse() {
        return of(tOut, tIn);
    }

    public boolean isClone() {
        return tIn() == tOut();
    }

    public GenTypePair<In, Out> simpleGen() {
        return new GenTypePair<>(this, tIn, tOut);
    }

    public GenTypePair<In, Out> gen(Type inGen, Type outGen) {
        return new GenTypePair<>(this, inGen, outGen);
    }
}


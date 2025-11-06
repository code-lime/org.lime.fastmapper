package org.lime.fastmapper.converter.impl;

import org.lime.core.common.utils.tuple.Tuple2;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.GenTypePair;
import org.lime.fastmapper.converter.TypeConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

public class CollectionTypeConverter implements TypeConverter<Collection, Collection> {
    @Override
    public Collection convert(FastMapper mapper, Collection value) {
        throw new IllegalArgumentException("Type support only generic");
    }

    @Override
    public Collection convert(FastMapper mapper, Collection value, Type inType, Type outType) {
        ParameterizedType genInType = (ParameterizedType) inType;
        ParameterizedType genOutType = (ParameterizedType) outType;

        var tIn = genInType.getActualTypeArguments();
        var tOut = genOutType.getActualTypeArguments();

        GenTypePair gen = GenTypePair.of(tIn[0], tOut[0]);

        Collection list = new ArrayList();

        Tuple2<TypeConverter, GenTypePair<?,?>> kv = mapper.converter(gen);

        value.forEach(v -> list.add(kv.val0.convert(mapper, v, kv.val1.inGen(), kv.val1.outGen())));

        return list;
    }
}

package org.lime.fastmapper.converter.impl;

import org.lime.system.tuple.Tuple2;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.GenTypePair;
import org.lime.fastmapper.converter.TypeConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ListTypeConverter implements TypeConverter<List, List> {
    @Override
    public List convert(FastMapper mapper, List value) {
        throw new IllegalArgumentException("Type support only generic");
    }

    @Override
    public List convert(FastMapper mapper, List value, Type inType, Type outType) {
        ParameterizedType genInType = (ParameterizedType) inType;
        ParameterizedType genOutType = (ParameterizedType) outType;

        var tIn = genInType.getActualTypeArguments();
        var tOut = genOutType.getActualTypeArguments();

        GenTypePair gen = GenTypePair.of(tIn[0], tOut[0]);

        List list = new ArrayList();

        Tuple2<TypeConverter, GenTypePair<?,?>> kv = mapper.converter(gen);

        value.forEach(v -> list.add(kv.val0.convert(mapper, v, kv.val1.inGen(), kv.val1.outGen())));

        return list;
    }
}

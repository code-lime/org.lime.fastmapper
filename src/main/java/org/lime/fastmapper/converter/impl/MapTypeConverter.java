package org.lime.fastmapper.converter.impl;

import org.lime.core.common.utils.tuple.Tuple2;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.GenTypePair;
import org.lime.fastmapper.converter.TypeConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class MapTypeConverter implements TypeConverter<Map, Map> {
    @Override
    public Map convert(FastMapper mapper, Map value) {
        throw new IllegalArgumentException("Type support only generic");
    }

    @Override
    public Map convert(FastMapper mapper, Map value, Type inType, Type outType) {
        ParameterizedType genInType = (ParameterizedType) inType;
        ParameterizedType genOutType = (ParameterizedType) outType;

        var tIn = genInType.getActualTypeArguments();
        var tOut = genOutType.getActualTypeArguments();

        GenTypePair genKey = GenTypePair.of(tIn[0], tOut[0]);
        GenTypePair genValue = GenTypePair.of(tIn[1], tOut[1]);

        Map map = new HashMap();

        Tuple2<TypeConverter, GenTypePair<?,?>> kvKey = mapper.converter(genKey);
        Tuple2<TypeConverter, GenTypePair<?,?>> kvValue = mapper.converter(genValue);

        value.forEach((k, v) -> map.put(
                kvKey.val0.convert(mapper, k, kvKey.val1.inGen(), kvKey.val1.outGen()),
                kvValue.val0.convert(mapper, v, kvValue.val1.inGen(), kvValue.val1.outGen())));

        return map;
    }
}

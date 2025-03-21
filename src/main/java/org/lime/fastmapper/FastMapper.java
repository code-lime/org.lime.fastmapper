package org.lime.fastmapper;

import com.google.common.collect.Streams;
import com.google.common.primitives.Primitives;
import com.google.protobuf.LazyStringArrayList;
import org.lime.system.execute.Func1;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple1;
import org.lime.system.tuple.Tuple2;
import org.lime.fastmapper.config.AutoConfig;
import org.lime.fastmapper.converter.ReverseTypeConverter;
import org.lime.fastmapper.converter.SimpleTypeConverter;
import org.lime.fastmapper.converter.TypeConverter;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class FastMapper {
    private final Map<TypePair<?,?>, TypeConverter<?,?>> mappers = new ConcurrentHashMap<>();
    private final Map<GenTypePair<?,?>, GenTypePair<?,?>> extendMap = new ConcurrentHashMap<>();

    private FastMapper() {

    }

    public Iterable<TypePair<?,?>> keys() {
        return mappers.keySet();
    }

    public static FastMapper create() {
        FastMapper mapper = new FastMapper();
        Primitives.allWrapperTypes().forEach(mapper::addPrimitive);
        mapper.addPrimitive(String.class);
        return mapper;
    }

    private <T>void addPrimitive(Class<T> tClass) {
        if (tClass.equals(Void.class))
            return;
        addSimpleClone(tClass, v -> v);
    }
    public <T>FastMapper addSimpleClone(Class<T> tClass, SimpleTypeConverter<T, T> converter) {
        return add(TypePair.of(tClass, tClass), converter);
    }
    public <In, Out>FastMapper add(TypePair<In, Out> key, TypeConverter<In, Out> converter) {
        if (mappers.containsKey(key))
            throw new IllegalArgumentException("Mapper key "+key+" already register");
        mappers.put(key, converter);
        extendMap.clear();
        return this;
    }
    public <In, Out>FastMapper addReverse(TypePair<In, Out> key, ReverseTypeConverter<In, Out> converter) {
        FastMapper mapper = add(key, converter);
        if (key.isClone())
            return mapper;
        return mapper.add(key.reverse(), converter.reverse());
    }
    public <In, Out>FastMapper addReverse(TypePair<In, Out> key, TypeConverter<In, Out> converter, TypeConverter<Out, In> reverseConverter) {
        return this
                .add(key, converter)
                .add(key.reverse(), reverseConverter);
    }
    public <In, Out>FastMapper addAuto(TypePair<In, Out> key) {
        return addAuto(key, null);
    }
    public <In, Out>FastMapper addAuto(
            TypePair<In, Out> key,
            @Nullable Func1<AutoConfig<In, Out>, AutoConfig<In, Out>> config) {

        var dat = new AutoConfig<>(this, key.tIn(), key.tOut());
        if (config != null)
            dat = config.invoke(dat);

        return addReverse(key, dat.converter().invoke(dat.inAccess().value(), dat.outAccess().value()));
    }

    private static final GenTypePair<?,?> none = GenTypePair.of(Void.class, Void.class);

    public static Stream<Class<?>> getAllInterfaces(Class<?> clazz) {
        return Arrays.stream(clazz.getInterfaces())
                .flatMap(i -> Stream.concat(Stream.of(i), getAllInterfaces(i)));
    }
    public static Stream<Class<?>> getAllSuperclasses(Class<?> clazz) {
        return Stream.iterate(clazz, c -> c != null && c != Object.class, Class::getSuperclass);
    }
    public static Stream<Class<?>> getAll(Class<?> clazz) {
        return getAllSuperclasses(clazz).flatMap(v -> Streams.concat(Stream.of(v), getAllInterfaces(v)));
    }

    private @Nullable GenTypePair<?,?> findExtendKey(GenTypePair<?,?> key) {
        return extendMap.computeIfAbsent(key, gen -> {
            TypePair<?,?> v = gen.pair();
            var find = getAll(v.tIn())
                    .flatMap(t -> gen.cast(t).stream())
                    .filter(t -> mappers.containsKey(t.pair()))
                    .findAny();
            return find.isEmpty() ? none : find.get();
        });
    }

    /**
     * Рекурсивно ищет тип, соответствующий целевому классу target,
     * начиная с переданного type.
     *
     * @param type   Начальный тип (например, из getGenericInterfaces() или getGenericSuperclass()).
     * @param target Целевой класс или интерфейс (например, Map.class).
     * @return Optional с найденным типом или Optional.empty(), если тип не найден.
     */
    public static Optional<Type> getGenericType(Type type, Class<?> target) {
        // Если type является параметризованным, проверяем его сырой тип.
        if (type instanceof ParameterizedType pt) {
            if (pt.getRawType() instanceof Class) {
                // Если сырой тип совпадает, возвращаем найденный тип.
                if (target.equals(pt.getRawType())) {
                    return Optional.of(type);
                }
                // Если не совпадает, пытаемся найти target в иерархии сырого типа.
                Optional<Type> result = getGenericType(pt.getRawType(), target);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        // Если type является классом, рекурсивно ищем в его интерфейсах и суперклассе.
        if (type instanceof Class<?> current) {
            // Проверяем интерфейсы
            for (Type iface : current.getGenericInterfaces()) {
                Optional<Type> result = getGenericType(iface, target);
                if (result.isPresent()) {
                    return result;
                }
            }
            // Проверяем суперкласс
            Type superType = current.getGenericSuperclass();
            if (superType != null) {
                Optional<Type> result = getGenericType(superType, target);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    public <In, Out> Tuple2<TypeConverter<In, Out>, GenTypePair<?,?>> converter(GenTypePair<In, Out> key) {
        var converter = mappers.get(key.pair());
        GenTypePair<?,?> result = key;
        if (converter == null) {
            var extend = findExtendKey(key);
            if (extend != null) {
                converter = mappers.get(extend.pair());
                result = extend;
            }
        }
        if (converter == null)
            throw new IllegalArgumentException("Mapper key "+key+" not register");
        return Tuple.of((TypeConverter<In, Out>)converter, result);
    }

    public <In, Out>Out map(TypePair<In, Out> key, In value) {
        if (value == null)
            return null;
        return map(key, value, key.tIn(), key.tOut());
    }
    public <In, Out>Out map(TypePair<In, Out> key, In value, Type inType, Type outType) {
        if (value == null)
            return null;
        var converter = mappers.get(key);
        if (converter == null) {
            var extend = findExtendKey(key.gen(inType, outType));
            if (extend != null) {
                converter = mappers.get(extend.pair());
                inType = extend.inGen();
                outType = extend.outGen();
            }
        }
        if (converter == null)
            throw new IllegalArgumentException("Mapper key "+key+" not register");
        return ((TypeConverter<In, Out>)converter).convert(this, value, inType, outType);
    }
    public <In, Out>Out map(In value, Class<Out> tOut) {
        if (value == null)
            return null;
        var key = TypePair.of((Class<In>) value.getClass(), tOut);
        return map(key, value);
    }
    public <In, Out>Out map(In value, Class<Out> tOut, Type inType, Type outType) {
        if (value == null)
            return null;
        var key = TypePair.of((Class<In>) value.getClass(), tOut);
        return map(key, value, inType, outType);
    }
    public <In, Out>Optional<Tuple1<Out>> tryMap(TypePair<In, Out> key, In value) {
        return tryMap(key, value, key.tIn(), key.tOut());
    }
    public <In, Out>Optional<Tuple1<Out>> tryMap(TypePair<In, Out> key, In value, Type inType, Type outType) {
        if (value == null)
            return Optional.of(Tuple.of(null));
        var converter = mappers.get(key);
        if (converter == null) {
            var extend = findExtendKey(key.gen(inType, outType));
            if (extend != null) {
                converter = mappers.get(extend.pair());
                inType = extend.inGen();
                outType = extend.outGen();
            }
        }
        if (converter == null)
            return Optional.empty();
        return Optional.of(Tuple.of(((TypeConverter<In, Out>)converter).convert(this, value, inType, outType)));
    }
    public <In, Out>Optional<Tuple1<Out>> tryMap(In value, Class<Out> tOut) {
        if (value == null)
            return Optional.of(Tuple.of(null));
        var key = TypePair.of((Class<In>) value.getClass(), tOut);
        return tryMap(key, value);
    }
    public <In, Out>Optional<Tuple1<Out>> tryMap(In value, Class<Out> tOut, Type inType, Type outType) {
        if (value == null)
            return Optional.of(Tuple.of(null));
        var key = TypePair.of((Class<In>) value.getClass(), tOut);
        return tryMap(key, value, inType, outType);
    }
}

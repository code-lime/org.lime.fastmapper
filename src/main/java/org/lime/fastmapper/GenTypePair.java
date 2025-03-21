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


    /**
     * Пытается привести текущий in‑тип к новому типу newInClass с сохранением информации о всех generic‑параметрах.
     * Если newInClass не является суперклассом или интерфейсом текущего in‑типа, возвращается Optional.empty().
     *
     * @param newInClass целевой тип, к которому приводим in‑тип (без параметризованных аргументов)
     * @return Optional с новым GenTypePair, если приведение удалось, иначе Optional.empty()
     */
    public Optional<GenTypePair<?, Out>> cast(Class<?> newInClass) {
        // Определяем сырой класс текущего in‑типа с использованием TypeUtils
        Class<?> currentInRaw = TypeUtils.getRawType(inGen, null);
        if (currentInRaw.equals(newInClass))
            return Optional.of(new GenTypePair<>(TypePair.of(newInClass, pair.tOut()), inGen, outGen));
        // Проверяем, что целевой класс newInClass является суперклассом или интерфейсом текущего типа
        if (!newInClass.isAssignableFrom(currentInRaw)) {
            return Optional.empty();
        }
        // Получаем сопоставление type-переменных: маппинг из type-переменных целевого типа в реальные типы
        Map<TypeVariable<?>, Type> typeArgMapping = TypeUtils.getTypeArguments(inGen, newInClass);
        Type resolvedInGen;
        if (newInClass.getTypeParameters().length > 0) {
            // Если целевой тип параметризован, подставляем все его generic-параметры
            TypeVariable<?>[] typeVars = newInClass.getTypeParameters();
            Type[] resolvedArgs = new Type[typeVars.length];
            for (int i = 0; i < typeVars.length; i++) {
                // Если маппинг содержит значение для type-переменной, используем его, иначе оставляем саму переменную
                Type arg = typeArgMapping.get(typeVars[i]);
                resolvedArgs[i] = (arg != null) ? arg : typeVars[i];
            }
            resolvedInGen = TypeUtils.parameterize(newInClass, resolvedArgs);
        } else {
            resolvedInGen = newInClass;
        }
        // Возвращаем новый экземпляр GenTypePair с обновлённой in‑частью (сырой класс и разрешённый generic тип)
        return Optional.of(new GenTypePair<>(TypePair.of(newInClass, pair.tOut()), resolvedInGen, outGen));
    }
}
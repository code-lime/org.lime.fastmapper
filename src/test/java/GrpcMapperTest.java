import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import common.*;
import common.FontData;
import common.RgbColor;
import common.entries.Anchor;
import common.entries.RawEntry;
import common.entries.TextEntry;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.jupiter.api.Test;
import org.lime.fastmapper.RandomProto;
import org.lime.fastmapper.test.protobuf.common.Common;
import org.lime.fastmapper.test.protobuf.oneof.Entry;
import org.lime.system.execute.Execute;
import org.lime.system.tuple.Tuple;
import org.lime.fastmapper.FastMapper;
import org.lime.fastmapper.TypePair;
import org.lime.fastmapper.converter.impl.DynamicIterableTypeConverter;
import org.lime.fastmapper.converter.impl.MapTypeConverter;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GrpcMapperTest {
    private final FastMapper mapper;
    public GrpcMapperTest() {
        mapper = FastMapper.create()
                .addAuto(TypePair.of(SlotId.class, SlotId.class))
                .addAuto(TypePair.of(SlotIdTmp.class, SlotId.class))
                .addAuto(TypePair.of(Common.SlotId.class, SlotId.class))
                .addAuto(TypePair.of(Common.SlotRange.class, SlotRange.class))
                .addReverse(TypePair.of(Common.Guid.class, UUID.class),
                        (_,v) -> new UUID(v.getMostSigBits(),v.getLeastSigBits()),
                        (_,v) -> Common.Guid.newBuilder()
                                .setMostSigBits(v.getMostSignificantBits())
                                .setLeastSigBits(v.getLeastSignificantBits())
                                .build())
                .add(TypePair.of(Map.class, Map.class), new MapTypeConverter())
                .add(TypePair.of(List.class, List.class), new DynamicIterableTypeConverter<>())
                .add(TypePair.of(Collection.class, Collection.class), new DynamicIterableTypeConverter<>())
                .add(TypePair.of(Iterable.class, Iterable.class), new DynamicIterableTypeConverter<>())
                .addAuto(TypePair.of(Common.Font.class, FontData.class),
                        v -> v
                                .inModify(vv -> vv
                                        .<Map<Integer, String>>modifyRead(Execute.func(Common.Font::getCharWidthMap), content ->
                                                content.map(TypeUtils.parameterize(Map.class, Character.class, Integer.class), value -> value
                                                        .entrySet()
                                                        .stream()
                                                        .flatMap(kv -> kv.getValue()
                                                                .chars()
                                                                .mapToObj(ch -> Tuple.of((char)ch, kv.getKey())))
                                                        .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1)))))
                                .outModify(vv -> vv
                                        .<Map<Character, Integer>>modifyRead(Execute.func(FontData::charWidth), content ->
                                                content.map(TypeUtils.parameterize(Map.class, Integer.class, String.class), value -> value
                                                        .entrySet()
                                                        .stream()
                                                        .collect(Collectors.groupingBy(Map.Entry::getValue))
                                                        .entrySet()
                                                        .stream()
                                                        .collect(Collectors.toMap(kv -> kv.getKey(), kv -> kv.getValue()
                                                                .stream()
                                                                .map(Map.Entry::getKey)
                                                                .sorted()
                                                                .map(Object::toString)
                                                                .collect(Collectors.joining())))))))
                .addAuto(TypePair.of(Common.RgbColor.class, RgbColor.class))
                .addAuto(TypePair.of(Entry.Offset.class, Vector2i.class));
        mapper
                .addAuto(TypePair.of(Entry.Text.Anchor.class, Anchor.class))
                .addAuto(TypePair.of(Entry.class, common.entries.Entry.class), v -> v
                        .oneOf(Entry::getTypeCase, vv -> vv
                                .withNamed((_,name) -> name + "Entry", (a,b) -> mapper.addAuto(TypePair.of(a,b)))));
    }
    private static class SlotIdTmp {
        private int x;
        private int y;

        public int getX() {
            return x;
        }
        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }
        public void setY(int y) {
            this.y = y;
        }
    }

    private <In, Out>void testInverseMap(In input, Class<Out> outClass) {
        testInverseMap(input, (Class<In>) input.getClass(), outClass);
    }
    private <In, Out>void testInverseMap(In input, Class<In> inClass, Class<Out> outClass) {
        In compare = input;
        TypePair<In, Out> key = TypePair.of(inClass, outClass);
        var reverseKey = key.reverse();
        for (int i = 0; i < 2; i++) {
            Out output = mapper.map(key, compare);
            compare = mapper.map(reverseKey, output);
        }
        assertEquals(input, compare);
    }

    @Test
    void testOverride() {
        FastMapper mapper = FastMapper.create();

        mapper.addReverse(TypePair.of(Integer.class, String.class),
                (_,v) -> v.toString(),
                (_,v) -> Integer.parseInt(v));
        assertEquals("10", mapper.map(10, String.class));
        assertEquals(45, mapper.map("45", Integer.class));

        try (var _ = mapper.override()) {
            mapper.addReverse(TypePair.of(Integer.class, String.class),
                    (_,v) -> "PREFIX:" + v.toString(),
                    (_,v) -> Integer.parseInt(v.split(":", 2)[1]));
            assertEquals("PREFIX:10", mapper.map(10, String.class));
            assertEquals(45, mapper.map("PREFIX:45", Integer.class));
        }

        try (var _ = mapper.override()) {
            try (var _ = mapper.override()) {
                mapper.addReverse(TypePair.of(Integer.class, String.class),
                        (_,v) -> "PREFIX:" + v.toString(),
                        (_,v) -> Integer.parseInt(v.split(":", 2)[1]));
                assertEquals("PREFIX:10", mapper.map(10, String.class));
                assertEquals(45, mapper.map("PREFIX:45", Integer.class));
            }
        }

        assertThrows(IllegalArgumentException.class,
                () -> mapper.addReverse(TypePair.of(Integer.class, String.class),
                        (_,v) -> "THROW:" + v.toString(),
                        (_,v) -> Integer.parseInt(v.split(":", 2)[1])));
    }
    @Test
    void testClone() {
        testInverseMap(new SlotId(2399, 991), SlotId.class);
    }
    @Test
    void testDefault() {
        testInverseMap(new SlotId(2399, 991), SlotIdTmp.class);
    }
    @Test
    void testBuilder() {
        testInverseMap(new SlotId(2399, 991), Common.SlotId.class);
    }
    @Test
    void testSubBuilder() {
        testInverseMap(new SlotRange(new SlotId(789, 345), new SlotId(1789, 1345)), Common.SlotRange.class);
    }
    @Test
    void testCustom() {
        testInverseMap(UUID.randomUUID(), UUID.class, Common.Guid.class);
    }
    @Test
    void testRecordToBuilder() {
        testInverseMap(new RgbColor(0.5f, 0.2f, 0.0f), Common.RgbColor.class);
    }
    @Test
    void testInterface() {
        testInverseMap(Entry.Raw.newBuilder()
                .setComponent("AABBCC")
                .build(), RawEntry.class);
    }
    @Test
    void testCollection() {
        testInverseMap(Common.Font.newBuilder()
                .setKey("minecraft:v9uf96f9uc9uv")
                .setHeight(6)
                .setDefaultWidth(7789)
                .putCharWidth(6, "123")
                .putCharWidth(7, "4567")
                .putCharWidth(8, "89")
                .putCharWidth(9, "0")
                .build(), Common.Font.class, FontData.class);
    }
    @Test
    void testOptional() {
        Entry.Text.Builder builder = Entry.Text.newBuilder()
                .setFont("minecraft:aabbcc")
                .setColor(Common.RgbColor.newBuilder()
                        .setR(0.5f)
                        .setG(0.2f)
                        .setB(0.0f)
                        .build())
                .setShadow(Common.RgbColor.newBuilder()
                        .setR(0.2f)
                        .setG(0.5f)
                        .setB(0.0f)
                        .build())
                .setScrollLine(7)
                .setAnchor(Entry.Text.Anchor.Bottom)
                .setMaxWidth(1)
                .setMaxLines(2)
                .addAllLines(List.of("a","b", "c"))
                .setOffset(Entry.Offset.newBuilder()
                        .setX(78)
                        .setY(77)
                        .build());
        testInverseMap(builder.build(), Entry.Text.class, TextEntry.class);
        builder.clearShadow();
        testInverseMap(builder.build(), Entry.Text.class, TextEntry.class);
        builder.clearMaxLines();
        testInverseMap(builder.build(), Entry.Text.class, TextEntry.class);
    }
    @Test
    void testOneOf() {
        testInverseMap(Entry.newBuilder()
                .setRaw(Entry.Raw.newBuilder()
                        .setComponent("yv.yv9yg76788889")
                        .build())
                .build(), Entry.class, common.entries.Entry.class);
        testInverseMap(Entry.newBuilder()
                .setImage(Entry.Image.newBuilder()
                        .setSymbol(0x0011)
                        .setColor(Common.RgbColor.newBuilder()
                                .setR(0.2f)
                                .setG(0.5f)
                                .setB(0.0f)
                                .build())
                        .setFont("minecraft:aabbcc")
                        .setOffset(Entry.Offset.newBuilder()
                                .setX(1)
                                .setY(10)
                                .build())
                        .setWidth(12)
                        .build())
                .build(), Entry.class, common.entries.Entry.class);
    }
    @Test
    void testRandom() {
        RandomProto rndProto = RandomProto.newRandom()
                .random(new Random(1))
                .override((v, rnd)
                        -> v.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.STRING)
                        && switch (v.getName()) {
                    case "font", "key" -> true;
                    default -> false;
                }
                        ? Optional.of("minecraft:" + rnd.nextString(false, true, false))
                        : Optional.empty())
                .override((v, rnd)
                        -> v.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE)
                        && v.isRepeated()
                        && v.getMessageType().getFullName().equals("common.Common.Font.CharWidthEntry")
                        ? Optional.of(List.of(DynamicMessage.newBuilder(v.getMessageType())
                        .setField(v.getMessageType().findFieldByName("key"), rnd.random().nextInt())
                        .setField(v.getMessageType().findFieldByName("value"), rnd.nextString(true, true, true, false))
                        .build()))
                        : Optional.empty())
                .override((v, rnd)
                        -> v.getJavaType().equals(Descriptors.FieldDescriptor.JavaType.MESSAGE)
                        && v.getMessageType().getFullName().equals("common.Common.RgbColor")
                        ? Optional.of(Common.RgbColor.newBuilder()
                        .setR(rnd.random().nextInt(6) / (float)5)
                        .setG(rnd.random().nextInt(6) / (float)5)
                        .setB(rnd.random().nextInt(6) / (float)5)
                        .build())
                        : Optional.empty());
        for (TypePair<?, ?> key : mapper.keys()) {
            if (!Message.class.isAssignableFrom(key.tIn()))
                continue;
            Class<Message> tClass = (Class<Message>) key.tIn();
            for (int i = 0; i < 10; i++) {
                var v = rndProto.nextMessage(tClass);
                testInverseMap(v, tClass, key.tOut());
            }
        }
    }
}

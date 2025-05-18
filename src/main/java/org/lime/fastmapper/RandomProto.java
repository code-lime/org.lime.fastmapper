package org.lime.fastmapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.Func1;
import org.lime.core.common.system.execute.Func2;
import org.lime.fastmapper.converter.property.info.PropertyLoader;

import java.util.*;

public class RandomProto {
    private Random rnd = new Random();
    private final List<Func2<Descriptors.FieldDescriptor, RandomProto, Optional<Object>>> overrideFields = new ArrayList<>();
    private final List<Func2<Descriptors.Descriptor, RandomProto, Optional<Message>>> overrideDescriptors = new ArrayList<>();

    private RandomProto() {}
    public static RandomProto newRandom() {
        return new RandomProto();
    }

    public Random random() {
        return rnd;
    }
    public RandomProto random(Random rnd) {
        this.rnd = rnd;
        return this;
    }

    public <T>RandomProto overrideFields(Func1<Descriptors.FieldDescriptor, Optional<T>> func) {
        overrideFields.add((f, _)->func.invoke(f).map(v->v));
        return this;
    }
    public <T>RandomProto overrideFields(Func2<Descriptors.FieldDescriptor, RandomProto, Optional<T>> func) {
        overrideFields.add((f, r)->func.invoke(f,r).map(v->v));
        return this;
    }

    public <T extends Message>RandomProto overrideDescriptors(Func1<Descriptors.Descriptor, Optional<T>> func) {
        overrideDescriptors.add((f, _)->func.invoke(f).map(v->v));
        return this;
    }
    public <T extends Message>RandomProto overrideDescriptors(Func2<Descriptors.Descriptor, RandomProto, Optional<T>> func) {
        overrideDescriptors.add((f, r)->func.invoke(f,r).map(v->v));
        return this;
    }

    public <T extends Message>RandomProto overrideDescriptors(Class<T> tClass, Func0<Optional<T>> func) {
        Descriptors.Descriptor desc = builderForClass(tClass).getDescriptorForType();
        return overrideDescriptors(v -> v.equals(desc) ? func.invoke() : Optional.empty());
    }
    public <T extends Message>RandomProto overrideDescriptors(Class<T> tClass, Func1<RandomProto, Optional<T>> func) {
        Descriptors.Descriptor desc = builderForClass(tClass).getDescriptorForType();
        return overrideDescriptors((v, rnd) -> v.equals(desc) ? func.invoke(rnd) : Optional.empty());
    }

    public <T extends Message> T nextMessage(Class<T> tClass) {
        return nextMessage(builderForClass(tClass));
    }
    @SuppressWarnings("unchecked")
    public <T extends Message> T nextMessage(Message.Builder builder) {
        Descriptors.Descriptor descriptor = builder.getDescriptorForType();
        for (var ov : overrideDescriptors) {
            var v = ov.invoke(descriptor, this);
            if (v.isEmpty())
                continue;
            return (T)v.get();
        }

        Map<Descriptors.OneofDescriptor, Descriptors.FieldDescriptor> oneofChoices = new HashMap<>();
        for (Descriptors.OneofDescriptor oneof : descriptor.getOneofs()) {
            List<Descriptors.FieldDescriptor> fields = oneof.getFields();
            Descriptors.FieldDescriptor chosenField = fields.get(rnd.nextInt(fields.size()));
            oneofChoices.put(oneof, chosenField);
        }

        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            Descriptors.OneofDescriptor oneof = field.getContainingOneof();
            if (oneof != null) {
                if (!oneofChoices.containsKey(oneof))
                    continue;
                if (!oneofChoices.get(oneof).equals(field))
                    continue;
            }
            if (field.isRepeated()) {
                int count = rnd.nextInt(1, 3);
                for (int i = 0; i < count; i++) {
                    Object value = generateRandomValue(field);
                    if (value instanceof Iterable<?> iterable) {
                        iterable.forEach(v -> builder.addRepeatedField(field, v));
                        break;
                    }
                    builder.addRepeatedField(field, value);
                }
            } else {
                if (!PropertyLoader.hasOptionalKeyword.invoke(field) || rnd.nextBoolean()) {
                    Object value = generateRandomValue(field);
                    builder.setField(field, value);
                }
            }
        }
        return (T) builder.build();
    }

    private Object generateRandomValue(Descriptors.FieldDescriptor field) {
        for (var ov : overrideFields) {
            var v = ov.invoke(field, this);
            if (v.isEmpty())
                continue;
            return v.get();
        }
        return switch (field.getJavaType()) {
            case INT -> rnd.nextInt();
            case LONG -> rnd.nextLong();
            case FLOAT -> rnd.nextFloat();
            case DOUBLE -> rnd.nextDouble();
            case BOOLEAN -> rnd.nextBoolean();
            case STRING -> nextString(true, true, true);
            case MESSAGE -> nextMessage(builderForField(field));
            case BYTE_STRING -> ByteString.copyFromUtf8(nextString(true, true, true));
            case ENUM -> {
                var values = field.getEnumType().getValues();
                yield values.get(rnd.nextInt(values.size()));
            }
            default -> throw new IllegalArgumentException("Неизвестный тип поля: " + field.getJavaType());
        };
    }
    private Message.Builder builderForField(Descriptors.FieldDescriptor field) {
        return DynamicMessage.newBuilder(field.getMessageType());
    }
    private Message.Builder builderForClass(Class<? extends Message> tClass) {
        var method = ReflectionMethod.of(tClass, "newBuilder");
        if (!method.isStatic() || !Message.Builder.class.isAssignableFrom(method.method().getReturnType()))
            throw new IllegalArgumentException("newBuilder method not found");
        return (Message.Builder)method.call(new Object[0]);
    }

    private void shuffle(char[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
    private static final String charsUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String charsLower = "abcdefghijklmnopqrstuvwxyz";
    private static final String charsNumber = "0123456789";
    public String nextString(int length, boolean upper, boolean lower, boolean number) {
        return nextString(length, upper, lower, number, true);
    }
    public String nextString(int length, boolean upper, boolean lower, boolean number, boolean duplicate) {
        char[] sb;
        String chars = (upper ? charsUpper : "")
                + (lower ? charsLower : "")
                + (number ? charsNumber : "");
        int charsLength = chars.length();
        if (duplicate) {
            sb = new char[length];
            for (int i = 0; i < length; i++) {
                sb[i] = chars.charAt(rnd.nextInt(charsLength));
            }
        } else {
            char[] ch = chars.toCharArray();
            shuffle(ch);
            sb = Arrays.copyOf(ch, length);
        }
        Arrays.sort(sb);
        return String.copyValueOf(sb);
    }
    public String nextString(boolean upper, boolean lower, boolean number) {
        return nextString(upper, lower, number, true);
    }
    public String nextString(boolean upper, boolean lower, boolean number, boolean duplicate) {
        return nextString(rnd.nextInt(5, 15), upper, lower, number, duplicate);
    }
}

package moe.nea.zwirn;

import java.util.ArrayList;
import java.util.List;

public interface Descriptor {
    Descriptor mapClassName(SimpleRemapper remapper);

    String toJvmDescriptor();

    interface Type extends Descriptor {
        Type mapClassName(SimpleRemapper remapper);

        static Type readType(GoodStringReader reader) {
            if (reader.peekChar() == 'V') {
                reader.nextChar();
                return VoidType.VOID;
            }
            return Field.readField(reader);
        }
    }

    interface Field extends Type {
        Field mapClassName(SimpleRemapper remapper);

        static Field readField(GoodStringReader reader) {
            switch (reader.nextChar()) {
                case 'L':
                    return new Class(reader.readUntil(';').replace('/', '.'));
                case 'Z':
                    return DefaultField.BOOLEAN;
                case 'B':
                    return DefaultField.BYTE;
                case 'I':
                    return DefaultField.INT;
                case 'D':
                    return DefaultField.DOUBLE;
                case 'J':
                    return DefaultField.LONG;
                case 'S':
                    return DefaultField.SHORT;
                case 'F':
                    return DefaultField.FLOAT;
                case 'C':
                    return DefaultField.CHAR;
                case '[':
                    return new Array(readField(reader));
            }
            throw new IllegalStateException("Unknown type");
        }
    }

    record Method(List<Field> argumentTypes, Type returnType) implements Descriptor {

        @Override
        public Descriptor mapClassName(SimpleRemapper remapper) {
            List<Field> newArgumentTypes = new ArrayList<>(argumentTypes.size());
            for (Field argumentType : argumentTypes) {
                newArgumentTypes.add(argumentType.mapClassName(remapper));
            }
            return new Method(newArgumentTypes, returnType.mapClassName(remapper));
        }

        @Override
        public String toJvmDescriptor() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            for (Field argumentType : argumentTypes) {
                sb.append(argumentType.toJvmDescriptor());
            }
            sb.append(")");
            sb.append(returnType.toJvmDescriptor());
            return sb.toString();
        }

        public static Method readMethod(GoodStringReader reader) {
            if (reader.nextChar() != '(')
                throw new IllegalStateException("Expected (");
            List<Field> argument = new ArrayList<>();
            while (reader.peekChar() != ')') {
                argument.add(Field.readField(reader));
            }
            reader.nextChar(); // Consume )
            return new Method(argument, Type.readType(reader));
        }
    }

    record Array(Field field) implements Field {

        @Override
        public String toJvmDescriptor() {
            return "[" + field.toJvmDescriptor();
        }

        @Override
        public Array mapClassName(SimpleRemapper remapper) {
            return new Array(field.mapClassName(remapper));
        }
    }

    record Class(String dottedName) implements Field {
        @Override
        public String toJvmDescriptor() {
            return "L" + dottedName.replace('.', '/') + ";";
        }

        @Override
        public Field mapClassName(SimpleRemapper remapper) {
            return new Class(remapper.remapClass(dottedName));
        }
    }


    enum VoidType implements Type {
        VOID;

        @Override
        public String toJvmDescriptor() {
            return "V";
        }

        @Override
        public VoidType mapClassName(SimpleRemapper remapper) {
            return this;
        }
    }

    enum DefaultField implements Field {
        BOOLEAN("Z"),
        BYTE("B"),
        INT("I"),
        DOUBLE("D"),
        LONG("J"),
        SHORT("S"),
        FLOAT("F"),
        CHAR("C");
        private final String jvmDescriptor;

        DefaultField(String jvmDescriptor) {
            this.jvmDescriptor = jvmDescriptor;
        }

        @Override
        public String toJvmDescriptor() {
            return jvmDescriptor;
        }

        @Override
        public DefaultField mapClassName(SimpleRemapper remapper) {
            return this;
        }
    }

}

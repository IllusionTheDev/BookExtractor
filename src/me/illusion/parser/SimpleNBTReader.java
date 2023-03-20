package me.illusion.parser;

import java.io.DataInput;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SimpleNBTReader {
    interface Helper{
        Object apply(DataInput t) throws Exception;
    }

    public static class Node{
        TagType type;
        String name;
        Object value;

        public Node(TagType type,String name, Object value){
            this.type = type;
            this.name = name;
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public TagType getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public boolean nameEquals(String name) {
            return this.name.equals(name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return type == node.type && nameEquals(node.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name, value);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "type=" + type +
                    ", name='" + name + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    public enum TagType {
        TAG_End(s -> null),
        TAG_Byte(DataInput::readByte),
        TAG_Short(DataInput::readShort),
        TAG_Int(DataInput::readInt),
        TAG_Long(DataInput::readLong),
        TAG_Float(DataInput::readFloat),
        TAG_Double(DataInput::readDouble),
        TAG_Byte_Array(in -> {
            int len = in.readInt();
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            return bytes;
        }),
        TAG_String(in -> {
            int len = in.readShort();
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }),
        TAG_List(in ->{
            TagType type = TagType.values()[in.readByte()];
            int len = in.readInt();
            Object[] values = new Object[len];
            for(int i=0; i < len; i++)
                values[i] = type.read(in);
            return values;
        }),
        TAG_Compound(in -> {
            List<Object> values = new LinkedList<>();
            while(true){
                TagType type = TagType.values()[in.readByte()];

                if(type == TagType.TAG_End)
                    break;

                values.add(readTag(type,in));
            }
            return values;
        }),
        TAG_Int_Array(in -> {
            int len = in.readInt();
            int[] values = new int[len];
            for(int i=0; i < len; i++)
                values[i] = in.readInt();
            return values;
        })
        ;
        private Helper body;

        TagType(Helper body) {
            this.body = body;
        }

        public Object read(DataInput in) throws Exception {
            return body.apply(in);
        }
    }

    private static Node readTag(TagType type, DataInput in) throws Exception{
        if(type == TagType.TAG_End)
            throw new Exception("TAG_END has no name data.");

        int nameLength = in.readShort();
        byte[] buffer = new byte[nameLength];
        in.readFully(buffer);
        String name = new String(buffer, StandardCharsets.UTF_8);

        return new Node(type,name,type.read(in));
    }

    public static Object read(DataInput in) throws Exception{
        TagType type = TagType.values()[in.readByte()];
        return readTag(type,in);
    }

}
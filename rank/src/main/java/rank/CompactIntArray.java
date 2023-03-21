package rank;

public class CompactIntArray {
    private Object elements;
    // private byte[] offsetsThrough256;
    // private short[] offsets;
    // private int size;
    private byte type; // 0 for byte, 1 for short, 2 for int

    public CompactIntArray(int size, int maxElement) {
        // this.size = size;
        if (maxElement < 256) {
            elements = new byte[size];
            type = 0;
        } else if (maxElement < 32767) {
            elements = new short[size];
            type = 1;
        } else {
            elements = new int[size];
            type = 2;
        }
    }

    public void add(int index, int element) {
        if (type == 0) {
            ((byte[]) elements)[index] = (byte) element;
        } else if (type == 1) {
            ((short[]) elements)[index] = (short) element;
        } else {
            ((int[]) elements)[index] = element;
        }
    }

    public byte getType() {
        return type;
    }

//    public int getSize() {
//        if (type == 0) {
//            return ((byte[]) elements).length;
//        } else if (type == 1) {
//            return ((short[]) elements).length;
//        }
//        return ((int[]) elements).length;
//    }

    public int getBitsForArray() {
        if (type == 0) {
            System.out.println("type: byte");
            return ((byte[]) elements).length * 8;
        } else if (type == 1) {
            System.out.println("type: short");
            return ((short[]) elements).length * 16;
        }
        System.out.println("type: int");
        return ((int[]) elements).length * 32;

    }
//
//    private enum Type {
//        BYTE, SHORT, INT;
//    }

}

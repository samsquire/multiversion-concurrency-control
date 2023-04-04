package main;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ByteBufferArrayOutputStream extends ByteArrayOutputStream {
    public ByteBufferArrayOutputStream(int size) {
        super(size);
    }

    public ByteBuffer toByteBuffer() {
        System.out.println(count);
        return ByteBuffer.wrap(buf, 0, count);
    }
}

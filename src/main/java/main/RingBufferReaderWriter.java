package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RingBufferReaderWriter extends RingBufferUserThread {
    private RingBuffer ringbuffer;
    public List<Integer> reads = new ArrayList<>();

    public RingBufferReaderWriter(RingBuffer ringbuffer) {

        this.ringbuffer = ringbuffer;
    }

    public void run() {
        for (int i = 0 ; i < 100; i++) {
            Integer read = this.ringbuffer.read(this);
            reads.add(read);
        }
    }

}

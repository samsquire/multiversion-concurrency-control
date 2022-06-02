package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RingBufferWriter extends RingBufferUserThread {
    private final RingBuffer ringbuffer;
    public List<Integer> writes = new ArrayList<>();
    public RingBufferWriter(RingBuffer ringbuffer) {
        this.ringbuffer = ringbuffer;
    }
    public void run() {

            for (int i = 0; i < 100; i++) {
                this.ringbuffer.write(this, i);

            }

    }

}

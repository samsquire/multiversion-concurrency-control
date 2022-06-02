package main;

import java.util.ArrayList;
import java.util.List;

public class MultipleProducerConsumerRingBufferRunner {
    public static void main(String[] args) throws InterruptedException {
        RingBuffer ringbuffer = new RingBuffer(50);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0 ; i < 100; i++) {
            RingBufferWriter ringBufferWriter = new RingBufferWriter(ringbuffer);
            ringbuffer.addProducerThread(ringBufferWriter);
            threads.add(ringBufferWriter);
            RingBufferReaderWriter ringbufferrw = new RingBufferReaderWriter(ringbuffer);
            threads.add(ringbufferrw);
            ringbuffer.addConsumerThread(ringbufferrw);


        }
        for (int i = 0 ; i < 100; i++) {
            threads.get(i).start();
        }
        for (int i = 0 ; i < 100; i++) {
            threads.get(i).join();
        }
        List<Integer> writes = new ArrayList<>();
        for (RingBufferUserThread thread : ringbuffer.getProducers()) {
            writes.addAll(((RingBufferWriter) thread).writes);
        }
        List<Integer> reads = new ArrayList<>();
        for (RingBufferUserThread thread : ringbuffer.getConsumers()) {
            reads.addAll(((RingBufferReaderWriter) thread).reads);
        }
        for (Integer write : writes) {
            assert reads.contains(write);
        }
    }
}

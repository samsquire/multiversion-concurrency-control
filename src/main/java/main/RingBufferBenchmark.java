package main;

import java.util.ArrayList;

public class RingBufferBenchmark extends RingBufferWriter {
    private final boolean writer;
    private final int id;
    private final RingBuffer ringBuffer;
    private volatile boolean running = true;
    private long requests;

    public static void main(String args[]) throws InterruptedException {
        int threadCount = 2;
        int seconds = 5;
        int writerCount = 11;
        int readerCount = 1;
        ArrayList<RingBufferBenchmark> threads = new ArrayList<>();
        RingBuffer ringBuffer = new RingBuffer(5000);
        for (int x = 0; x < writerCount; x++) {

            RingBufferBenchmark ringBufferBenchmark = new RingBufferBenchmark(x, ringBuffer, true);
            ringBuffer.addProducerThread(ringBufferBenchmark);

            threads.add(ringBufferBenchmark);
        }
        for (int x = 0; x < readerCount; x++) {
            RingBufferBenchmark ringBufferBenchmark = new RingBufferBenchmark(x, ringBuffer, false);
            ringBuffer.addConsumerThread(ringBufferBenchmark);
            threads.add(ringBufferBenchmark);

        }
        long start = System.currentTimeMillis();

        for (RingBufferBenchmark thread : threads) {
            thread.start();
        }
        Thread.sleep(seconds * 1000);
        for (RingBufferBenchmark thread : threads) {
            thread.running = false;
        }
        for (RingBufferBenchmark thread : threads) {
            thread.join();
        }
        long end = System.currentTimeMillis();
        long requests = 0;
        for (RingBufferBenchmark thread : threads) {
            requests += thread.requests;
        }
        System.out.println(String.format("Requests %d", requests));
        double elapsed = (end - start) / 1000.0;
        System.out.println(String.format("Requests per second %f", requests / elapsed));

    }

    public RingBufferBenchmark(int id, RingBuffer ringBuffer, boolean writer) {
        super(ringBuffer);
        this.writer = writer;
        this.id = id;
        this.ringBuffer = ringBuffer;
    }

    public void run() {
        Integer value = id;
        if (writer) {
            while (running) {
                ringBuffer.write(this, value);
                // requests++;
            }
        } else {
            while (running) {
                value = ringBuffer.read(this);
                requests++;
            }
        }
    }
}

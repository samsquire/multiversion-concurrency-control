package main;

import java.util.ArrayList;
import java.util.List;

public class QueueBenchmark extends RingBufferReaderWriter {
    private int threadCount;
    private RingBuffer ringBuffer;
    private volatile boolean running = true;
    private ArrayList<QueueBenchmark> threads;
    private int n;

    public QueueBenchmark(int threadCount, RingBuffer ringBuffer) {
        super(ringBuffer);
        this.threadCount = threadCount;
        this.ringBuffer = ringBuffer;
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        List<QueueBenchmark> threads = new ArrayList<>();
        for (int i = 0 ; i < threadCount ; i++) {
            QueueBenchmark queueBenchmark = new QueueBenchmark(threadCount, new RingBuffer(12));
            threads.add(queueBenchmark);
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
            threads.get(i).ringBuffer.addConsumerThread(threads.get(i));

            for (int j = 0 ; j < threadCount; j++) {
                    threads.get(i).ringBuffer.addProducerThread(threads.get(j));
            }
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).start();
        }
        Thread.sleep(5000);
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).join();
        }
        int total = 0;
        for (int i = 0 ; i < threadCount ; i++) {
            total += threads.get(i).n;
        }
        System.out.println(String.format("Number of requests %d", total));
        System.out.println(String.format("Number of requests per second %d", total / 5));
    }

    private void setThreads(ArrayList<QueueBenchmark> queueBenchmarks) {
        this.threads = queueBenchmarks;
    }

    public void run() {
        while (running) {
            for (int i = 0 ; i < threadCount; i++) {

                threads.get(i).ringBuffer.writeNonblocking(this, i);
            }
            int value = 0;
            while (value != -1) {
                value = ringBuffer.readNoblocking(this);
                // System.out.println(String.format("Read %d", value));
                n++;
            }

        }
    }
}

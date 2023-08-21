/**
 * Ported from https://www.linuxjournal.com/content/lock-free-multi-producer-multi-consumer-queue-ring-buffer
 * to Java by Samuel Michael Squire 2022.
 * See my github https://github.com/samsquire
 * See original C++ code
 * https://github.com/tempesta-tech/blog/blob/master/lockfree_rb_q.cc
 * Thank you to those that wrote the original.
 */

package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RingBuffer {
    private final int size;

    private volatile AtomicInteger last_head = new AtomicInteger(1);

    private volatile AtomicInteger last_tail = new AtomicInteger(1);
    private int[] data;
    private List<RingBufferUserThread> consumerThreads;
    private List<RingBufferUserThread> producerThreads;

    private volatile AtomicInteger head = new AtomicInteger(0);

    private volatile AtomicInteger tail = new AtomicInteger(0);

    public RingBuffer(int size) {
        data = new int[size];
        this.size = size;
        consumerThreads = new ArrayList<>();
        producerThreads = new ArrayList<>();

    }

    public void addConsumerThread(RingBufferUserThread writer) {
        writer.head.set(0);
        writer.tail.set(0);
        this.consumerThreads.add(writer);
    }
    public void addProducerThread(RingBufferUserThread writer) {
        writer.tail.set(0);
        writer.head.set(0);
        this.producerThreads.add(writer);
    }

    public List<RingBufferUserThread> getProducers() {
        return producerThreads;
    }

    public List<RingBufferUserThread> getConsumers() {
        return consumerThreads;
    }

    public Integer read(RingBufferReaderWriter reader) {
        reader.tail.set(tail.get());
        tail.set(tail.get()%size);
        reader.tail.set(tail.getAndIncrement());
        reader.tail.set(reader.tail.get()%size);

        while (reader.tail.get() >= last_head.get() + size) {
            System.out.println(String.format("%d %d Cannot read", reader.tail.get(), last_head.get()));
            Thread.yield();
            int min = head.get();
            for (RingBufferUserThread thread : producerThreads) {
                int tmp_h = thread.head.get();
                if (tmp_h < min) {
                    min = tmp_h;
                }

            }

            last_head.set(min);
            if (reader.tail.get() < last_head.get() + size) {
                break;
            }
        }

        int returnValue = data[reader.tail.get()];
        reader.tail.set(Integer.MAX_VALUE);
        // System.out.println(String.format("Read %d", returnValue));
        return returnValue;
    }

    public Integer readNoblocking(RingBufferReaderWriter reader) {
        if (reader.tail.get() >= last_head.get() + size) {
            // System.out.println(String.format("%d %d Cannot read", reader.tail.get(), last_head.get()));
            return -1;
        }
        reader.tail.set(tail.get());
        tail.set(tail.get()%size);
        reader.tail.set(tail.getAndIncrement());
        reader.tail.set(reader.tail.get()%size);

        if (reader.tail.get() >= last_head.get() + size) {
            //System.out.println(String.format("%d %d Cannot read", reader.tail.get(), last_head.get()));
            return -1;
        }
            int min = head.get();
            for (RingBufferUserThread thread : producerThreads) {
                int tmp_h = thread.head.get();
                if (tmp_h < min) {
                    min = tmp_h;
                }

            }

            last_head.set(min);
            if (reader.tail.get() < last_head.get() + size) {

            }


        int returnValue = data[reader.tail.get()];
        reader.tail.set(Integer.MAX_VALUE);
        // System.out.println(String.format("Read %d", returnValue));
        return returnValue;
    }

    public int writeNonblocking(RingBufferUserThread writer, int value) {
        if (writer.head.get() >= last_tail.get()+size) {
            return -1;
        }
        writer.head.set(head.get());
        writer.head.set(head.incrementAndGet());
        head.set(head.get() % size);
        writer.head.set(writer.head.get()%size);


//            System.out.println("Cannot write");
            int min = tail.get();
            for (RingBufferUserThread thread : consumerThreads) {
                int tmp_t = thread.tail.get();
                if (tmp_t < min) {
                    min = tmp_t;
                }
            }
            last_tail.set(min);
            if (writer.head.get() < last_tail.get() + size) {

            }


        // System.out.println(String.format("Wrote %d", value));
        data[writer.head.get()] = value;

        writer.head.set(Integer.MAX_VALUE);
        return 0;
    }

    public void write(RingBufferUserThread writer, int value) {
        int i = head.get();
        int newValue = (i + 1) % size;
        head.set(newValue);
        writer.head.set(newValue);
        while (writer.head.get() >= last_tail.get()) {
//            System.out.println("Cannot write");
            int min = tail.get();
            for (RingBufferUserThread thread : consumerThreads) {
                int tmp_t = thread.tail.get();
                if (tmp_t < min) {
                    min = tmp_t;
                }
            }
            last_tail.set(min);
            if (writer.head.get() < last_tail.get() + size) {
                break;
            }
        }
        // System.out.println(String.format("Wrote %d", value));
        data[writer.head.get()] = value;

        writer.head.set(Integer.MAX_VALUE);
    }


    public Integer read(RingBufferUserThread reader) {
        int andIncrement = tail.get();
        int newValue = (andIncrement + 1) % size;
        tail.set(newValue);
        reader.tail.set(newValue);

        while (reader.tail.get() >= last_head.get()) {
//            System.out.println(String.format("%d %d Cannot read", reader.tail.get(), last_head.get()));
            int min = head.get();
            for (RingBufferUserThread thread : producerThreads) {
                int tmp_h = thread.head.get();
                if (tmp_h < min) {
                    min = tmp_h;
                }

            }

            last_head.set(min);
            if (reader.tail.get() < last_head.get() + size) {
                break;
            }
        }

        int returnValue = data[reader.tail.get()];
        reader.tail.set(Integer.MAX_VALUE);
//        System.out.println(String.format("Read %d", returnValue));
        return returnValue;
    }
}

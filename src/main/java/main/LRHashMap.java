package main;



import sun.misc.Contended;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class LRHashMap<K,V> {
    private Lock lock;
    private int size;



    int LEFT = -1;
    int RIGHT = 1;
    int READING = 1;
    int NOT_READING = -1;
    private ReadWriteLock writerLock = new ReentrantReadWriteLock();
    private AtomicInteger leftRight = new AtomicInteger(LEFT);
    private volatile HashMap<K,V> left = new HashMap<K,V>();
    private volatile HashMap<K,V> right = new HashMap<K,V>();

    @Contended("readersVersion")
    private volatile AtomicInteger[][] readersVersion = new AtomicInteger[2][];
    private ThreadLocal<Integer> versionIndex = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };
    public LRHashMap(int size) {
        this.size = size;


        this.lock = writerLock.writeLock();
        for (int i = 0 ; i < readersVersion.length; i++) {
            readersVersion[i] = new AtomicInteger[size];
            for (int j = 0 ; j < size; j++) {
                readersVersion[i][j] = new AtomicInteger();
                readersVersion[i][j].set(NOT_READING);
            }
        }
    }

    public void arrive(Reader reader) {
        readersVersion[versionIndex.get()][reader.threadIndex].set(READING);
    }
    public void depart(Reader reader) {

        readersVersion[versionIndex.get()][reader.threadIndex].set(NOT_READING);
        // System.out.println(String.format("%d departed", reader.threadIndex));

    }

    public Set<Map.Entry<K, V>> entrySet(LRHashMap.Reader reader) {
        arrive(reader);
        Set<Map.Entry<K, V>> value = null;
        if (leftRight.get() == LEFT) {
            value = left.entrySet();
        }
        else {
            value = right.entrySet();
        }
        depart(reader);
        return value;
    }



    public Set<K> keySet(Reader reader) {
        arrive(reader);
        Set<K> value = null;
        if (leftRight.get() == LEFT) {
            value = left.keySet();
        }
        else {
            value = right.keySet();
        }

        return value;
    }



    public V get(Reader reader, K key) {
        arrive(reader);
        V value = null;
        if (leftRight.get() == LEFT) {
             value = left.get(key);
        }
        else {
            value = right.get(key);
        }
        depart(reader);
        return value;
    }

    public boolean containsKey(Reader reader, K key) {
        arrive(reader);
        boolean value = false;
        if (leftRight.get() == LEFT) {
            value = left.containsKey(key);
        }
        else {
            value = right.containsKey(key);
        }

        depart(reader);
        return value;
    }

    public void put(K key, V value, int timestamp) {
        System.out.println(String.format("t%d %s %s Put lock", timestamp, key, value.toString()));
        lock.lock();
        int previousMode = leftRight.get();
        if (previousMode == LEFT) {
            right.put(key, value);
        }
        else if (previousMode == RIGHT) {
            left.put(key, value);
        }
        int nextMode = LEFT;
        if (previousMode == LEFT) {
            nextMode = RIGHT;
        }
        else if (previousMode == RIGHT) {
            nextMode = LEFT;
        }
        leftRight.set(nextMode);
        int previousVersionIndex = versionIndex.get();
        int nextVersionIndex = (previousVersionIndex + 1) % 2;
        while (!nobodyReading(nextVersionIndex)) {
            System.out.println(String.format("Someone is reading, t%d is waiting", timestamp));
            Thread.yield();
        }
        versionIndex.set(nextVersionIndex);
        while (!nobodyReading(previousVersionIndex)) {
            System.out.println(String.format("Someone is reading, t%d is waiting", timestamp));
            Thread.yield();
        }

        if (previousMode == LEFT) {
            left.put(key, value);
        }
        else if (previousMode == RIGHT) {
            right.put(key, value);
        }

        lock.unlock();

    }

    public void remove(int threadId, K key) {
        System.out.println("Remove lock");
        lock.lock();
        int previousMode = leftRight.get();
        if (previousMode == LEFT) {
            right.remove(key);

        }
        else if (previousMode == RIGHT) {
            left.remove(key);

        }
        int nextMode = LEFT;
        if (previousMode == LEFT) {
            nextMode = RIGHT;
        }
        else if (previousMode == RIGHT) {
            nextMode = LEFT;
        }
        leftRight.set(nextMode);
        int previousVersionIndex = versionIndex.get();
        int nextVersionIndex = (previousVersionIndex + 1) % 2;
        while (!nobodyReading(nextVersionIndex)) {
            Thread.yield();
        }
        versionIndex.set(nextVersionIndex);
        while (!nobodyReading(previousVersionIndex)) {
            Thread.yield();
        }


        if (previousMode == LEFT) {
            left.remove(key);
        }
        else if (previousMode == RIGHT) {
            right.remove(key);
        }

        lock.unlock();
    }

    public void clear() {
        System.out.println(String.format("%d Clear lock", versionIndex.get()));
        lock.lock();
        int previousMode = leftRight.get();
        if (previousMode == LEFT) {
            right.clear();

        }
        else if (previousMode == RIGHT) {
            left.clear();

        }
        int nextMode = LEFT;
        if (previousMode == LEFT) {
            nextMode = RIGHT;
        }
        else if (previousMode == RIGHT) {
            nextMode = LEFT;
        }
        leftRight.set(nextMode);
        int previousVersionIndex = versionIndex.get();
        int nextVersionIndex = (previousVersionIndex + 1) % 2;
        while (!nobodyReading(nextVersionIndex)) {
            System.out.println("Someone is reading");

            Thread.yield();
        }
        versionIndex.set(nextVersionIndex);
        while (!nobodyReading(previousVersionIndex)) {
            System.out.println("Someone is reading");

            Thread.yield();
        }

        if (previousMode == LEFT) {
            left.clear();
        }
        else if (previousMode == RIGHT) {
            right.clear();
        }
        lock.unlock();
    }


    public boolean nobodyReading(int previousVersionIndex) {
        for (int i = 0 ; i < size; i++) {
            if (readersVersion[previousVersionIndex][i].get() == READING) {
                System.out.println(String.format("%d is reading", i));
                return false;
            }
        }
        return true;
    }

    public static class Reader {
        public int threadIndex;
        public Reader(int threadIndex) {
            this.threadIndex = threadIndex;
        }
    }


    public String toString(int threadIndex) {
        Reader reader = new Reader(threadIndex);
        arrive(reader);
        String value;
        if (leftRight.get() == LEFT) {
            value = left.toString();
        } else {
            value = right.toString();
        }
        depart(reader);
        return value;
    }


}

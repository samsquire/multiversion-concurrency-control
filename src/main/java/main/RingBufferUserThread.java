package main;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class RingBufferUserThread extends Thread {
    public volatile AtomicInteger head = new AtomicInteger(0);
    public volatile AtomicInteger tail = new AtomicInteger(-1);

}

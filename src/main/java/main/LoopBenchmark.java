package main;

import java.util.ArrayList;
import java.util.List;

public class LoopBenchmark {
    public static void main(String[] args) {
        int size = 1000000000;
        List<Message> messages = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            messages.add(new Message(1));
        }
        long start = System.currentTimeMillis();
        int requestCount = 0;
        for (Message m : messages) {
            requestCount += m.i;
        }
        long end = System.currentTimeMillis();
        long duration = end - start;
        System.out.println(String.format("Time to loop %d: %dms or %ss", size, duration, duration / 1000));

    }

    public static class Message {
        public int i;
        public Message(int i) {
            this.i = i;
        }
    }
}

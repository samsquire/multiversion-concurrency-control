package main;

import java.util.Arrays;
import java.util.List;

public class StringOrConcurrentLoop {
    public final String value;
    public final List<ConcurrentLoop> loops;


    public StringOrConcurrentLoop(String value, List<ConcurrentLoop> loops) {
        this.value = value;
        this.loops = loops;
    }

    public boolean isString() {
        return value != null;
    }

    public boolean isLoops() {
        return loops != null;
    }
}

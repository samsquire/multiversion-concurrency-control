package main;

public class Change {
    private Runnable runnable;

    public Change(Runnable runnable) {

        this.runnable = runnable;
    }

    public void apply() {
        this.runnable.run();
    }
}

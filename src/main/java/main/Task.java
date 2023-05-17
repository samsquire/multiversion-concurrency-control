package main;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private List<Task> tasks = new ArrayList<>();

    public static void main(String[] args) {
        Task task1 = new Task();
        Task task2 = new Task();
        Task task3 = new Task();
        Task childTask1 = task1.createChildTask();
        childTask1.mutuallyExclusiveWith(task2);
        task2.dependsOn(task1);
        task3.dependsOn(task1);
        Task task4 = new Task();
        task4.runWhenever(task3);
        Task task5 = new Task();
        task5.communicateWith(task2);
        task5.triggers(task1);
    }

    private Task triggers(Task task) {
        return this;
    }

    private Task communicateWith(Task task2) {
        return this;
    }

    private Task runWhenever(Task task3) {
        return this;
    }

    private Task dependsOn(Task task) {
        return this;
    }

    private Task mutuallyExclusiveWith(Task task2) {
        return this;
    }

    private Task createChildTask() {
        Task task = new Task();
        this.tasks.add(task);
        return task;
    }
}

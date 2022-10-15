package main;

public class DoublyLinkedList {
    public volatile int modCount = 0;
    public volatile Integer value;
    public volatile int[] reading;
    public volatile DoublyLinkedList head = null;
    public volatile  DoublyLinkedList tail = null;
    public DoublyLinkedList(Integer value) {
        this.value = value;
    }
    public DoublyLinkedList insert(Integer value) {
        int previousModCount = modCount;
        DoublyLinkedList newItem = new DoublyLinkedList(value);

        newItem.head = this;
        DoublyLinkedList previous = tail;
        if (tail != null) {
            tail.head = newItem;
        }
        newItem.tail = previous;
        tail = newItem;
        assert previousModCount == modCount;
        modCount++;
        return this;
    }
}

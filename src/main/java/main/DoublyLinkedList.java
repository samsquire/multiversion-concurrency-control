package main;

public class DoublyLinkedList {
    public volatile int modCount = 0;
    public volatile Integer value;
    private long timestamp;
    public volatile int[] reading;
    public volatile DoublyLinkedList head = null;
    public volatile DoublyLinkedList tail = null;
    public volatile ReferencePassing.Reference reference = null;
    public DoublyLinkedList(Integer value, long timestamp) {
        this.value = value;

        this.timestamp = timestamp;
        this.reference = new ReferencePassing.Reference(this, 0);
    }
    public DoublyLinkedList insert(Integer value) {
        int previousModCount = modCount;
        DoublyLinkedList newItem = new DoublyLinkedList(value, System.nanoTime());

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

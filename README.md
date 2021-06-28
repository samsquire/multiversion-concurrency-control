# multiversion-concurrency-control

MVCC TransactionA and TransactionC is an example of multiversion concurrency control and the avoidance of locks for multithreaded programming.

ConcurrentWithdrawer is another attempt to implement MVCC - it simulates 5 users in a bank where a random account withdraws 100 and sends the 100 to another bank account at random. It takes a different approach and serializes the accounts instead so they don't conflict.

Raft implementation
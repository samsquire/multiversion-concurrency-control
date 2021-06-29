# multiversion-concurrency-control

MVCC TransactionA and TransactionC is an example of multiversion concurrency control and the avoidance of locks for multithreaded programming.

ConcurrentWithdrawer is another attempt to implement MVCC - it simulates 5 users in a bank where a random account withdraws 100 and sends the 100 to another bank account at random. It takes a different approach and serializes the accounts instead so they don't conflict.

Raft implementation

#Multiversion concurrency control - How it works

The database offers keys of values which are integers. They are named, in the transaction examples, the keys are A and B.

When two transactions begin concurrently, they are numbered monotonically increasing: 1, 2, 3 , etc They both try read A and B. They will only "see" versions of A and B that are committed and versioned less than their transaction id so if transaction 2 is running, it won't see transaction 1's changes because 1 hasn't committed. But if transaction 0 has comitted, they will both see those values. But when they both try write to A and B, they create a new version using their transaction timestamp. We keep tract of all the transactions that have "seen" a value.

At commit time, we check all the transactions that have seen the value. The lowest transaction ID wins. Everybody else has to abort and try again. The only exception to this if a transaction was faster than the other and got ahead of everybody else, in which case the younger transaction will be the one that aborts.
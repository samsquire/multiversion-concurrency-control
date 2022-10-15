# multiversion-concurrency-control

This repository is where I do experimental work on concurrency and parallelism problems. In this repository:

* Raft implementation
* Multithreaded multiversion concurrency control
* Left Right Concurrency control with Hashmaps
* A parallel actor interpreter programmed with its own assembly
* 4 variations of a multithreaded parallel actor implementations
* Concurrent looping (parallel mapreduce for nested loops)
* A multiconsumer multiproducer ringbuffer which is threadsafe This is inspired by [Alexander Krizhanovsky](https://www.linuxjournal.com/content/lock-free-multi-producer-multi-consumer-queue-ring-buffer)

The headline implementation is a multithreaded multiversion concurrency control solution which handles safe and concurrent access to a database of integers without locking. We timestamp read events and check if there is any read event with a timestamp that is higher than us, in which case, we restart our transaction.

MVCC.java TransactionA.java and TransactionC.java is an example of multiversion concurrency control and the avoidance of locks of the data being modified for multithreaded programming.

ConcurrentWithdrawer is another attempt to implement MVCC - it simulates 5 users in a bank where a random account withdraws 100 and sends the 100 to another bank account at random. It takes a different approach and serializes the accounts instead so they don't conflict.

# Raft implementation

This is my understanding of the Raft algorithm. I used the Raft paper to implement this and simulate late messages.

# Main.java - parallel actor model

This is an parallel multithreadeded actor model. Run `Main.java` to run it. I get between 100 million requests per second on my Intel(R) Core(TM) i7-10710U CPU @ 1.10GHz, 1608 Mhz, 6 Core(s), 12 Logical Processor(s). This places communication costs between ~500-1000 nanoseconds.

The model checker is written in Python in a different repository, see [multithreaded-model-checker](https://github.com/samsquire/multithreaded-model-checker)

# Actor2 - parallel actor model 2

This is another parallel multithreaded actor model. Run `Actor2.java` to run it. I get around 1.1 billion requests per second with this model.

This program allocates all 10 million messages in advance and communicates them with lists of lists which avoid the parallel iterator problem.

Main.java creates messages as it goes, it reaches 100 million requests per second.

There is another version which creates messages in separate thread and this gets 61 million requests per second.

# Parallel Interpreter

This is a parallel threaded interpreter based on the Actor2 code above which can communicate between itself.

It can achieve the following throughput of communication:

8612462 total requests
1700387.364265 requests per second
Time taken: 5.065000

It runs this program:

```
threads 25
<start>
set running 1
set current_thread 0
set received_value 0
set current 1
:while1
while running :end
receive received_value :send
:send
add received_value current
addv current_thread 1
modulo current_thread 25
send current_thread current :while1
endwhile :while1
:end
```

This starts 25 threads which each try to receive a message and send a message to another thread and add one to the counter of the number of messages received.

# Multiversion concurrency control - How it works

The database offers keys of values which are integers. They are named, in the transaction examples, the keys are A and B.

When two transactions begin concurrently, they are numbered monotonically increasing: 1, 2, 3 , etc They both try read A and B. They will only "see" versions of A and B that are committed and versioned less than their transaction id so if transaction 2 is running, it won't see transaction 1's changes because 1 hasn't committed. But if transaction 0 has comitted, they will both see those values. But when they both try write to A and B, they create a new version using their transaction timestamp. We keep track of all the transactions that have "seen" a value.

At commit time, we check all the transactions that have seen the value. The lowest transaction ID wins. Everybody else has to abort and try again. The only exception to this if a transaction was faster than the other and got ahead of everybody else, in which case the younger transaction will be the one that aborts.

On my machine 100 threads can increment a number in 496ms milliseconds in 888 attempts or lower.

# Concurrent loop ConcurrentLoop.java and tick.py

Naming ideas that other people shall understand can be difficult.
I'm going to use this opportunity to try explain something that can be complicated with the ambiguousness of English.
Here is an example from something I am working on. I am working on parallelising nested loops.
```

for letter in letters:
 for number in numbers:
  for symbol in symbols:
   print(letter + number + symbol)
```

If letters, numbers and symbols are very large and the inner loop does not depend on previous iterations, we can parallelise it. We can separate the lists into chunks and assign each chunk to a thread for processing.

What if we want to keep processing after all threads are finished? We need some idea of waiting for completion of loops.

I have a shared collection of loop objects over all threads. Each thread loops over this collection repeatedly but only evaluates threads that fall within that thread's chunk.

So if we have 1,000,000,000 records in each list and 1000 threads each thread shall process 10,000,000 items.

The first thread processes items 0 — 10,000,000. The second thread 10,000,000 — 20,000,000 and the third thread 30,000,000 to 40,000,000 and so on until 1 billion. All at the same time simultaneously in parallel.

If we want to wait for two other loops to finish, I have an API that links loops together to wait for multiple items before iterating.

I created methods on the loop objects called “wait_for” and “link". Link is used when telling a loop to send items to its argument. Wait for is the reverse relationship — to wait for inputs from each of these loops.
I call the loop objects ConcurrentLoops.

I can execute loops out of order and in parallel and load balanced. When I say load balanced, each subloop is iterated concurrently. So in my example above all letters, numbers and symbol loops are executed concurrently.
Usually the innermost loop finishes before the next iteration of the outerloop. In my design this isn't the case. Every loop is evenly spread out.


# Running for yourself


If you install IntelliJ community edition (it's Java and is available on Mac and Windows)

Install the OpenJDK from Amazon Corretto
https://aws.amazon.com/corretto/

Clone the repository open the project and import the build.gradle. Then when IntelliJ finishes indexing the project, create a gradle run configuration and run the task "run"

![image](https://user-images.githubusercontent.com/1983701/168322406-544b5de1-6113-4cbb-a3e3-b2ad6ce6e57c.png)


To run a stress test, Then in git bash on windows or Mac terminal run the following bash -

 cd build/classes/java/main ; bash -c 'set -e ; while [ true ] ; do java -ea -cp . main.Runner; done'

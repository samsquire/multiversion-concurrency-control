# multiversion-concurrency-control

This repository is where I do experimental work on concurrency and parallelism problems. In this repository:

* A toy Raft implementation
* Multithreaded multiversion concurrency control (MVCC.java and TransactionC.java)
* Left Right Concurrency control with Hashmaps
* A parallel actor interpreter programmed with its own assembly
* Many variations of a multithreaded parallel actor implementations
* Concurrent looping (parallel mapreduce for nested loops)
* A multiconsumer multiproducer ringbuffer which is threadsafe This is inspired by [Alexander Krizhanovsky](https://www.linuxjournal.com/content/lock-free-multi-producer-multi-consumer-queue-ring-buffer)
* An incomplete high level programming language compiler that resembles Javascript that codegen targets the multithreaded interpreter
* An async await switch statement
* Async/await thread pool
* A Soft Lock, a compositional lock scheme

The headline implementation is a multithreaded multiversion concurrency control solution which handles safe and concurrent access to a database of integers without locking. We timestamp read events and check if there is any read event with a timestamp that is lower than us, in which case, we restart our transaction.

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

# High level language

The high level language is incomplete but compiles to the above assembly language.

```
threads 25
<start>
set struct accounts = {
    '1' = '700';
};
for (i = 0 ; i < 100; i++) {
    accounts[i] = {
        balance = randomInt(2500);
    }
}
function deposit(int account, int amount) {
    accounts[account].balance += amount;
}
function withdraw(int account, int amount) {
    accounts[account].balance -= amount;
}
```

Hashes can be built up by the codegen. The following code generates the assembly following:

```
threads 1
<start>
set struct accounts = {
    '1' = {
        'balance' = 700;
        'details' = {
            'name' = 'Samuel Squire';
        }
    };
};
function deposit(string account, int amount) {
    accounts[account]['balance'] += amount;
}
deposit('1', 100);
```



```
0 define {variable=accounts, type=struct}
1 pushstruct {}
2 pushstring {token=1}
3 pushtype {type=string}
4 pushkey {type=struct}
5 pushstruct {}
6 pushstring {token=balance}
7 pushtype {type=string}
8 pushkey {type=struct}
9 pushint {token=700}
10 pushstring {token=700}
11 pushtype {type=int}
12 pushvalue {type=string}
13 poptype {}
14 pushstring {token=details}
15 pushtype {type=string}
16 pushkey {type=struct}
17 pushstruct {}
18 pushstring {token=name}
19 pushtype {type=string}
20 pushkey {type=struct}
21 pushstring {token=Samuel Squire}
22 pushtype {type=string}
23 pushvalue {type=string}
24 poptype {}
25 pushtype {type=struct}
26 pushvalue {type=string}
27 poptype {}
28 pushtype {type=struct}
29 pushvalue {type=string}
30 poptype {}
31 pushtype {type=struct}
32 store {variable=accounts, type=struct}
33 createlabel {label=deposit}
34 define {variable=account, type=string}
35 define {variable=amount, type=int}
36 load {variable=accounts, type=struct, token=accounts}
37 pushstring {variable=account, type=string, token=account}
38 loadhashvar {}
39 pushstring {token=account}
40 pushstring {variable=balance, type=string, token=balance}
41 loadhash {}
42 pushstring {token=balance}
43 load {variable=amount, type=int, token=amount}
44 pluseq {}
45 return {}
46 pushargumentstr {argument=1}
47 pushargument {argument=100}
48 call {method=deposit}
```

Which executes twice because I haven't written guards.

```
{1={balance=900, details={name=Samuel Squire}}}
```



# AsyncAwait.java

It's possible to implement async/await using switch statements.

This schedules multiple tasks that each increment a counter.

# TokenRingTimer

This is an approach to concurrency and parallleism where the writer thread is passed around a ring. It achieves 951,098 requests per second.

# TokenRingTimer2

This is an advancement of the original TokenRingTimer and does reading as well as writing. It achives around 585,000 requests per second.

# TokenRingTimer2AsyncAwait

This is the async await script adapted into the token ring parallelism idea. There is one thread writing at any given point. 

# TokenRingTimer2AsyncAwait2

This is where the token ring actually distributes work across threads.

# MultiAwait

This distributes work across multiple threads.

# Async/await thread pool

This is an eagerly executing async/await thread pool.

There is a number of worker threads and each is executing a set asnc/await program

Each thread executes in a cycle of reading phase and writing phase. This allows thread safety.

The environment of the async/await task system is stored per thread. A `Run` is ran during the reading phase of the read. Any `Fork` events are appended to the next thread. The next thread shall process a `Fork` event and get a `Yield` event, which it

During a read phase events placed by other threads are processed.

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

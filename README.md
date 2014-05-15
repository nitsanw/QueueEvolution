examples- Queue Evolution
========
This repository has been setup as supporting collateral for my Queue Evolution talk.
This is a trimmed down and tidied up version of the revisited branch of my examples repository. The queues are
grouped to 4 groups:
- Lamport : The Lamport concurrent queue algorithm.
- Thompson : Martin Thompson's refinement of the Lamport algorithm to prevent read misses by introducing index chahe fields
  See talk [here](http://www.infoq.com/presentations/Lock-Free-Algorithms) and original code [here](https://github.com/mjpt777/examples).
- FF : Fast Flow SPSC algorithm. 
There are some benchmarks included:
- JMH Busy/Yield all out throughput
- Handrolled Yield/Busy throughput
- JMH RTT for a given burst size
- Single threaded offer/poll
- CirularArray read/write

In the data folder you'll find the data from which the slides were derived.
Finally in the presentation folder is the presentation itself.
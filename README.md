examples- Queue Evolution
========
This repository has been setup as supporting collateral for my Queue Evolution talk.
This is a trimmed down and tidied up version of the revisited branch. The queues are
grouped to 4 groups:
- Lamport : The Lamport concurrent queue algorithm, some initial optimizations. Original implementations from
  Martin T. Lock Free Programming talk, tidied up. Intermediate steps split to different implementations.
- Thompson : Martin's changes to memory layout and reducing cache coherence traffic. Original implementations from 
  Martin T. Lock Free Programming talk, tidied up. Intermediate steps split to different implementations. Further
  improvements to counters padding are made. Use unsafe to access array. Added class fields padding and data padding.
  Added sparse data support.
- Yak : Same algorithm as Martin but counters are inlined.
- FF : Fast Flow SPSC algorithm. Corrections to original. Padding everywhere, sparse data support. BQueue implementation
  and combined FFBuffer with BQueue offer method.
There are 3 performance tests:
...
In the data folder you'll find the data from which the slides were derived.
Finally in the presentation folder is the presentation itself.
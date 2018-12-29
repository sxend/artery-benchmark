artery-benchmark
==============

:memo:

best score of `ab -c 10 -n 10000 http://localhost:<port>/echo` (with warmup)

netty tcp
```
Percentage of the requests served within a certain time (ms)
  50%      3
  66%      3
  75%      3
  80%      3
  90%      4
  95%      4
  98%      6
  99%     13
 100%    193 (longest request)
```

artery tcp
```
Percentage of the requests served within a certain time (ms)
  50%      3
  66%      3
  75%      3
  80%      4
  90%      4
  95%      6
  98%     10
  99%     26
 100%    165 (longest request)
```

artery aeron-udp
```
Percentage of the requests served within a certain time (ms)
  50%      5
  66%      6
  75%      6
  80%      7
  90%      8
  95%     11
  98%     33
  99%     47
 100%    313 (longest request)
```

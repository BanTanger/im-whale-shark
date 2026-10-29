# 长连接网关容量模型调研（Netty / 单机几十万到百万连接）

调研对象：基线 `docs/architecture/im-v2-architecture.md`（`rewrite/v2` 分支）§2.1 L2 指标、§6.8 心跳与重连、§6.9 平滑重启、§9 性能预算。
对应 GitHub issue：`#107 长连接网关调研（Netty 容量模型）`。

本文只做取证与对照，不改基线、不写实现代码。每条结论后附可点开的 URL；引不到原文的，明确标 `[未取得原文]`。

**读法与置信度标记**

| 标记 | 含义 |
|---|---|
| 一手 | 厂商/项目官方文档、规范正文、源码、作者本人的测量记录 |
| 二手 | 转述他人测量结果（如邮件列表转述博客表格）；数字可用，口径以原文为准 |
| 推断 | 由已引数字做的算术推导，无直接测量 |

---

## 0. 结论摘要

1. **每连接内存：可引用区间是「应用进程侧 8–17 KB + 内核侧 ~4 KB」**，1M 空闲连接对应 **12–20 GB**，规划取 **13–20 KB/连接**。基线 §2.1 的「内存 1.5–3 GB」在数量级上低了 4–8 倍（§3、C1）。
2. **基线 §9 的「每消息 10 µs」不是预算，是 10 万 msg/s 的倒数**。它与同一张表里「解码+鉴权 < 100 µs」「网关→api 1–2 ms」自相矛盾；实测单线程扇出写一条就要 **12.5 µs**（§8、C2）。
3. **1M 连接 + 10 万 msg/s 同时成立时，30 s 心跳本身吃掉了 33% 的消息预算**（33333 次/秒）。这是 1M 连接真正的成本，不是内存（§8.3）。
4. **epoll ET 在 Netty 4.1 是默认，4.2 已被整体移除改成 LT**（PR #14031）。ET 下 `maxMessagesPerRead` 无法生效 —— 一条热连接可以独占 event loop，这在 1M 连接分片上是有意义的失败模式（§4）。
5. **JDK21 虚拟线程在 Netty EventLoop 上没有官方支持，且官方明确表示不打算做**：chrisvest「event loop threads should be platform threads」；franz1981「virtual threads doesn't play nicely with JNI code, or NIO, there is no point to invest in that area」（§7）。
6. **基线 §6.8 的「`IdleStateHandler` + `HashedWheelTimer`，O(1)/事件」在事实层面是错的**：`IdleStateHandler` 用的是 EventLoop 自己的 `PriorityQueue` 定时队列，跟 `HashedWheelTimer` 没有调用关系（§5.4、C3）。
7. 与基线冲突 **5 条**（C1–C5），另有 2 条「结论对但理由要改」（C4）与「前提未落实」（§7.3）。

---

## 1. 领域基线：Linux 侧的硬数字

先把不属于 Netty 的那部分定下来，否则后面所有「每连接成本」都会算漏。

### 1.1 内核 socket 的固定成本

| 项 | 值 | 出处 |
|---|---|---|
| `tcp_rmem` 最小值 | **4 KB**（"guaranteed to each TCP socket, even under moderate memory pressure"） | [ip-sysctl.txt](https://www.kernel.org/doc/Documentation/networking/ip-sysctl.txt) |
| `tcp_wmem` 最小值 | **4 KB** | 同上 |
| `tcp_rmem` 默认值 | 87380 B（约 85 KB），上限 87380 B–6 MB 随内存 | 同上 |
| `tcp_wmem` 默认值 | 16 KB，上限 64 KB–4 MB 随内存 | 同上 |
| `somaxconn` 默认 | **4096**（Linux 5.4 之前是 128） | 同上 |
| 内核 per-socket 结构（`tcp_sock` + inode + dentry + file），实测 | **~4 KB/socket**，记在 slab，**不计入进程 RSS** | [Centrifugo 实测](https://centrifugal.dev/blog/2026/08/17/tuning-idle-connections) |

两条容易踩的：

- 内核结构**不在进程 RSS 里**，`ps`/`top`/容器 RSS 都看不见，但 cgroup v2 会计入容器限额。做容量规划必须单独加。
- `somaxconn` 默认 4096，但应用侧的 `listen()` backlog 也要显式调大 —— `somaxconn` 只是上限。Netty 的 `SO_BACKLOG` 不设就用 JDK 的 `NetUtil.SOMAXCONN`（Linux 上 128），这是重连风暴时 SYN 被丢的直接原因。

### 1.2 内核 keepalive 的检测时延

| 参数 | 默认 | 含义 |
|---|---|---|
| `tcp_keepalive_time` | **7200 s（2 h）** | 空闲多久开始探活 |
| `tcp_keepalive_intvl` | **75 s** | 探测间隔 |
| `tcp_keepalive_probes` | **9** | 最多 9 次 |

出处：[tcp(7)](https://man7.org/linux/man-pages/man7/tcp.7.html)。内核文档自己的结论："connection will be aborted after ~11 minutes of retries"（[ip-sysctl.txt](https://www.kernel.org/doc/Documentation/networking/ip-sysctl.txt)）。

**所以默认配置下，一条被 NAT/防火墙静默丢弃的连接要 2 h 11 min 才被内核回收。** 这就是「业务心跳不可省」的量化依据：不是 TCP keepalive 不好，是它的默认时间尺度（小时级）和产品的在线状态语义（秒级）差三个数量级。

`TCP_USER_TIMEOUT`（Linux 2.6.37+）是更精确的工具：它限制「已发出的数据最多多久没被 ACK 就断开」，且与 `SO_KEEPALIVE` 同用时覆盖 keepalive 的判定（[tcp(7)](https://man7.org/linux/man-pages/man7/tcp.7.html)）。它解决的是「有数据要发但发不出去」，不是「空闲连接是否还活着」——两者不能互相替代。

---

## 2. 每连接内存：可引用的数字区间

### 2.1 已公开的实测（按年代）

| 来源 | 技术栈 | 连接数 | 应用进程侧/连接 | 内核侧 | 口径说明 | 置信度 |
|---|---|---|---|---|---|---|
| Urban Airship "C500K"(2010) | Java + **Netty 3** | 80,000 | **8.9 KB** | 未计 | 711 MB / 80,000，原表语义为"增量" | 二手（[邮件列表转述](https://lists.jboss.org/pipermail/netty-users/2010-August/003069.html)，原博文 `[未取得原文]`，Wayback 仅剩 301 跳转） |
| colobu C1000K(2015) | **Netty 4** + JVM `-Xms12G -Xmx12G -Xss1M` | 1,200,000 | **12.9 KB** | 含在整机 used 里 | 整机 `used 15432 MB` / 1.2 M | 一手（[colobu.com](https://colobu.com/2015/05/22/implement-C1000K-servers-by-spray-netty-undertow-and-node-js/)） |
| Centrifugo PRO(2026) 默认配置 | Go | 200,000 | **27.0 KB**（栈 12.3 + 堆 12.7 + 运行时 2） | +4 KB | 空闲 WS，25 s ping | 一手（[centrifugal.dev](https://centrifugal.dev/blog/2026/08/17/tuning-idle-connections)） |
| Centrifugo PRO(2026) 调优后 | Go | 1,000,000 | **11.6 KB**（栈 4.1 + 堆 5.2） | +4.2 GB（1 M × 4 KB） | 同上；整机 15.5 GB | 一手（同上） |
| Hical(2026) 调优后 | C++ / Boost.Asio | 500,000 | **7.58 KB** | 未计入 RSS | 5 档位线性回归，R²=1.0000 | 一手（[掘金](https://juejin.cn/post/7650675966520541222)） |

### 2.2 区间结论

- **空闲连接的每连接内存下限，在刻意抹掉 per-connection 缓冲之后是 7.6–11.6 KB（应用侧）+ ~4 KB（内核）**。C++ 的 7.58 KB 和 Go 的 11.6 KB 是两个独立实现打出来的同一个量级，可信。
- **Java/Netty 侧没有官方数字**。Netty 官方文档、wiki、Javadoc 都不发布 per-connection footprint。可引用的实测只有 2010 年 Netty 3 的 8.9 KB 和 2015 年 Netty 4 的 12.9 KB 两个点，而且都不是「抹掉缓冲后」的极限值。
- 因此 **Java/Netty 每连接内存的合理规划值：13–17 KB（应用 + 内核），1 M 连接 ≈ 13–17 GB**；留 30% 余量后按 **17–22 GB** 规划。

这个「没有官方数字」本身就是 §3 的一条行动项：本项目要在压测里自己测出这条线的斜率，否则「单机 100 万」这个目标不可证伪。

### 2.3 怎么测才可信（可直接照搬的方法）

[Hical 的实测](https://juejin.cn/post/7650675966520541222)给出的方法论比数字本身更有用：

1. 造不满百万就**别硬造**。分档（1 万 / 2 万 / 3 万 / 5 万 / 10 万）量服务端 RSS，做最小二乘线性回归，取斜率即 KB/连接，看 R² 是否贴近 1。
2. 关键验证是**用一个更高档位的实测点去验证外推线**（他在 10 万点验证了 5 万外推的斜率，偏差 0），而不是只报告 R²。
3. 单机自连接（客户端与服务端同机）会污染三项数据：环回不走真实网卡、客户端进程抢 CPU、建连延迟混入客户端调度延迟。**百万级的终极验证必须多机。**

他自己踩过的坑也是可复用的：四元组墙（单源 IP 单目标端口 ≈ 6 万）、`ulimit -n` 的 hard limit 与 `/proc/sys/fs/nr_open` 三层限制、`nf_conntrack_max` 默认 26 万在 50 万连接时直接撑爆、`tcp_mem` 水位。

### 2.4 Netty 侧的参数与它们的实际效果

以下是 Netty 4.1 的默认值，全部取自源码（`4.1` 分支），不是博客转述：

| 参数 | 默认值 | 影响 | 出处 |
|---|---|---|---|
| `PooledByteBufAllocator` pageSize | 8192 | 分配粒度 | [`PooledByteBufAllocator.java`](https://github.com/netty/netty/blob/4.1/buffer/src/main/java/io/netty/buffer/PooledByteBufAllocator.java) `io.netty.allocator.pageSize` |
| `maxOrder` | **9** | chunk = 8192 << 9 = **4 MiB**（不是很多博客说的 16 MB） | 同上 `io.netty.allocator.maxOrder` |
| arena 数 | `min(2×cores, maxMemory/chunk/2/3)` | 每个 arena 3 个 chunk 上限，约不超过 50% 最大堆 | 同上 |
| 线程本地缓存 | `smallCacheSize=256`、`normalCacheSize=64`、`maxCachedBufferCapacity=32 KB`、`cacheTrimInterval=8192` | 每个 EventLoop 线程持有的缓存缓冲，**不随连接数增长** | 同上 |
| `useCacheForAllThreads` | **false** | 虚拟线程不缓存，避免 thread-local 堆积 | 同上 + [discussion #13636](https://github.com/netty/netty/discussions/13636) |
| `AdaptiveRecvByteBufAllocator` | min **64** / initial **2048** / max **65536** | 每连接接收缓冲的猜测区间 | [`AdaptiveRecvByteBufAllocator.java`](https://github.com/netty/netty/blob/4.1/transport/src/main/java/io/netty/channel/AdaptiveRecvByteBufAllocator.java)（注意：Javadoc 里写的 "starts from 1024" 已过期，常量是 2048） |
| `maxMessagesPerRead` | **16** | 每次读事件最多读 16 次 | [`AbstractNioByteChannel.java`:45](https://github.com/netty/netty/blob/4.1/transport/src/main/java/io/netty/channel/nio/AbstractNioByteChannel.java) `new ChannelMetadata(false, 16)`；epoll 同值 |
| `WriteBufferWaterMark` | **low 32 KB / high 64 KB** | 每连接写缓冲的高低位水位 | [`WriteBufferWaterMark.java`:38](https://github.com/netty/netty/blob/4.1/transport/src/main/java/io/netty/channel/WriteBufferWaterMark.java) |
| `connectTimeoutMillis` | 30000 | | [`DefaultChannelConfig.java`:50](https://github.com/netty/netty/blob/4.1/transport/src/main/java/io/netty/channel/DefaultChannelConfig.java) |

**最容易被忽略的一条**：`WriteBufferWaterMark` 只是"通知"，不是"限制"。Netty 不因为你超过 high water mark 就拒绝写入 —— 它只是把 channel 置为 `isWritable() == false`。如果业务侧在 1M 连接上无条件 `writeAndFlush`，一个慢客户端就能让出站队列无限增长。这是 1M 连接场景下唯一能造成**单连接 OOM** 的路径，比每连接固定成本危险得多。

### 2.5 ByteBuf 池化：4.1 与 4.2 是两种策略

- **Netty 4.1**：默认 `PooledByteBufAllocator`（jemalloc 思路，arena / chunk / size-class）。
- **Netty 4.2**：默认改为 `AdaptiveByteBufAllocator`。官方原文："In Netty 4.1.x the adaptive memory allocator was experimental. We have now made it the new default instead of the pooled allocator. We believe that most workloads will observe reduced memory usage, with performance that is on par or slightly better than the pooled allocator. The adaptive allocator automatically tunes itself to perform well for the observed workload. **It was also designed from the start to work well with virtual threads.**"（[4.2.0.Final 发布说明](https://netty.io/news/2025/04/03/4-2-0.html)，PR [#14271](https://github.com/netty/netty/pull/14271)）

本项目现在用的是 `netty-all 4.1.94.Final`（`pom.xml:53`），所以走的是 pooled 路径。这一条的实际意义：

- 池化的收益在**高消息率**下才成立（避免每条消息一次 direct buffer 分配/释放）；对**空闲连接占比高**的场景，池化的 chunk 是**按 arena 常驻**的，不随连接数释放，属于固定开销而不是每连接开销。
- 所以「启用池化」不会降低每连接内存，它降低的是 GC/分配开销。**把池化当成省内存手段是方向性错误。**

---

## 3. 与基线 §2.1 的对照：1.5–3 GB 不成立

基线 §2.1 L2：「单机连接 **100 万** … 内存 **1.5–3 GB**，FD ≥ 110 万」。

反推：1.5–3 GB / 1 M = **1.5–3 KB/连接**。

对照 §1.1 的硬约束：Linux 给每个 TCP socket 保证的最小收发缓冲各 4 KB —— 光是这两项的"权利"就是 8 KB/socket，已经 2.7–5.3 倍于该预算；再加实测的 ~4 KB/socket 内核 slab，**内核侧一项就 ≥ 4 KB，全部内核侧 ≥ 8 KB**。而应用侧最低的实测值是 C++ 框架刻意抹掉读缓冲后的 7.58 KB。

也就是说 1.5–3 GB 这个数字需要「内核侧 + 应用侧合计 < 3 KB/连接」，**与所有公开实测相差 4–8 倍**。

> **结论**：1M 连接的合理内存预算是 **13–20 GB**，机器内存起点 **32 GB**（留出 JVM 堆外、页缓存、突发）。这个数字直接决定了 L2 的压测机型，属于「不拍板就买错机器」的问题。

详见 C1。

---

## 4. epoll ET / LT：Netty 自己已经改主意了

### 4.1 事实

| 版本 | 默认模式 | 可否切换 | 出处 |
|---|---|---|---|
| Netty 4.0.16+ / 4.1 | **Edge-Triggered** | 可以，`EpollChannelOption.EPOLL_MODE` | [`io.netty.channel.epoll` 包说明](https://netty.io/4.1/api/io/netty/channel/epoll/package-summary.html)："uses EPOLL Edge-Triggered Mode for maximal performance" |
| Netty 4.2 | **Level-Triggered only** | 不能，`EPOLL_MODE` 变成 no-op | [PR #14031](https://github.com/netty/netty/pull/14031)（2024-05-03 合入 4.2.0.Alpha1）；[`EpollMode` 4.2 Javadoc](https://netty.io/4.2/api/io/netty/channel/epoll/EpollMode.html)："Deprecated. Netty always uses level-triggered mode." |

PR #14031 的动机原文："We supported edge-triggered and level-triggered modes for our epoll transport. This made things more complex while not really providing much value."

> 补充：Netty 自己的文档目前还没对齐 —— 4.2 的 epoll 包说明仍写着 edge-triggered，已有人开 issue 指出（[#16427](https://github.com/netty/netty/issues/16427)，2026-03）。所以看到"Netty 用 ET 追求极致性能"的说法，要注意它描述的是 4.1 及以前。

### 4.2 ET 在长连接网关上真正的风险

不是性能，是**公平性**。Netty 4.1 的 Javadoc 写得很直接：

> "Default is `EpollMode.EDGE_TRIGGERED`. If you want to use `isAutoRead()` false or `getMaxMessagesPerRead()` and have an accurate behaviour you should use `EpollMode.LEVEL_TRIGGERED`." —— [`EpollChannelConfig`](https://netty.io/4.1/api/io/netty/channel/epoll/EpollChannelConfig.html)

换个说法：**ET 模式下 `maxMessagesPerRead=16` 不生效**。因为 ET 的语义是"不读到 EAGAIN 就不会再通知你"，所以 Netty 必须一直读到 EAGAIN 为止。

放到 IM 网关的具体场景：

- 1M 连接按 16 个 event loop 均摊 → 每个 loop 挂 ~62500 条连接。
- 一个热门万人群爆发时，其中一个连接在短时间内持续有数据可读。ET 下这个 loop 会**一直读这条连接直到 EAGAIN**，其余 62499 条连接在这一轮里得不到调度。
- LT 下 `maxMessagesPerRead=16` 可以把单连接单轮的处理量卡住，把剩下的连接向前排。

这不是理论推演，是 `maxMessagesPerRead` 这个参数存在的理由。Netty 4.2 砍掉 ET 也是同一个方向。

**对基线的意义**：`netty-all 4.1.94` 上跑，默认就是 ET。如果要守住 §9 的 p99 100–120 ms，在 1M 连接密度下建议显式设 `EPOLL_MODE = LEVEL_TRIGGERED`（4.1 支持，且必须在 channel 注册前设置），或者直接升 4.2。

### 4.3 顺带：SO_REUSEPORT 与多进程

Netty 支持 `EpollChannelOption.SO_REUSEPORT`（Linux 3.9+），让多个网关进程共享同一监听端口。但内核按四元组哈希分配新连接，**不做负载感知**：进程数增加时连接数分布会偏向已有进程，且不解决"连接与路由表绑定"的问题。多进程适合吃满 CPU 核，不适合做分片。

---

## 5. 心跳 / 超时 / 半开连接检测

### 5.1 业界取值区间

| 系统 | 心跳间隔 | 判定超时 | 判定/间隔 | 出处 |
|---|---|---|---|---|
| Linux 内核 keepalive 默认 | 7200 s | +9×75 s ≈ 2 h 11 min | — | [tcp(7)](https://man7.org/linux/man-pages/man7/tcp.7.html) |
| RFC 5382 NAT 最低要求 | — | established ≥ **2 h 04 min** | — | [RFC 5382](https://www.rfc-editor.org/rfc/rfc5382.html) REQ-5 |
| 蜂窝网络 NAT 实测 | — | **< 5 min ~ > 30 min** | — | [ACM CoNEXT 2013](https://conferences.sigcomm.org/co-next/2013/program/p211.pdf)："ranging from less than 5 mins to more than 30 mins in today's cellular networks" |
| Fastmail（push 运维方） | — | **2–30 min**，视设备而定 | — | [Fastmail 工程博客](https://www.fastmail.com/blog/push-events-nat-tcp-connection-timeouts-and-device-sleep/) |
| MQTT 3.1.1 | 客户端自定（≤ 65535 s） | **1.5 ×** | 1.5 | [OASIS 规范 MQTT-3.1.2-24](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) |
| Socket.IO v4 | **25 s** | **20 s**（客户端侧 45 s） | 0.8 / 1.8 | [socket.io server options](https://socket.io/docs/v4/server-options/) |
| Centrifugo | **25 s** | **8 s**（合计 33 s） | 0.32 | [Centrifugo 配置](https://centrifugal.dev/docs/server/configuration) |
| **基线 §6.8** | **30 s** | **90 s** | **3.0** | — |

### 5.2 三个可落地的结论

1. **30 s 的间隔是对的，而且有 4–10 倍安全余量**。蜂窝 NAT 的最短实测值是 5 min，30 s 是它的 1/10。要把间隔拉长到 4 min 也能活（微信早期是 4.5 min，`[未取得原文]`，二手转述不足采信），但会牺牲在线状态精度。
2. **3 倍的判定系数是业界里最松的一档**。主流是 1.3–1.8×：MQTT 1.5×、Socket.IO 1.8×、Centrifugo 1.3×。3× 的直接后果是**一条已死的连接最长 90 s 内仍被当作在线**：这 90 s 里发给它的消息全部白投（走 MQ、走目标网关、写到一条死 socket 上），而且它继续占着连接表、内存、fd 名额。
3. **半开连接的检测必须靠应用层心跳，不能靠 TCP**。三者的分工要写清楚：

   | 机制 | 解决什么 | 时间尺度 | 代价 |
   |---|---|---|---|
   | `SO_KEEPALIVE` | 内核回收"应用已经忘了的" socket | 小时级（默认 2 h） | 零（内核） |
   | `TCP_USER_TIMEOUT` | 已发出但长期未被 ACK 的数据 | 秒级，需显式设 | 零 |
   | 应用心跳 | 判定"对端是否还活着"，驱动在线状态 | 秒级 | 每连接每周期一次全链路（客户端→网关→状态） |

   RFC 5382 和 Fastmail 都指向同一件事：**NAT 侧的保活要求（≥2 h）与运营商的现实（5–30 min）差一个数量级，所以只能按最短的那个来设计**。

### 5.3 心跳的成本必须进容量模型

1M 连接 / 30 s = **33,333 次/秒**。相对基线 §2.1 的 10 万 msg/s，这是 **33%** 的固定开销。

- 如果心跳算进 10 万 msg/s，真实业务消息只剩 6.7 万/s。
- 如果把心跳间隔拉到 60 s（仍然安全，是 NAT 最短值的 1/5），降到 16,667/s，释放 17% 预算。

建议：**在 §9 里把"心跳"单列一行，不要和业务消息共用预算**，否则压测目标会含糊。

### 5.4 基线 §6.8 的实现描述有事实错误

基线原文：「断连判定 | `IdleStateHandler(READER_IDLE)` + `HashedWheelTimer`，O(1)/事件」。

读 Netty 4.1 源码：

- `IdleStateHandler.initialize()` 调 `schedule(ctx, new ReaderIdleTimeoutTask(ctx), readerIdleTimeNanos, NANOSECONDS)`，而 `schedule()` 的实现是 `ctx.executor().schedule(task, delay, unit)`（[`IdleStateHandler.java`](https://github.com/netty/netty/blob/4.1/handler/src/main/java/io/netty/handler/timeout/IdleStateHandler.java)）。
- 也就是走 EventLoop 自己的定时任务队列 `AbstractScheduledEventExecutor.scheduledTaskQueue`，类型是 `DefaultPriorityQueue<ScheduledFutureTask<?>>`（[`AbstractScheduledEventExecutor.java`](https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/concurrent/AbstractScheduledEventExecutor.java)）。
- **`HashedWheelTimer` 完全没有被 `IdleStateHandler` 使用。** 它是 `io.netty.util.HashedWheelTimer`，默认 512 个 slot 的时间轮，需要显式 `new` 才用得上。

所以 O(1)/事件 的说法对不上：优先级队列是 O(log n) 插入、O(log n) 取消。1M 连接 / 30 s 心跳 = 33k 次/秒的 schedule + cancel，每次 O(log 62500) ≈ 16 次比较 —— 总开销仍然可忽略，但**结论对不代表理由对**，而这个理由会误导后续优化（有人真去换 HashedWheelTimer 是白做功）。

另外：`IdleStateHandler` 每连接持有一个 `ScheduledFuture` 引用 + 一个 task 对象，这是每连接的固定堆开销，属于 §2 里那 8–17 KB 的一部分。

---

## 6. 重连退避

### 6.1 两个有明确参数的公开规范

| 来源 | INITIAL | MULTIPLIER | MAX | JITTER | 备注 |
|---|---|---|---|---|---|
| [gRPC Connection Backoff Protocol](https://raw.githubusercontent.com/grpc/grpc/master/doc/connection-backoff.md) | **1 s** | **1.6** | **120 s** | **±20%** | 另有 `MIN_CONNECT_TIMEOUT = 20 s`；收到 SETTINGS 帧后重置 |
| [AWS "Exponential Backoff And Jitter"](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/) | base | 2 | cap | 多种：Full / Equal / Decorrelated | 结论是 no-jitter 是明确的输家；Full Jitter 工作量最少 |
| **基线 §6.8** | 5 s | 未定 | 未定 | "随机"（分布未定） | — |

### 6.2 基线这条描述的问题

「等下次有消息要发或 **5 s 后随机退避**重连（错峰）」这句话缺三个要素：**倍增因子、上限、抖动分布**。缺了这三样的"退避"在重连风暴下等价于固定延迟。

量化一下最坏情况：一次网关重启，1M 客户端同时收到 FIN。

- 按「5 s ± 随机」：如果抖动区间是 ±5 s，则 1M 个 SYN 落在 10 s 窗口内 = **100,000 SYN/s**。按 §1.1，`somaxconn` 默认 4096、Netty `SO_BACKLOG` 默认 128 —— accept 队列会溢出，内核丢 SYN，客户端按 TCP SYN 重传退避（1s / 3s / 7s…）重试，形成二次风暴。
- 按 gRPC 参数（1 s ×1.6 上限 120 s，±20% 抖动）：1M 个连接分散在约 2 min 内，峰值约 8,000–15,000/s，可控。

**建议**：直接采用 gRPC 那组参数（1 s / 1.6 / 120 s / ±20%），或者 AWS Full Jitter（`sleep = random(0, min(cap, base × 2^attempt))`）。两组都有正式文档背书，比自己拍一个"5 s 随机"好，而且服务器端的 `SERVER_DRAIN`（§6.9 已有）可以和它配合：服务端分批下发 drain，本身就是一次退避。

### 6.3 服务端侧：accept 速率是独立预算

客户端退避只能削峰，不能改变「1M 连接要重建」这个事实。补充三个必须调的：

- `net.core.somaxconn` 与应用 `SO_BACKLOG` 同时调大（Netty 显式 `.option(ChannelOption.SO_BACKLOG, ...)`）。
- `net.ipv4.tcp_max_syn_backlog`。
- K8s 节点的 `nf_conntrack_max`（默认常见 65536 / 262144，1M 连接直接撑爆）：[CoNEXT 2013](https://conferences.sigcomm.org/co-next/2013/program/p211.pdf) 讨论的是 NAT 侧，但 K8s 的 kube-proxy iptables 模式同样依赖 conntrack，这是同一个坑。

---

## 7. 虚拟线程在 Netty EventLoop 上的官方立场

### 7.1 原文

**chrisvest（Netty 维护者）**，2023-09-22，[discussion #13636](https://github.com/netty/netty/discussions/13636)：

> "The event loop threads should be platform threads, but you can use virtual threads for offloading message processing just fine. The only issue is the use of thread-locals in the pooled bytebuf allocator, but the `io.netty.allocator.useCacheForAllThreads` system property is `false` by default, so those thread-locals won't actually pile up. Allocation will just be a bit slower in virtual threads."

**franz1981（Netty 协作者）**，2025-01-14，[issue #14636](https://github.com/netty/netty/issues/14636)（该 issue 以「completed」关闭）：

> "Since Netty is a networking framework, it doesn't provide support per se, but will make sure the components which could interact with virtual threads, to perform as expected (e.g. by not using thread local in the virtual thread path). … from a Netty perspective, since virtual threads doesn't play nicely with JNI code, or NIO, there is no point to invest in that area; it will just add delay while dealing with I/O events, for no gain to final users."

同一 issue 里 franz1981 指向 Quarkus 的虚拟线程指南作为"上层框架自己提供"的范例。

用户提「用 `Executors.newVirtualThreadPerTaskExecutor()` 做 EventLoopGroup 的 executor」时，normanmaurer 直接关闭了该 issue（[#13720](https://github.com/netty/netty/issues/13720)，2024-02-13）—— 即官方不接受这种用法。

### 7.2 Netty 实际为虚拟线程做过的两件事

官方不做"EventLoop 跑虚拟线程"，但确实做了兼容性工作，这两条决定了「把虚拟线程用在非热路径」是安全的：

1. **避免在虚拟线程路径上使用 ThreadLocal**：`io.netty.allocator.useCacheForAllThreads` 默认 `false`，池化分配器不会在每个虚拟线程上挂缓存。
2. **为长寿虚拟线程补 FastThreadLocal 支持**：PR [#15222](https://github.com/netty/netty/pull/15222)（2025-05-22）新增包装 API，让长生命周期虚拟线程也能用 FastThreadLocal。原文动机："Recent changes to avoid ThreadLocals in virtual threads made many optimizations unworkable for those threads. In some scenarios, however, virtual threads live long enough…"
3. 已知危险：在 event loop 里用虚拟线程跑阻塞代码会与分配器的 `ReentrantLock` 交互出死锁风险，见 issue [#13204](https://github.com/netty/netty/issues/13204)。

### 7.3 基线 §9 的结论对，理由要改（C4）

基线：「EventLoop 用平台线程（Netty 官方立场：**event loop 是 CPU 密集轮询**，虚拟线程只增加调度开销）」。

- **结论正确**：EventLoop 必须是平台线程。
- **理由不准确**：官方给的理由不是"CPU 密集轮询"，而是「event loop 会阻塞在 `epoll_wait`/JNI 里，虚拟线程在这里没有收益，只会在 I/O 事件处理上增加延迟」（franz1981 原文）。"CPU 密集"恰恰是相反的描述 —— event loop 大部分时间是**阻塞的在等**，不是忙的。
- 这个差别有实际后果：如果按"CPU 密集"去理解，会得出"那就多起几个平台线程压满 CPU"的结论；按官方理由，正确的推论是"event loop 数量跟着核数走，阻塞型工作扔到别的池"。

其次，基线 §9 后半句「虚拟线程只用在必须阻塞的地方，例如独立的历史消息查询线程池」是正确的，而且和 chrisvest 的说法完全一致（"you can use virtual threads for offloading message processing just fine"）。

### 7.4 前提未落实：本项目当前是 Java 8

- `pom.xml` 的 `maven-compiler-plugin` 用 `${java.version}`，该属性没有在本项目声明，来自 `spring-boot-starter-parent 2.3.2.RELEASE`，其默认值即 **1.8**；各子模块 `maven.compiler.source` 也显式写着 `8`。
- 虚拟线程需要 JDK 21 GA。所以 §9 关于虚拟线程的段落目前是**没有可执行前提的**：先把 Spring Boot 2.3.2（2020 年）和 JDK 8 升上去，这段结论才有意义。
- 顺带：Netty 4.2 要求 JDK8+（[4.2.0 发布说明](https://netty.io/news/2025/04/03/4-2-0.html)），升 4.2 不被 JDK 版本阻塞，被 Spring Boot / Netty 的 API 变更阻塞（`MultiThreadIoEventLoopGroup` 替代 `EpollEventLoopGroup` 等）。

---

## 8. 回答基线 §9 的两个数

### 8.1 「每消息 10 µs」不是预算，是倒推值

基线原文：「10 万 msg/s 单机预算：每消息 10 µs」。

`1 s / 100,000 = 10 µs`。它和上面那行"10 万 msg/s"是同一个数的两种写法，不构成独立预算。

三个具体问题：

**(a) 它是吞吐的倒数，却被放在延迟表里。** §9 的表是逐跳延迟预算（客户端→网关 10–30 ms、解码+鉴权 <0.1 ms、网关→api 1–2 ms、MQ 投递 1–3 ms…），合计 p99 60–120 ms。而同一节末尾冒出一个 10 µs 的"每消息预算"—— 10 µs 比表里最小的一跳（解码+鉴权 < 100 µs）还小一个数量级，比"网关→api 1–2 ms"小两个数量级。**这两个数不在同一个维度上，放在一起会被误读成"端到端只花 10 µs"。**

**(b) 如果按"CPU 预算"读，它漏掉了核数。** 10 万 msg/s 落在 16 个 event loop 上，每条消息实际可用的 CPU 时间是 `16 / 100,000 = 160 µs`，不是 10 µs。要写成预算，公式应该是 `cores × 1 s / msg_rate`。

**(c) 真实测量值已经超过 10 µs。** [colobu 2015](https://colobu.com/2015/05/22/implement-C1000K-servers-by-spray-netty-undertow-and-node-js/) 的实测：单线程向 120 万个 WebSocket 连接各发一条消息，总耗时 **15 秒** → 摊薄 **12.5 µs/条**。这还只是「编码 + `writeAndFlush` 到已建立的 channel」这一段，**不含**解码、鉴权、gRPC、MQ 投递。也就是说，光是 §9 表的最后一行"目标网关写出"的实测摊薄成本就是 12.5 µs，已经超过"每消息 10 µs 总预算"。

> **修订建议**：把「每消息 10 µs」改成两条可验证的：①`单机 10 万 msg/s @ 16 vCPU ⇒ 每消息 CPU 预算 160 µs`；②`单线程扇出吞吐 ≈ 8 万条/s（12.5 µs/条）`，并注明这是 write 路径实测值。

### 8.2 「单机 100 万连接」：成立，但成立的是「空闲的 100 万」

有正面证据，也有被忽略的约束条件。分开说：

**正面证据（1M 连接本身没问题）**

| 证据 | 数字 |
|---|---|
| [colobu 2015](https://colobu.com/2015/05/22/implement-C1000K-servers-by-spray-netty-undertow-and-node-js/) | Netty 4 单机 **1,200,006** 条 established，`ss -s` 实测；16 核 32 GB；广播 1.2 M 用时 15 s |
| [Centrifugo 2026](https://centrifugal.dev/blog/2026/08/17/tuning-idle-connections) | 单节点 **1,000,000** 条空闲 WS，2.33 核 / 11.3 GB / 16 vCPU 30 GB |
| [Hical 2026](https://juejin.cn/post/7650675966520541222) | 50 万点实测，每连接 7.58 KB 全程线性 |

所以「单机 100 万连接」在 Linux + Netty 上是**有实现先例的**，不是乐观估计。乐观的是**在同一台机器上同时成立 100 万连接 + 10 万 msg/s + p99 200 ms**。

**被忽略的三条约束**

1. **心跳吃掉三分之一预算。** 1M / 30 s = 33,333/s，占 10 万 msg/s 的 33%（§5.3）。剩下 6.7 万/s 才是业务消息，平摊到 100 万用户是 **0.067 msg/s/人**。
2. **重连风暴下 accept 速率是独立瓶颈。** 1M 连接重建，按 §6.2 算，即使按 gRPC 退避也要 2 min、峰值上万 SYN/s。`somaxconn` 默认 4096、Netty `SO_BACKLOG` 默认 128 —— 这两个默认值在 1M 规模下必然溢出。
3. **1M 连接的单机故障面太大。** 这不是性能问题，是运维问题：一次发布要迁移 100 万连接；一次 GC 停顿影响 100 万人；`SERVER_DRAIN`（§6.9）的"逐批关闭"如果按 1M 规模做，drain 窗口会到分钟级。

> **建议**：把「单机 100 万」定位为**压测上限**，部署单元取 **10 万–30 万/节点**（`[推断]`，依据是：故障面 ∝ 单机连接数，而单机成本在 10 万–100 万区间内是线性的，没有规模化收益）。100 万作为"我们的架构不会在百万连接下退化"的证明，而不是"我们每台跑 100 万"的常态。

### 8.3 一个正面结论：基线 §9 的三条硬约束方向都对

顺带确认，§9 的三条硬约束在取证上是站得住的：

1. **热路径不许有 Redis / RPC 往返** —— gRPC 一跳 1–2 ms 就是 10 万–20 万个 10 µs，方向正确。
2. **不许重复序列化** —— 与 §2.3 的旧实现对照，正确。
3. **出站用 `FlushConsolidationHandler`** —— Netty 确实提供该类（`io.netty.handler.flush.FlushConsolidationHandler`），作用是把同一 event loop 轮次内的多次 flush 合并成一次系统调用，正是"每消息一次 flush syscall"的解药。

第四条「Disruptor 现在不用」的判断也与本文证据一致：`maxMessagesPerRead=16` + ET 的"读到 EAGAIN"已经给出了批处理语义，跨线程交接的收益要等火焰图说话。

---

## 9. 网关无状态化的边界

「网关零业务」（§0 原则 2）不等于「网关无状态」。要区分三种状态：

| 状态 | 例子 | 归属 | 网关重启后 |
|---|---|---|---|
| **进程局部状态** | fd ↔ Channel、`ChannelPipeline`、连接级 Attribute、读缓冲 | **只能在网关内**，不可外置 | 随进程消失，**不恢复**；客户端重连重建 |
| **可重建的本地缓存** | `uid → Channel` 本地索引、本机在线数 | 网关内，作为上面那张表的派生视图 | 随进程消失，重连时重建 |
| **跨进程必须可见的状态** | 用户当前在哪个网关（路由）、会话成员关系、在线/离线、序列号 | **状态路由服务**，网关只读 | 不受影响 |

边界判据只有一条：**「如果网关进程在任意时刻被杀，这个状态需不需要被另一个进程接管？」**

- 需要接管 → 必须落在状态路由服务里，网关只持有带 TTL 的只读副本。
- 不需要接管 → 可以只存在网关内存里。

按这条判据检查基线：

| 基线位置 | 状态 | 判定 | 问题 |
|---|---|---|---|
| §6.1 连接建立与调度 | 网关地址注册到 Nacos | 正确，属于第三方 | 必须带 **TTL = 2–3 × 心跳周期**，否则网关 crash 后留下幽灵路由 |
| §6.8 心跳 | 纯本地，不查 Redis | 正确 | 但"在线状态"的对外可见性必须由路由服务的 TTL 派生，不能由网关自己维护 |
| §6.9 平滑重启 | 摘注册 → 下发 drain → 逐批关 fd | 正确 | 只覆盖"优雅退出"，**没有覆盖 crash**。crash 后 1M 条连接的路由条目要靠 TTL 自然过期 |

两条推论：

1. **路由 TTL 必须和心跳周期绑定**。心跳 30 s、判定 90 s ⇒ 路由 TTL 应取 90–180 s。TTL 太短会让正常抖动导致路由抖动；太长会让 crash 后的幽灵路由存活更久，期间投递给它的消息全部丢失。
2. **网关不能当"连接注册表"用**。1M 连接 × N 节点的路由表放在状态路由服务里；网关只持有本地那张 `uid → Channel` 的表，且明确它是"可随时丢弃的缓存"。`ChannelGroup` / `DefaultChannelGroup` 是进程内的，不能跨节点查询 —— 这点在 1M 规模下尤其要说清楚，否则会写出"遍历所有网关的 ChannelGroup"这种实现。

---

## 10. 与基线冲突清单

> 格式：① 原方案 → ② 新结论 → ③ 理由 → ④ 性能/成本差异。**不修改基线，只记录。**

### C1 §2.1 单机 100 万连接的内存预算

**① 原方案**：§2.1 L2 表 —— 「单机连接 100 万 … 内存 **1.5–3 GB**，FD ≥ 110 万」。

**② 新结论**：应改为 **13–20 GB**（应用侧 8–17 KB/连接 + 内核侧 ~4 KB/socket），机器内存起点 32 GB。

**③ 理由**：1.5–3 GB / 1 M = 1.5–3 KB/连接，低于 Linux 保证给每个 socket 的最小收发缓冲（4 KB + 4 KB，[ip-sysctl.txt](https://www.kernel.org/doc/Documentation/networking/ip-sysctl.txt)），也低于所有公开实测：C++ 调优后 7.58 KB（[Hical](https://juejin.cn/post/7650675966520541222)）、Go 调优后 11.6 KB + 4 KB 内核（[Centrifugo](https://centrifugal.dev/blog/2026/08/17/tuning-idle-connections)）、Netty 4 的 12.9 KB（[colobu](https://colobu.com/2015/05/22/implement-C1000K-servers-by-spray-netty-undertow-and-node-js/)）、Netty 3 的 8.9 KB（[Urban Airship](https://lists.jboss.org/pipermail/netty-users/2010-August/003069.html)）。

**④ 性能/成本差异**：低估 4–8 倍。若按 3 GB 选机型（如 8 GB 实例），1M 连接在 ~25 万时就会吃光这 3 GB（3 GB ÷ 13 KB/连接 ≈ 23 万），到不了 100 万；继续压会触发 OOM Killer 或 swap，且**内核 slab 不在进程 RSS 里，监控看不到逼近的过程**（[Centrifugo 实测口径说明](https://centrifugal.dev/blog/2026/08/17/tuning-idle-connections)）。直接成本差：1M 连接需要 32 GB 机型而非 8 GB，压测机型选错等于返工。

---

### C2 §9 「每消息 10 µs」

**① 原方案**：§9 —— 「10 万 msg/s 单机预算：每消息 10 µs」。

**② 新结论**：应拆成两条：`每消息 CPU 预算 = cores × 1 s / msg_rate`（16 核 10 万/s ⇒ 160 µs）与 `单线程扇出实测 ≈ 12.5 µs/条`（仅 write 路径）。

**③ 理由**：10 µs 是 10 万 msg/s 的倒数，不是独立预算；同一节表里的「解码+鉴权 < 100 µs」「网关→api 1–2 ms」都比它大 1–2 个数量级，两个维度混在一张表里会被误读。实测单线程向 1.2 M 连接各写一条耗时 15 s = 12.5 µs/条（[colobu](https://colobu.com/2015/05/22/implement-C1000K-servers-by-spray-netty-undertow-and-node-js/)），已经超过 10 µs，且不含解码/鉴权/gRPC/MQ。

**④ 性能/成本差异**：这是容量规划阶段的算术错误。把 10 µs 当"每条消息的 CPU 成本"，L1 的 750 万 QPS 只需要 `7.5e6 × 10e-6 = 75` 核，看起来单机就够；按实测 write 路径摊薄 12.5 µs/条算，已经要 `7.5e6 × 12.5e-6 ≈ 94` 核，而这 94 核**只覆盖"写出去"**，不包含解码、鉴权、gRPC、MQ 投递。用它反推网关台数会系统性偏小。

---

### C3 §6.8 「`IdleStateHandler` + `HashedWheelTimer`，O(1)/事件」

**① 原方案**：§6.8 表 —— 「断连判定 | `IdleStateHandler(READER_IDLE)` + `HashedWheelTimer`，O(1)/事件」。

**② 新结论**：`IdleStateHandler` 用的是 EventLoop 自己的 `DefaultPriorityQueue<ScheduledFutureTask>`，复杂度 O(log n)；`HashedWheelTimer` 未被使用，应删除该表述。IDLE 判定机制本身没问题。

**③ 理由**：`IdleStateHandler.schedule()` 的实现是 `ctx.executor().schedule(task, delay, unit)`（[源码](https://github.com/netty/netty/blob/4.1/handler/src/main/java/io/netty/handler/timeout/IdleStateHandler.java)），落到 [`AbstractScheduledEventExecutor`](https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/concurrent/AbstractScheduledEventExecutor.java) 的 `DefaultPriorityQueue`。全仓库搜索不到 `IdleStateHandler` 对 `HashedWheelTimer` 的引用。

**④ 性能/成本差异**：数字上影响可忽略（33k 次/s × O(log 62500) ≈ 16 次比较 ≈ 每事件几十 ns），但会误导优化方向：按"O(1)/事件"理解会认为定时器无需关注，按真实 O(log n) 理解才会想到"1M 连接时定时器密度是每 event loop 6 万条"。同时 `IdleStateHandler` 每连接的 `ScheduledFuture` + task 对象是每连接固定堆开销，应计入 §2 的 8–17 KB。

---

### C4 §9 虚拟线程的官方理由

**① 原方案**：§9 —— 「EventLoop 用平台线程（Netty 官方立场：**event loop 是 CPU 密集轮询**，虚拟线程只增加调度开销）」。

**② 新结论**：结论正确，理由应改为「event loop 会长期阻塞在 `epoll_wait` / JNI 调用上，虚拟线程在这里没有收益，只会给 I/O 事件处理增加调度延迟」。

**③ 理由**：chrisvest 原文是 "The event loop threads should be platform threads"（[#13636](https://github.com/netty/netty/discussions/13636)）；franz1981 原文是 "since virtual threads doesn't play nicely with JNI code, or NIO, there is no point to invest in that area; it will just add delay while dealing with I/O events, for no gain to final users"（[#14636](https://github.com/netty/netty/issues/14636)）。两处都没有说"CPU 密集"。

**④ 性能/成本差异**：无直接性能差异，但推论方向相反。"CPU 密集"会推出"加平台线程压满 CPU"；"阻塞在 epoll_wait"会推出"event loop 数 = 核数，阻塞型工作（历史消息查询、DB 调用）另起池"——后者才是正确的架构结论，也正是基线 §9 后半句在做的事。理由写错会让后来者按错的理由去优化。

**附**：该段落在本项目当前不成立 —— 工程是 JDK 8（`spring-boot-starter-parent 2.3.2.RELEASE` 的默认 `java.version`，各模块 `maven.compiler.source=8`），虚拟线程需要 JDK 21 GA。结论要等 JDK 升级后才可执行。

---

### C5 §6.8 心跳判定系数 3×

**① 原方案**：§6.8 —— 「心跳周期 30 s（服务端读空闲 **90 s** 判定超时）」，即判定 = 3 × 间隔。

**② 新结论**：建议改为 **1.5–2 ×**（45–60 s），或维持 30 s 间隔但把判定窗口压到 45 s。

**③ 理由**：业界主流是 1.3–1.8×：MQTT 规范 1.5×（[OASIS](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html)）、Socket.IO 20 s/25 s = 0.8×（客户端侧 1.8×，[文档](https://socket.io/docs/v4/server-options/)）、Centrifugo 8 s/25 s = 0.32×（[配置](https://centrifugal.dev/docs/server/configuration)）。3× 是这一组里最松的一档，没有见到支持它的依据。

**④ 性能/成本差异**：90 s 的判定窗口意味着一条已死连接最长 90 s 内仍被当作在线：这期间投给它的消息全部走完 MQ → 目标网关 → 写死 socket 的完整链路后丢弃（按 §9 表，单条成本 ≈ 1–3 ms MQ + 3–10 ms 消费 + <1 ms 写出）。按 1M 连接、1%/min 的连接异常率（`[推断]`，无出处，仅作量级示意）估算，稳态下有 ~15,000 条僵尸连接，每条每分钟被投递一次 ⇒ ~250 条/秒的无效投递链路。改为 45 s 可把该窗口减半，成本为零。

---

## 11. 未取得原文 / 待验证

| # | 事项 | 状态 | 影响 |
|---|---|---|---|
| 1 | Urban Airship "C500K in Action"(2010) 原始博文的表格 | `[未取得原文]`，Wayback 只剩 301 跳转，只能引 [netty-users 邮件列表的转述](https://lists.jboss.org/pipermail/netty-users/2010-August/003069.html) | 8.9 KB/连接 的表格语义（"增量"口径）无法核实，仅作量级参考 |
| 2 | Java / Netty 每连接内存的官方数字 | **不存在**。Netty 官方文档/wiki/Javadoc 均不发布 per-connection footprint | 只能靠本项目自己压测；§2.3 给了方法论 |
| 3 | Netty 4.1 在 JDK 21 上的 per-connection footprint | 无公开实测 | 影响 C1 的 13–17 GB 是否偏保守 |
| 4 | 微信/WhatsApp 心跳的官方数字（4.5 min 等） | `[未取得原文]`，只有二手转述 | 未在本文作为依据使用 |
| 5 | 蜂窝 NAT 超时的分运营商明细（移动 5 min / 电信 >28 min） | `[未取得原文]`，只有二手博客表格 | 本文改用 [CoNEXT 2013](https://conferences.sigcomm.org/co-next/2013/program/p211.pdf) 的"<5 min ~ >30 min"区间，这是一手 |
| 6 | Netty 4.2 的 ET→LT 变更对长连接网关的实际吞吐影响 | 无公开基准 | C3/§4 的公平性论断基于 Javadoc 语义推导，属于 `[推断]`，需自己压测验证 |
| 7 | `maxMessagesPerRead` 在 4.2 LT 下的实际效果 | 无公开数据 | 同上 |

> 注：第 5 项与 #4 的链接如失效，请以 CoNEXT 2013 论文与 RFC 5382 为准。

---

## 12. 对基线的建议改动（不代为修改，供决策）

按「不拍板就会返工」排序：

| 优先级 | 位置 | 建议 | 依据 |
|---|---|---|---|
| P0 | §2.1 L2 | 内存 1.5–3 GB → **13–20 GB**，机器 ≥ 32 GB | C1 |
| P0 | §9 | 删除「每消息 10 µs」，改为「每消息 CPU = cores×1s/msg_rate」+「单线程扇出 12.5 µs/条」 | C2 |
| P0 | §9 | 心跳成本单列（1M/30s = 33k/s），不与业务消息共用 10 万 msg/s 预算 | §5.3 |
| P1 | §6.8 | 删 `HashedWheelTimer`，改「EventLoop 优先级定时队列，O(log n)」 | C3 |
| P1 | §6.8 | 判定窗口 90 s → 45–60 s（1.5–2×） | C5 |
| P1 | §6.8 | 重连退避补全参数：1 s / ×1.6 / 上限 120 s / ±20% 抖动 | §6.1 |
| P1 | §9 | 虚拟线程理由改写；并注明依赖 JDK 21（当前 JDK 8） | C4 |
| P2 | §6.1 / §6.9 | 路由条目加 TTL（90–180 s），并覆盖 crash 场景 | §9 |
| P2 | §2.1 | 「单机 100 万」标注为**压测上限**，部署单元 10–30 万/节点 | §8.2 |
| P2 | §9 | 若 4.1 上跑，显式设 `EPOLL_MODE=LEVEL_TRIGGERED`；或规划升 4.2 | §4 |

---

## 13. 参考资料

**Netty 官方**
- Native transports：<https://netty.io/wiki/native-transports.html>
- 4.1 epoll 包说明（ET 模式）：<https://netty.io/4.1/api/io/netty/channel/epoll/package-summary.html>
- 4.1 `EpollChannelConfig`（ET 默认 / LT 与 `maxMessagesPerRead`）：<https://netty.io/4.1/api/io/netty/channel/epoll/EpollChannelConfig.html>
- 4.2 `EpollMode`（always LT）：<https://netty.io/4.2/api/io/netty/channel/epoll/EpollMode.html>
- PR #14031 移除 ET：<https://github.com/netty/netty/pull/14031>
- issue #16427 文档不一致：<https://github.com/netty/netty/issues/16427>
- 4.2.0.Final 发布说明（Adaptive 默认）：<https://netty.io/news/2025/04/03/4-2-0.html>
- PR #14271 切 adaptive 默认：<https://github.com/netty/netty/pull/14271>
- 内存分配器行为分析：<https://netty.io/wiki/analyzing-memory-allocator-behavior.html>
- 虚拟线程立场：#13636 <https://github.com/netty/netty/discussions/13636>、#14636 <https://github.com/netty/netty/issues/14636>、#13720 <https://github.com/netty/netty/issues/13720>
- 虚拟线程 FastThreadLocal 支持 PR #15222：<https://github.com/netty/netty/pull/15222>
- 虚拟线程 + 分配器锁死锁风险 #13204：<https://github.com/netty/netty/issues/13204>

**Netty 源码（`4.1` 分支）**
- `AdaptiveRecvByteBufAllocator`（64/2048/65536）：<https://github.com/netty/netty/blob/4.1/transport/src/main/java/io/netty/channel/AdaptiveRecvByteBufAllocator.java>
- `PooledByteBufAllocator`（pageSize 8192、maxOrder 9、4 MiB chunk、cache 参数）：<https://github.com/netty/netty/blob/4.1/buffer/src/main/java/io/netty/buffer/PooledByteBufAllocator.java>
- `WriteBufferWaterMark`（32 KB/64 KB）：<https://github.com/netty/netty/blob/4.1/transport/src/main/java/io/netty/channel/WriteBufferWaterMark.java>
- `AbstractNioByteChannel`（`ChannelMetadata(false, 16)`）：<https://github.com/netty/netty/blob/4.1/transport/src/main/java/io/netty/channel/nio/AbstractNioByteChannel.java>
- `IdleStateHandler`（用 `ctx.executor().schedule`）：<https://github.com/netty/netty/blob/4.1/handler/src/main/java/io/netty/handler/timeout/IdleStateHandler.java>
- `AbstractScheduledEventExecutor`（`DefaultPriorityQueue`）：<https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/concurrent/AbstractScheduledEventExecutor.java>

**Linux / TCP / NAT**
- `tcp(7)`：<https://man7.org/linux/man-pages/man7/tcp.7.html>
- `ip-sysctl.txt`：<https://www.kernel.org/doc/Documentation/networking/ip-sysctl.txt>
- RFC 5382（NAT TCP 行为要求，REQ-5 2h04m）：<https://www.rfc-editor.org/rfc/rfc5382.html>
- "Silent TCP Connection Closure for Cellular Networks"（蜂窝 NAT 超时 <5 min ~ >30 min）：<https://conferences.sigcomm.org/co-next/2013/program/p211.pdf>
- Fastmail：Push events, NAT TCP connection timeouts, and device sleep：<https://www.fastmail.com/blog/push-events-nat-tcp-connection-timeouts-and-device-sleep/>

**心跳 / 退避规范**
- MQTT 3.1.1（1.5× Keep Alive）：<https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html>
- Socket.IO server options（25 s / 20 s）：<https://socket.io/docs/v4/server-options/>
- Centrifugo 配置（ping 25 s / pong 8 s）：<https://centrifugal.dev/docs/server/configuration>
- gRPC Connection Backoff Protocol（1 s / 1.6 / 120 s / 0.2）：<https://raw.githubusercontent.com/grpc/grpc/master/doc/connection-backoff.md>
- AWS Exponential Backoff And Jitter：<https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/>

**实测 / 工程记录**
- Centrifugo PRO 空闲连接调优（1M 连接实测）：<https://centrifugal.dev/blog/2026/08/17/tuning-idle-connections>
- colobu C1000K（Netty 4 / 120 万 WebSocket）：<https://colobu.com/2015/05/22/implement-C1000K-servers-by-spray-netty-undertow-and-node-js/>
- Urban Airship C500K 表格转述（netty-users 邮件列表）：<https://lists.jboss.org/pipermail/netty-users/2010-August/003069.html>
- Hical 百万连接内存回归实测（R²=1.0000）：<https://juejin.cn/post/7650675966520541222>

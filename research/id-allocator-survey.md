# 发号器方案调研：会话内 `sequence` 与全局 `serverMsgId`

调研对象：GitHub issue #106。基线为 `rewrite/v2` 上的 `docs/architecture/im-v2-architecture.md`（1020 行，下文 §x.y 均指该文件行内章节）。
调研方式：只读一手资料（厂商原文 / 开源源码 / 官方文档），每条结论附可点开 URL；读不到原文的一律在 §7 标注。

---

## 0. 结论摘要

**推荐组合（两个号码分开，但都要改）**

| 号码 | 保留 | 必须补 | 必须换 |
|---|---|---|---|
| `serverMsgId` | 本地雪花、零网络调用 | ① `twepoch` 写死为上线时间 ② workerId 唯一性由 DB 唯一索引兜底 ③ 跨重启时钟校验 | — |
| `sequence` | 号段 + 进程内自增 | ① 双 buffer + 消耗 10% 异步预取 + step 自适应 ② 发号点按 `convId` 绑定实例 | 号段权威从 Redis 换成 OceanBase |

一句话：**`serverMsgId` 的方案是对的、实现细节全错；`sequence` 的方案形态是对的、存储选错了。** 前者靠"部署时人工保证唯一"，后者把唯一会回退的数据放在唯一会回退的存储上。

**与基线冲突 5 条**（详见 §5）：

| # | 基线 | 冲突点 | 严重度 |
|---|---|---|---|
| C-1 | §8.3 号段存 Redis | Redis 异步复制，failover 会丢已确认的 `INCRBY` → `seq` 回退 → 静默丢消息。号段权威应放 OB（Paxos，RPO=0） | 正确性 |
| C-2 | §8.3「单调性由每个 convId 的号段单点递增保证」 | 发号单调 ≠ 落库单调。同一 conv 被多实例各持一段号 → 乱序落库 → `fromSeq` 增量拉取**永久漏消息** | 正确性（架构级） |
| C-3 | §8.3 未定义号段续租时机 | 用完才取 = 请求 hang 在 I/O 上、TP999 尖刺（Leaf 原文原话） | 性能 |
| C-4 | §8.3 / §10「workerId 部署唯一 + 回拨拒绝发号」 | 漏掉跨重启回拨、workerId 重复、`twepoch` 未定三类失效 | 正确性 |
| C-5 | §8.2「碰撞也由数据库兜底」 | 唯一索引不是兜底，是报警器。没有恢复路径时它把"数据错乱"变成"数据丢失" | 正确性 |

---

## 1. 判据与范围

issue 给的三项硬指标，本票按"能否被证伪"的方式定义：

| 指标 | 定义 | 为什么它重要 |
|---|---|---|
| 单会话可达 QPS | 同一个 `convId` 每秒最多能拿到多少个严格递增的号 | 决定大群/热点会话会不会被发号器卡住 |
| 故障切换后的空洞行为 | 进程崩溃、存储层主从切换、发号点迁移三种故障下，号是"跳"还是"退" | **"退"= 重复号 = 消息丢失；"跳"= 空洞 = 无副作用** |
| 号段耗尽前能否续租 | 当前号段还剩多少时触发预取、预取失败时是阻塞还是快速失败 | 决定 p99 是平滑还是有周期性尖刺 |

不覆盖：UUID / 数据库自增（`auto_increment`）/ MongoDB ObjectId。理由：前者不可排序、后两者在 §8.2 已确定不用 MySQL 自增，与 `sequence` 的语义不匹配。

---

## 2. 五个方案速览（含一手参数）

### 2.1 微信 seqsvr：分段 + 内存 + 定期落地

原文《微信序列号生成器架构设计及演变》（曾钦松，微信高级工程师）：
<https://www.infoq.cn/article/wechat-serial-number-generator-architecture/>
本机访问该 URL 返回 HTTP 451，正文取自两处全文转贴（同一作者、同一正文，见 §7）：
<https://developer.aliyun.com/article/650782>（算法原理篇）、<https://www.harries.blog/archives/360283.html>（含容灾篇）

关键事实（均为原文数字，非推算）：

| 项 | 数值 | 出处 |
|---|---|---|
| 调用量 | 每天万亿级，峰值 ~10⁷ QPS | 原文首段 |
| 延迟 | 平均 1 ms，99.9% < 3 ms | 原文首段 |
| 部署 | 数百台 4 核机器 | 原文首段 |
| 步长 step | 实际生产 **10000** | 原文 5.1「实际应用中每次提升的步长为10000」 |
| 落盘频率 | 由 ~10⁷ QPS 降到 ~10³ QPS | 同上 |
| Section 粒度 | 1 个 Section = **10 万个 uid** 共享一个 `max_seq`，总数据 300+ KB | 原文 5.2 |
| 存储层 | StoreSvr 用多机 NRW 策略保证持久化后不丢失 | 原文 5.3 |
| 分配模型 | 内存 `cur_seq` / `max_seq`；`cur_seq > max_seq` 时 `max_seq += step` **并持久化 max_seq**；重启时读出 `max_seq` 赋给 `cur_seq` | 原文 5.1 |

**这是本票最重要的一条设计**：持久化的是**分配上界** `max_seq`，不是已用量 `cur_seq`。所以重启只会让下一个号从 `max_seq + 1` 开始 —— 跳跃，永不回退。空洞上限 = 一个 step。

容灾（原文容灾篇）：

- 硬约束：**任意时刻任意 uid 段有且仅有一台 AllocSvr 提供服务**。两台同时服务同一段会直接产生回退（原文图 5 举例：client 读到 101、201、102）。
- 单点服务 → 只能做故障切换，不能做多主机模型。切换靠**仲裁服务 + 租约**：
  - 租约失效：AllocSvr 在 N 秒内读不到 StoreSvr 的加载配置 → **停止服务**；
  - 租约生效：读到新配置后立即卸载旧段，加载的新段**等 N 秒后**才提供服务。
  - 两个条件保证"新机一定在旧机下线后才开始服务"，代价是**切换的号段有一小段不可服务**（原文：靠后台逻辑层重试 + 异步重试队列，用户无感知）。
- 1.0 是主备；2.0 把路由表（uid 段 → AllocSvr 全映射）**旁路进取号响应包**，Client 带路由版本号，版本旧就在响应里回一份新表。解决了"Client 与 AllocSvr 路由状态不一致"，才敢做动态号段迁移。

另外一条对基线有利的佐证：原文明确说不用"全局唯一 sequence"，因为"全局唯一的 sequence 会有非常严重的申请互斥问题"。这与基线 §7.1 把 `clientMsgId` / `serverMsgId` / `sequence` 拆成三个标识的方向一致 —— 微信也是"唯一性"与"顺序性"分开（原文点评里写作 msgId 与 seqId）。

注意粒度差异：微信的 `sequence` 是 **per-uid**（每个用户一个独立的 64 位序列空间），不是 per-conversation。形状相同（按分片键的内存号段），但分片键是 uid 而非 convId。

### 2.2 美团 Leaf：号段模式 + snowflake 模式

原文《Leaf——美团点评分布式ID生成系统》（照东，2017-04-21）：
<https://tech.meituan.com/2017/04/21/mt-leaf.html>
源码：<https://github.com/Meituan-Dianping/Leaf>

**Leaf-segment（对应我们的 `sequence`）**

| 项 | 数值 / 行为 | 出处 |
|---|---|---|
| 表结构 | `leaf_alloc(biz_tag PK, max_id, step, ...)` | 原文 + `README.md` |
| 取号 SQL | `Begin; UPDATE ... SET max_id = max_id + step WHERE biz_tag = xxx; SELECT ...; Commit` | 原文 |
| 双 buffer | 「当前号段已下发 10% 时，如果下一个号段未更新，则另启一个更新线程去更新下一个号段」 | 原文 |
| 触发阈值（源码） | `!buffer.isNextReady() && segment.getIdle() < 0.9 * segment.getStep()` | `SegmentIDGenImpl.java` |
| 两段都没好 | 返回 `EXCEPTION_ID_TWO_SEGMENTS_ARE_NULL = -3`（**异常，不阻塞**） | 同源码 |
| step 上限 | `MAX_STEP = 1000000` | 同源码 |
| step 自适应 | 上次填充耗时 < 15 min → step ×2；15–30 min → 不变；> 30 min → step ÷2（不低于 DB 里的 minStep）；`SEGMENT_DURATION = 15 * 60 * 1000` | 同源码 |
| 推荐 step | 「服务高峰期发号 QPS 的 600 倍（10 分钟）」，DB 宕机可撑 10–20 分钟 | 原文 |
| 实测吞吐 | 4C8G，**近 5 万/s，TP999 1 ms** | 原文 + `README.md` |
| 原文自陈的缺点 | 「TP999 数据波动大，当号段使用完之后还是会 hang 在更新数据库的 I/O 上」「DB 宕机会造成整个系统不可用」 | 原文 |
| 高可用 | MySQL 一主两从 + **半同步** + Atlas 切换；原文承认「一些情况会退化成异步模式，甚至在非常极端情况下仍然会造成数据不一致」 | 原文 |

**Leaf-snowflake（对应我们的 `serverMsgId`）**

- 位宽完全沿用 snowflake：**1 + 41 + 10 + 12**，`twepoch = 1288834974657`（2010-11-04 09:42:54 GMT+0800）。
- workerId 用 **ZooKeeper 持久顺序节点**自动分配；本机文件系统缓存一份 workerId 文件，做到"对 ZK 弱依赖"。
- 启动期时钟校验：与 `leaf_forever/${self}` 节点记录时间比对，若本机时间更小 → 认为大步长回拨，**启动失败并报警**；首次启动则与 `leaf_temporary` 下所有运行节点的系统时间均值比对，偏差超阈值同样拒绝启动。
- 运行期回拨处理（源码 `SnowflakeIDGenImpl.get()` 与原文代码块一致）：

```java
if (timestamp < lastTimestamp) {
    long offset = lastTimestamp - timestamp;
    if (offset <= 5) {          // ≤5ms：等待两倍时间后重试
        wait(offset << 1);
        timestamp = timeGen();
        if (timestamp < lastTimestamp) return new Result(-1, Status.EXCEPTION);
    } else {
        return new Result(-3, Status.EXCEPTION);   // >5ms：拒绝
    }
}
```

- 同毫秒内 sequence 的起点不是 0，而是 `RANDOM.nextInt(100)`（源码：`sequence = RANDOM.nextInt(100)`）。目的是不让外部从 ID 反推发号量（原文"信息安全"一节）。副作用：**同一毫秒内 `serverMsgId` 的序列部分无序**。
- 原文"缺点"一栏自陈：「强依赖机器时钟，如果机器上时钟回拨，会导致发号重复或者服务会处于不可用状态。」
- 真实生产证据：**2017 年闰秒那次出现过部分机器回拨**，靠上述策略避免了业务影响（原文）。

### 2.3 百度 uid-generator

源码与文档：<https://github.com/baidu/uid-generator>

| 项 | 数值 | 出处 |
|---|---|---|
| 默认位分配 | 1 + **28 位秒** + **22 位 workerId** + **13 位序列** | `README.md` |
| 时间位寿命 | 28 位秒，从 epoch 2016-05-20 起 **8.7 年 → 2024-11-20 21:24:16 耗尽** | `DefaultUidGenerator.java` 类注释原文 |
| workerId 上限 | 22 位 → 约 420 万 | `README.md` |
| 单 worker 序列上限 | 13 位 → **8192/s** | `README.md` |
| 时钟回拨 | `if (currentSecond < lastSecond) throw new UidGenerateException(...)` —— **任意回拨直接抛异常**，无阈值、无等待 | `DefaultUidGenerator.nextId()` |
| 序列耗尽 | `sequence == 0` 时 `getNextSecond()` **忙等自旋**到下一秒 | 同源码 |
| CachedUidGenerator | RingBuffer 预生成 + cacheline padding，**单实例 600 万 QPS** | `README.md` |
| RingBuffer 填充 | 初始化全量填充 + 可用比例低于 `paddingFactor` 时填充 + 定时填充 | `README.md` |

对我们的直接意义：**秒级时间位 + 13 位序列 = 单 worker 8192/s 硬顶**。我们 §2.1 的单机目标是 10 万 msg/s，照抄这套位分配需要 ≥ 13 个 workerId，且撞上限时是自旋阻塞而非快速失败 —— 与基线"不阻塞等待"的立场相反。这套位分配适合"低 QPS、海量 worker"的场景，不适合我们。

### 2.4 Redis `INCR` / `INCRBY`

一手文档：
- 持久化 <https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/>
- 复制 <https://redis.io/docs/latest/operate/oss_and_stack/management/replication/>
- `INCR` <https://redis.io/docs/latest/commands/incr/>
- benchmark <https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/benchmarks/>

原子性：`INCR` 是 O(1) 单 key 写命令（`complexity: O(1)`，`command_flags: [write, denyoom, fast]`），配合"Redis 从命令执行角度看基本是单线程的"（benchmark 文档原文：*Redis is, mostly, a single-threaded server from the POV of commands execution*），单 key 自增天然原子。

**持久化风险（这是关键，逐条引原文）**

| 配置 | 官方原话 | 最大丢失窗口 |
|---|---|---|
| RDB | *you should be prepared to lose the latest minutes of data* | 分钟级 |
| AOF `everysec`（默认） | *you may lose 1 second of data if there is a disaster* | 1 秒 |
| AOF `always` | *Very very slow, very safe*（支持 group commit） | ~0，但每次写都要 fsync |

**复制风险（比持久化风险更致命）**

- *Redis uses by default asynchronous replication* —— 主从默认异步。
- `WAIT` 也救不了：*it does not turn a set of Redis instances into a CP system with strong consistency: acknowledged writes can still be lost during a failover, depending on the exact configuration of the Redis persistence.*
- `min-replicas-to-write` 同样只是收窄窗口：*because Redis uses asynchronous replication it is not possible to ensure the replica actually received a given write, so there is always a window for data loss.*

吞吐参考（官方 `redis-benchmark`，本机未安装 redis 无法自测，故引用文档数字，**这是 SET 的同量级代理，不是 INCR 实测**）：

- `-t set -n 100000 -q` → **180180.17 req/s**，p50 0.143 ms
- `-t set -r 100000 -n 1000000`（随机 key、50 连接）→ **72144.87 req/s**，99.76% ≤ 1 ms
- `-P 16` 流水线 → 153 万 req/s（流水线数字不能用于"每消息一次往返"的模型）

### 2.5 Twitter Snowflake（原始）

仓库 <https://github.com/twitter-archive/snowflake> 已 retired，README 原话：*We have retired the initial release of Snowflake*，初版代码在 `snowflake-2010` tag。**本票未能读到原始 `IdWorker.scala`**（tag 的 git tree 返回空），故位宽与回拨行为以美团 Leaf 原文的转述 + Leaf / uid-generator 的实现为准（两者都是 snowflake 的公认忠实实现）。

业界通行的位宽与容量（Leaf 原文明确给出算式）：
- 41 位毫秒：`(1L<<41)/(1000*3600*24*365) = 69 年`
- 10 位机器：1024 台
- 12 位序列：`2^12` → 理论 **409.6 万/s**
- 原文缺点：「强依赖机器时钟，如果机器上时钟回拨，会导致发号重复或者服务会处于不可用状态。」

---

## 3. 三项硬指标对比

### 3.1 单会话可达 QPS

| 方案 | 单会话 QPS 上限 | 这个上限由什么决定 | 与"会话"的关系 | 出处 |
|---|---|---|---|---|
| 本地雪花 | 单实例 **409.6 万/s**（4096/ms） | 12 位序列 / 毫秒 | **无关** —— 雪花不区分会话 | Leaf 原文算式 + `SnowflakeIDGenImpl` |
| Redis 号段 + 进程内自增 | 无生成器上限；实际 = 该实例进程内自增速度 | 进程内计数器（`AtomicLong` / `SegmentBuffer` 读锁） | 每 1000 条一次 Redis（1/1000） | Leaf `SegmentIDGenImpl` |
| Redis 每消息 `INCR` | ≈ 单 Redis 实例 ops 上界，**7.2 万–18 万/s**（同一实例所有会话共享） | Redis 单实例单线程 | 单会话 = 单 key = 串行，但它和全站抢同一个线程 | redis.io benchmark |
| seqsvr | 单 AllocSvr 内存自增；官方"10⁷ QPS / 数百台 4 核" → 单机 **2–5 万 QPS 量级**（推算） | AllocSvr 台数 | 无关（per-uid 号段） | 微信原文数字推算 |
| Leaf-segment | 单实例 **近 5 万/s**（4C8G，TP999 1ms） | 服务端实例 | 按 biz_tag 隔离 | Leaf 原文 |
| uid-generator | 单实例 600 万/s（Cached）；**单 worker 8192/s 硬顶** | 13 位秒级序列 | 无关 | `README.md` |

**判断：单会话 QPS 不是选型判据。** 我们的最坏单会话是万人群 50 条/s（§2.1），比上表任何一个上限都低 3–5 个数量级。真正会打穿的不是"生成器算力"，而是下面两条：

1. **热路径网络往返**。每消息一次 Redis `INCR` 在数学上够用（50 条/s ≪ 7 万/s），但它直接违反 §9 硬约束 1「热路径不许有 Redis / RPC 往返」，并且把这 50 次/s 的 p99 从 ~0 抬到 Redis p99（官方 benchmark 99.76% ≤ 1 ms）。所以**必须用号段**，这不是优化选择，是基线自己的约束推出来的。
2. **发号点唯一性**。详见 §5 C-2：号段一旦分发到多个实例，"单会话 QPS"这个指标就不再是瓶颈，而"同一会话的号会不会乱序落库"变成致命问题。这是号段方案的**隐藏代价**，基线没有写。

### 3.2 故障切换后的空洞行为

| 方案 | ① 进程崩溃 | ② 存储层主从切换 | ③ 发号点迁移 | 会回退吗 | 空洞大小 |
|---|---|---|---|---|---|
| seqsvr | `cur_seq` 从持久化的 `max_seq` 恢复 → 跳 | StoreSvr 用多机 NRW，持久化后不丢 | 租约：旧机停 → 新机等 N 秒再服务 → 该段**短暂不可服务** | **不会**（持久化上界，机制上排除了回退） | ≤ step（生产 10000） |
| Leaf-segment | 丢弃内存号段 → 跳 | MySQL **半同步**；原文承认可能退化为异步、极端情况数据不一致 | 无迁移概念（无状态服务，号段靠 DB 原子分配） | **极端情况可能** | ≤ step（源码上限 1,000,000） |
| Redis 号段（基线） | 丢弃内存号段 → 跳 | **异步复制 → 已确认的 `INCRBY` 会丢 → 回退** | 同上 | **会** | 崩溃 ≤ 1000；切换 = 负向（重号） |
| uid-generator | 重启时 `DisposableWorkerIdAssigner` 弃用旧 workerId 重新分配 | DB 分配 workerId，DB 主从切换可能重复分配 → workerId 碰撞 | 无（workerId 是身份不是号段） | 时钟回拨：**直接抛异常**，不重号 | 无（雪花无空洞概念） |
| Snowflake（本地） | `lastTimestamp = -1` 重置 → **重启后的回拨不可检测** | 无存储层 | 无 | **可能重号** | 无 |

三条要展开讲：

**(a) 为什么 seqsvr 物理上不会回退。** 因为持久化的是"我最多发到哪"（`max_seq`），不是"我发到哪了"（`cur_seq`）。重启后从 `max_seq + 1` 开始，最坏只是浪费 `[cur_seq+1, max_seq]` 这一段。反观"持久化已用量"的方案（`seq:seg:{convId}` 存的是已发出的最大号），崩溃本身也不回退，但**存储层回退时它就一起回退了**。

**(b) Redis 的回退后果为什么是"丢消息"而不是"数据错乱"。** 链路是这样串起来的：

1. `INCRBY` 在 Redis 主上成功，返回 `max=5000`，此时代理进程已把它当成本地号段 `[4001, 5000]`；
2. Redis 主故障，Sentinel 提升一个落后的从库，该从库的 `seq:seg:{convId}` 还是 3000；
3. 新主上再 `INCRBY 1000` → 返回 4000，新号段 `[3001, 4000]`；
4. 号 3501 被再次发出 → 与之前已发出的 3501 重复；
5. 按 §6.2，`ACK` **早于落库**（基线第 418 行明写），客户端已经收到这条消息的 ACK；
6. 落库时 `PRIMARY KEY (conv_id, seq)` 冲突；按 §10「消费端按 `(convId, seq)` 天然幂等」→ 当作重复消息丢弃；
7. 消息消失，且没有任何告警。

这正好撞上 §15.1 判据 2：「宁可要一个会丢消息但能告警的组件，也不要一个会静默降级的组件」。Redis 在这个位置是静默的。

**(c) Leaf 为什么敢用 MySQL 存号段。** 因为它上的是**半同步复制**，并且原文直说了退化风险。也就是说 Leaf 对这个风险的处置是"承认 + 收窄窗口"，不是"消除"。我们既然 §8.2 已经因为"多副本强一致，不用再维护 MySQL 主从切换那一套"选了 OB，就没有理由把最怕回退的数据留在 Redis 上。

### 3.3 号段耗尽前能否续租

| 方案 | 触发时机 | 提前量 | 续租失败时 | 出处 |
|---|---|---|---|---|
| seqsvr | `max_seq` 被突破时**同步**写 StoreSvr 提升 | 无"预取"概念，持久化即续租 | Store 不可用 → 租约失效 → **停止服务**（不是随机发号） | 微信原文 5.1 + 容灾篇租约 |
| Leaf-segment | 已消耗 **10%** 即异步加载下一段（双 buffer） | 90% × step | 两段都空 → 返回 `-3` 异常（快速失败，不阻塞） | 源码 + 原文 |
| Redis 号段（基线） | **未定义** | **无** | **未定义** | §8.3 无此内容 |
| uid-generator | `paddingFactor` 阈值触发填充 + 定时填充 | 可配 | `rejectedTakeBufferHandler` 默认抛异常 | `README.md` |

基线这一格是空的，必须补。补什么，Leaf 的两条经验直接可用：

- **双 buffer + 消耗 10% 触发异步预取**：不做这件事，就是 Leaf 原文点名的失败模式 —— 「当号段使用完之后还是会 hang 在更新数据库的 I/O 上，TP999 数据会出现偶尔的尖刺」。
- **step 自适应**：`SEGMENT_DURATION = 15 min`，上次填满得快就 ×2，慢就 ÷2，上限 1,000,000。理由是我们的 step 写死 1000 时：万人群 50 条/s → **每 20 秒续租一次**；而 1 条/天 的死会话，1000 条号段要用 3 年。固定 step 让热会话频繁续租、冷会话永久占号段。

量化一下续租对 p99 的影响。L2 目标 10 万 msg/s、step=1000 → 100 次续租/s：

| 做法 | 续租耗时（估） | 每秒占用的线程时间 | 相对"1 核处理全部消息"（10万 × 10 µs = 1 s） |
|---|---|---|---|
| 同步阻塞取号 | 1 ms（Redis p99） | 0.1 s | **10%** |
| 同步阻塞取号 | 3 ms（OB 单行更新 p99，待压测） | 0.3 s | **30%** |
| 异步预取（双 buffer） | — | 0（在独立线程池） | **0** |

所以 §5 C-1 把号段换到 OB 之后，"同步取号"的成本从 10% 变成 30% —— 这**不是**可以忽略的差异，它就是"必须做双 buffer"的量化理由。两件事必须一起做，只做一件会更糟。

---

## 4. 基线 §8.3「Nacos workerId + 5ms 拒绝发号」的失效条件

基线原文（第 546、651–652 行）：

> `serverMsgId` 由业务进程本地生成（雪花），**不走 Redis、不走 RPC**，热路径零网络往返。`workerId` 由部署配置从 Nacos 下发（10 位，最多 1024 个实例），时钟回拨超过阈值就拒绝发号并告警，不靠等待硬扛。

> - `workerId` 由 Nacos 配置下发，部署时唯一分配；集群超过 1024 个实例再换 Leaf 之类的分段方案。
> - 时钟回拨超过阈值（比如 5 ms）直接拒绝发号并告警，不阻塞等待。

这套说法的成立前提是：「workerId 一定唯一」+「时钟回拨一定能被检测到」。两个前提都只在**运行中、且人工没出错**的情况下成立。具体失效条件：

### S1 跨重启的时钟回拨：检测机制直接失效（唯一性）

`snowflake` 系列的 `lastTimestamp` **只存在于进程内存**（Leaf `SnowflakeIDGenImpl.lastTimestamp = -1L`、uid-generator `DefaultUidGenerator.lastSecond = -1L`、原始 snowflake 同构）。进程重启后这个变量归零，**"当前时间 < 上次发号时间"这个判断永远为假**。

所以：进程停机期间 NTP 把时钟往回拨（运维手动改时间、闰秒 smearing 结束后的校正、宿主机重启后 RTC 落后），新进程启动后从"更早的时间"开始发号 → 与停机前发的号重复。基线只写了"运行中回拨超过阈值拒绝"，对这个最常见的场景零覆盖。

行业里唯一做了这件事的是 Leaf-snowflake：**把发号时间写进 ZooKeeper，启动时比对**（与 `leaf_forever/${self}` 记录时间比，首次启动则与全体运行节点的时间均值比），比不过就启动失败。我们没有 ZK（§15.2 明确不用），但同样的机制可以用 OB 做（见 §6）。

### S2 workerId 重复：唯一性靠人工承诺，没有任何校验（唯一性）

Nacos 是配置中心，官方文档对配置的定义是"配置内容是一个整体。Nacos 保存并分发 `content`，但不会理解配置文件内部某个业务字段的含义"（<https://nacos.io/docs/latest/manual/user/config/overview/>）。也就是说 Nacos 下发的只是一个静态数字，**它不知道有几个实例在跑，也不会做任何唯一性校验**。

具体会重复的三个场景：

1. **滚动发布双活**：旧 Pod（workerId=7）还没退出，新 Pod 已经从 Nacos 读到 workerId=7 开始接流量。这是滚动发布的**正常行为**，不是异常。
2. **模板化部署漏配**：K8s 里 workerId 若来自 StatefulSet 序号（这已经是最优解了），缩容再扩容时序号复用（Pod-3 删了，新 Pod 又是 Pod-3）→ 只要旧进程还没彻底退出就是重复。若 workerId 来自人名/环境名这类手填值，出错概率更高。
3. **同名配置被多环境共用**：`dataId` 一样、`group` 也一样的两个业务进程读到同一个值。

对比两个成熟实现的处置方式：Leaf 用 **ZK 持久顺序节点**（唯一性由存储的原子性保证），uid-generator 用 **DB 自增 `WORKER_NODE` 表**（同上）。都是"机制"，不是"约定"。基线用的是"约定"。

### S3 workerId 空间 1024：与 L1 节点数冲突（容量）

10 位 → 1024 个实例位。§2.1 的 L1 是 750 万 QPS 峰值、§15.4 说"需要 50–100 个网关节点"。按 10 万 msg/s per 业务进程算，750 万 QPS 需要 ~75 个 messaging 实例；加上网关、api、relation 三个部署单元各自要发号（至少 api/messaging 要发），再乘滚动发布时的双活，1024 不算宽裕。

基线自己的应对是"超过 1024 再换 Leaf 之类的分段方案"—— 但换方案意味着**位宽变化**（10 位不够就得动 12 位序列或 41 位时间），这是协议级的破坏性变更（`serverMsgId` 已经存进 `message.server_msg_id`、已经发给客户端、已经进了日志和审计）。**位宽必须一开始就参数化**，不能硬编码 41/10/12。

### S4 ≤5 ms 没有重试路径：NTP 常态微调也会变成发号失败（可用性）

基线写的是"超过阈值直接拒绝"，没说阈值以内怎么办。如果阈值以内也拒绝，那么**亚毫秒级的 NTP 微调**（Linux `ntpd`/`chronyd` 的小幅 slew 是常态）都会让发号失败。Leaf 的实际做法是：`offset <= 5` 时 `wait(offset << 1)` 后重试一次，仍然落后才返回异常；`offset > 5` 才直接异常。

也就是说 Leaf 的 5 ms 阈值是"等待预算"，不是"驳回阈值"。基线把它读成了驳回阈值，等于把可用性问题放大了一个数量级。而原文明确写过 NTP 会造成回拨：「在机器工作时 NTP 同步也会造成秒级别的回退」，且 2017 闰秒确实发生过部分机器回拨。

同时要承认基线那句"不阻塞等待……不要卡住整个 EventLoop"的直觉是对的 —— Leaf 的 `wait()` 是在 `synchronized get()` 里，会阻塞该实例上**所有**发号线程最长 10 ms。正确的折中是：**≤ 阈值时快速失败 + 让上层重试（一次重试的代价远小于 10 ms 阻塞）**，而不是 sleep。

### S5 同毫秒内不严格单调：如果有人拿 `serverMsgId` 当游标就会错

Leaf 的实现里新毫秒的起点是 `RANDOM.nextInt(100)`，所以同一毫秒内的 `serverMsgId` 序列部分是无序的。基线 §7.1 已经正确声明"`serverMsgId` 不能用来排序"，所以**不构成冲突**，但需要落成一条硬约束（比如在 proto 注释里写死"禁止用作游标/分页/水位"），否则第 5 个用这个字段的人就会踩。

### S6 Nacos 是启动期强依赖（可用性）

§8.3 强调 `serverMsgId` "零网络调用"，指热路径；但 workerId 来自 Nacos 意味着**冷启动依赖 Nacos**。Nacos 不可用时新实例起不来 → 扩容和故障替换同时失效。这正是微信那两条架构原则里"避免对外部模块的强依赖"要防的东西。Leaf 对 ZK 的处理是"本机文件缓存 workerId"，把它降级为弱依赖。

### S7 `twepoch` 未定义：41 位时间位的实际寿命不确定（容量）

基线只写"41 位时间"，没写基准点。这直接决定什么时候耗尽：

| twepoch | 41 位毫秒可支撑到 |
|---|---|
| 1970-01-01（Unix epoch，不指定时的默认实现） | **2039-09** |
| 2010-11-04（Leaf 用的 `1288834974657`） | 2080-06 |
| 项目上线时间（2026，推荐） | 2096 左右 |

`2^41 ms = 69.68 年`。反面教材就在 uid-generator：默认 28 位秒 + 2016-05-20 epoch，源码注释自陈 **"Supports about 8.7 years until to 2024-11-20 21:24:16"** —— 这个默认配置已经过期了。位分配与 epoch 必须和协议版本一起写死，不能留给实现者选默认值。

---

## 5. 与基线的冲突（四段格式）

### C-1 号段权威存储：Redis → OceanBase

**① 原方案**：§8.3（第 649 行）「`sequence` | Redis 号段 `INCRBY ... 1000` + 进程内自增」；§8.1（第 558 行）定义 key `seq:seg:{convId}`，`int64`，无过期；§15.2 把"号段要原子自增"列为选 Redis 的理由；§10（第 805 行）「序号回退：号段只增不减；Redis Lua 校验单调」。

**② 新结论**：号段的权威存储改为 OceanBase（新建 `conv_seq_alloc(conv_id PK, max_seq, step)`，按 `conv_id` 分区，取号语句照 Leaf-segment：`UPDATE ... SET max_seq = max_seq + step WHERE conv_id = ?` + 同事务 `SELECT`）。Redis 不再承载号段，§8.1 的 `seq:seg:{convId}` 这一行删除。

**③ 理由**：

- Redis 官方文档明确：默认异步复制；`WAIT` "does not turn a set of Redis instances into a CP system with strong consistency: acknowledged writes can still be lost during a failover"；`min-replicas-to-write` 也只是"always a window for data loss"（<https://redis.io/docs/latest/operate/oss_and_stack/management/replication/>）。**号段回退是可证明会发生的，不是理论风险**。
- 回退后果沿 §3.2(b) 的七步链落到"ACK 已发、落库被幂等吞掉、无告警" = 静默丢消息，违反 §15.1 判据 2。
- §10 的"Redis Lua 校验单调"**在这个失效面前无效**：主从切换后，校验所依据的"已发出的最大值"本身也一起回退了。Lua 只能在同一个 Redis 实例的生存期内保证单调。
- OB 的多副本是 Paxos 强一致："同一分区的多个副本使用 Paxos 一致性协议保证副本的强一致"（<https://www.oceanbase.com/docs/enterprise-oceanbase-database-cn-10000000000943941>）；"少数派故障时 RPO = 0，RTO < 8s"（<https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000003976557>）。已提交的号段分配**物理上不会丢**。
- 频率完全吃得下：§9 自己写"1/1000 次走 Redis"—— L2 下 10 万 msg/s ⇒ **100 次/s**，L1 下 750 万 ⇒ **7500 次/s**。这是噪声级别的写入。
- 少一个依赖：§8.2 选 OB 的理由第 3 条就是"多副本强一致，不用再维护 MySQL 主从切换那一套"。把最怕回退的数据放在唯一会回退的组件上，是内部不自洽。

**④ 性能/成本差异**：

- 热路径单消息成本：**不变**。仍然 1/1000 次远程调用，且改为异步预取后不进请求路径（见 C-3）。
- 单次取号延迟：Redis 侧取 O(1) 单 key 写命令的官方 benchmark 数字作代理（`SET`，随机 key、50 连接：99.76% ≤ 1 ms，<https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/benchmarks/>；**非 `INCR` 实测**）；OB 侧单行 `UPDATE` 走 leader，同城 p99 估 1–3 ms（**本票未取得公司内 OB 压测基线，需实测**）。
- 每秒线程占用：同步取号 100 次/s × (1 ms vs 3 ms) = **0.1 s vs 0.3 s**，相对"10 万 msg/s × 10 µs = 1 s"的服务时间 = **10% vs 30%**。异步预取后两者都是 0 → 换 OB 的代价可以完全靠双 buffer 吃掉。
- 新增成本：一张表（行数 = 会话数，100 万会话 ≈ 100 万行，按 `conv_id` 分区）+ OB 上 100–7500 写/s。
- 减少：一条静默失败路径、一个 §8.1 的 Redis key、§15.4 里"号段按 convId 分片"的 Redis 扩容负担。

### C-2 「号段单点递增」≠「落库顺序单调」：增量拉取会永久漏消息

**① 原方案**：§8.3（第 654 行）「单调性由"每个 convId 的号段单点递增"保证；跨会话无全局顺序需求」；§6.4 客户端按 `sequence` 增量拉取；§6.6 `GET /messages?convId&fromSeq&limit`，「`sequence` 即 offset」。

**② 新结论**：号段单点递增只保证"发号递增"，不保证"落库递增"。必须显式约束**同一 `convId` 在同一时刻只能由一个实例发号**（按 `convId` 哈希把取号 + 投递绑定到固定实例），否则 `fromSeq` 增量拉取会永久漏消息。

**③ 理由**：

- `INCRBY 1000` 天然把同一 conv 的号分成多段，可以被多个实例各持一段。实例 A 持 `[1001, 2000]`、实例 B 持 `[2001, 3000]`，两者并发发号并各自投 MQ。
- MQ 只对**同一 sharding key** 保序。§6.3 只在广播模式显式指定 `sharding key = convId`（第 437 行），§6.2 的单聊链路 `MessageAccepted` 未指定。
- 于是落库顺序可能是 `2001, 1001, 1002, 2002`。客户端已拉到 2001，`fromSeq=2001` 之后**永远取不到 1001/1002** —— 静默丢消息，比重复更糟（重复能被客户端去重，漏取不能）。
- 这不是 Redis/OB 的选择带来的，是号段粒度本身带来的。**换存储解决不了它。**
- 这也是微信 seqsvr 那条硬约束的来源：「任意时刻任意 uid 段有且仅有一台 AllocSvr 提供服务」。他们的容灾 1.0→2.0 花了五年就在处理"这条约束带来的迁移问题"。我们如果不想引入路由表 + 租约 + 仲裁，就只有"按 convId 固定实例"这一条便宜路。

**④ 性能/成本差异**：

- 方案 (a) 按 `convId` 哈希绑定实例：**零额外成本**。与 §15.3 已有的"分区数 ≥ 节点数 × 4"一致；热点代价可接受 —— 最坏的单会话是万人群 50 条/s（§2.1），落在单实例上是噪声。
- 方案 (b) 每消息一次 `INCR`（号段长度 = 1）：消除多段并存，但把 10 万次/s 的 Redis 往返放进热路径，直接违反 §9 硬约束 1，且 p99 从 ~0 抬到 ≥1 ms。
- 方案 (c) 落库前做 `seq` 水位 CAS（`UPDATE conversation SET last_seq = ? WHERE last_seq = ? AND ? = last_seq + 1`）：**不可行** —— 每消息一次 DB 写，10 万/s 的写放大，且乱序到达时只能丢弃等待，等于自建重排缓冲。
- 扩缩容代价（(a) 的代价）：某 conv 的归属变更时，旧实例作废未用完的号段、新实例取新段 → 产生 ≤ step 的空洞（不回退，符合 §8.3 的语义）。迁移期间该 conv 短暂拒绝（复用 §6.9 的 `SERVER_DRAIN` 语义），由客户端重试兜底 —— 与 seqsvr 租约切换的"短暂不可服务"同构，成本同量级。

### C-3 号段续租策略缺失，且 step 不该固定 1000

**① 原方案**：§8.3 只写「Redis 号段 `INCRBY ... 1000` + 进程内自增」，**未定义何时取下一个号段、未定义预取失败时的行为**；§8.1 把 step 写死为 1000。

**② 新结论**：写死三件事 —— (i) 双 buffer；(ii) 当前段已消耗 **10%** 时异步预取下一段；(iii) step 自适应（上次填满 < 15 min → ×2，15–30 min 不变，> 30 min → ÷2，下限 1000、上限 1,000,000）。两段都不可用时**快速失败 + 告警**，不阻塞。

**③ 理由**：

- Leaf 原文点名的失败模式：「TP999 数据波动大，当号段使用完之后还是会 hang 在更新数据库的 I/O 上，TP999 数据会出现偶尔的尖刺」。
- Leaf 源码的阈值与行为可直接照抄：`segment.getIdle() < 0.9 * getStep()` 触发；两段都空返回 `EXCEPTION_ID_TWO_SEGMENTS_ARE_NULL = -3`（异常，不阻塞）；`MAX_STEP = 1000000`；`SEGMENT_DURATION = 15 min`。
- 固定 step=1000 的实际后果：万人群 50 条/s → **每 20 秒续租**；1 条/天的死会话 → 1000 条号段用 3 年。热会话频繁续租、冷会话占号段，正好反了。
- Leaf 给的量级参考：「segment 长度设置为服务高峰期发号 QPS 的 600 倍（10 分钟），这样即使 DB 宕机，Leaf 仍能持续发号 10–20 分钟不受影响」。按此，万人群应该拿 50 × 600 = 30000，而不是 1000。

**④ 性能/成本差异**：

- 不做双 buffer（同步取号）：L2 下 100 次/s × 1 ms（Redis）= **0.1 线程当量/s**，相对 1 线程当量的消息处理 = **10% 开销**；换成 OB（C-1）后 100 × 3 ms = **30%**。
- 做双 buffer：**0**。
- 记忆成本：每 conv 两个号段（`Segment` × 2）常驻内存。100 万活跃会话 × 2 × 16 B ≈ 32 MB，可忽略。
- step 自适应的收益：DB/OB 宕机时的可用窗口从"20 秒"（step=1000，50 条/s）拉到"10 分钟"量级（step=30000）。
- 代价：自适应需要记录"上次填充时间戳"（Leaf 用 `updateTimestamp`），每段两个 `long`，同上可忽略；副作用是 step 变大后单次崩溃产生的空洞上限也变大（≤ step），但空洞本来就是允许的。

### C-4 workerId「部署唯一」与「回拨拒绝」覆盖不足，`twepoch` 未定义

**① 原方案**：§8.3（第 651–652 行）「`workerId` 由 Nacos 配置下发，部署时唯一分配」「时钟回拨超过阈值（比如 5 ms）直接拒绝发号并告警」；§7.1（第 546 行）同义；§10（第 808 行）「`serverMsgId` 碰撞：雪花 `workerId` 部署唯一 + 时钟回拨拒绝发号」。

**② 新结论**：把"唯一性"从人工约定改成存储机制，并补三件事：(i) workerId 启动时向 OB `worker_node(worker_id)` 唯一索引注册，冲突即 fail-fast；(ii) 启动期读回该 worker 上次发出的最大时间戳做跨重启校验，不通过拒绝启动并告警；(iii) `twepoch` 与位宽一起写死成协议常量（推荐 `twepoch` = 上线时间，位宽参数化）。另外把 ≤5 ms 的处理改成"快速失败让上层重试"，不做 sleep。

**③ 理由**：见 §4 的 S1–S7。三个已证实的事实支撑：

- `lastTimestamp` 是进程内变量（Leaf `SnowflakeIDGenImpl.lastTimestamp = -1L`、uid-generator `DefaultUidGenerator.lastSecond = -1L` 源码可查）→ 跨重启回拨天然漏检。
- Nacos 只分发配置内容，不做唯一性校验（<https://nacos.io/docs/latest/manual/user/config/overview/>）→ "部署时唯一分配"是流程承诺。
- 成熟实现的处置方式无一例外是机制：Leaf 用 ZK 顺序节点 + ZK 记录时间做启动校验；uid-generator 用 DB 自增 `WORKER_NODE` 表。**没有一个靠人工配置。**

**④ 性能/成本差异**：

- 热路径：**0**（workerId 只在启动时取一次，`serverMsgId` 仍然纯本地生成）。
- 冷启动：+1 次 OB 读写（~1–3 ms），相对进程启动时间可忽略。
- 跨重启校验的落盘成本，两种取法：仅在优雅退出时写（0 常态成本，但 crash 漏网）或定期写（每实例 2 次/s；50 实例 = 100 次/s，OB 噪声级）。推荐后者，因为本票 S1 要防的正是 crash。
- 位宽参数化的收益：1024 → 4096 个实例位只是一次配置，不改协议；不参数化则要改 `server_msg_id` 的语义，属于破坏性变更。
- 换成"每实例 sleep 重试"的替代（照抄 Leaf）：单实例 `synchronized get()` 被 `wait()` 阻塞最长 10 ms，会顶住该实例所有并发的上行。**这是基线那句"不要卡住整个 EventLoop"要避免的，所以不照抄。**

### C-5 唯一索引不是"兜底"，是报警器

**① 原方案**：§8.2（第 630–633 行）「建全局唯一索引 → …… 碰撞也由数据库兜底，不靠生成器自证」。

**② 新结论**：`uk_server_msg` / `uk_msg` 只能"发现"碰撞，不能"兜底"—— 发现之后没有恢复路径就是丢消息。消费端必须区分两种唯一键冲突并分别处置：(a) 同 `clientMsgId` 重投 → 幂等，丢弃，正常；(b) 不同 `clientMsgId` 但 `server_msg_id`（或 `(conv_id, seq)`）相同 → **生成器故障**，不能丢弃，必须换 id 重写 + 告警。

**③ 理由**：

- 基线 §6.2 明写「ACK 早于落库」。碰撞发生在落库阶段时，客户端已经收到 ACK，不会重发。
- §10 把消费端幂等定义为「按 `(convId, seq)` 天然幂等，重复写无副作用」—— 这条幂等规则的实现方式是"冲突就丢弃"，于是 (b) 类冲突被 (a) 的规则吃掉，变成静默丢消息。
- 这与 §15.1 判据 2 冲突：唯一索引让故障**可见性**变差了（DB 帮它吞了），而不是更好。

**④ 性能/成本差异**：

- 区分 (a)/(b) 的实现成本：冲突时读一次原行比对 `client_msg_id`（冲突是异常路径，不在热路径）。**热路径成本 = 0**。
- 若连这次读都不想付：代价是每次生成器故障都变成一批无告警的消息丢失。参考量级：一次 workerId 重复（S2）在滚动发布窗口内影响的可能是该实例全部上行（10 万/s × 窗口秒数）。
- 与之对比：不在 DB 上建 `uk_server_msg`（§8.2 已列为"压测不达标就退回"的项）会省掉"每条消息一次跨分区全局索引写"的放大，但失去唯一的碰撞检测点 → 建议**保留**该索引，同时按上述方式分类处置。

---

## 6. 结论：我们的场景该用哪种组合

### 6.1 两个号码的正确解法

**`serverMsgId`：继续用本地雪花，但把"靠人"改成"靠机制"。**

| 决定 | 理由（对应本票章节） |
|---|---|
| 保留本地生成，不走 Redis / RPC | §3.1 已证：单实例 409.6 万/s，我们最坏 50 条/s；热路径零往返是 §9 硬约束。没有方案能在"零往返"上赢过它 |
| `twepoch` = 上线时间，位宽参数化（41/10/12 起步） | S7：不指定时默认实现会用 1970，2039-09 耗尽；uid-generator 的默认配置已经过期（2024-11-20）是活教材 |
| workerId 由 OB `worker_node` 唯一索引兜底 | S2/S3：Leaf 用 ZK、uid-generator 用 DB，都在用机制；Nacos 只能下发数字，不能校验 |
| 启动期做跨重启时间校验 | S1：`lastTimestamp` 进程内变量，重启即失效，这是唯一会**静默重号**的口子 |
| ≤阈值时快速失败 + 上层重试，不 sleep | S4：Leaf 的 `wait()` 阻塞全部发号线程最长 10 ms，与基线"不卡 EventLoop"的立场相反 |
| `serverMsgId` 禁止用作游标/分页/水位 | S5：Leaf 同毫秒起点是 `RANDOM.nextInt(100)`，同毫秒内无序 |

**不选**：Leaf-snowflake（我们要 ZK，而 §15.2 明确"不用 ZK"）、uid-generator（13 位秒级序列 = 单 worker 8192/s 硬顶，10 万 msg/s 单机要 13 个 workerId；且序列耗尽时是忙等自旋）。

**`sequence`：继续用号段，但存储换 OB、续租写死、发号点绑实例。**

| 决定 | 理由（对应本票章节） |
|---|---|
| 号段权威放 OB `conv_seq_alloc`（Leaf-segment 形态） | C-1：Redis 异步复制会让已确认的 `INCRBY` 回退；OB 是 Paxos 多副本，RPO=0。频率只有 100–7500 次/s |
| 双 buffer + 消耗 10% 异步预取 + step 自适应（1000 起，上限 100 万） | C-3：否则就是 Leaf 原文点名的 TP999 尖刺；自适应解决"热会话每 20 秒续租、冷会话占号段 3 年" |
| 按 `convId` 哈希把取号 + 投递绑定到固定实例 | C-2：不绑就会出现同一 conv 多段并存 → 乱序落库 → `fromSeq` 增量拉取永久漏消息。这是号段的**隐藏代价**，不是优化 |
| 两段号段都不可用 → 快速失败 + 告警 | C-3：照 Leaf 的 `-3` 快速失败，与 §15.1 判据 2 一致 |
| 删掉 §8.1 的 `seq:seg:{convId}` | C-1 |

**不选**：每消息一次 `INCR`（违反 §9 硬约束 1）、微信 seqsvr 式 AllocSvr 集群（我们的分片键是 convId 且单 conv 量级只有 50 条/s，用不上"数百台 4 核撑 10⁷ QPS"那套；引入它就得同时引入路由表 + 租约 + 仲裁服务）。

### 6.2 为什么这个组合是对的（三条判据对齐）

1. **§15.1 判据 1（吃得下指标）**：`serverMsgId` 零网络、409.6 万/s per 实例；`sequence` 1/1000 次远程调用，L1 下 7500 次/s 的 OB 写是噪声。两条都不需要"等 L1 再换"。
2. **§15.1 判据 2（失败模式可见）**：改动后三个失效点全部从"静默"变"可见"—— Redis 回退（消失）、落库乱序（消失）、workerId 重复/时钟回拨（撞唯一索引或校验失败 → 启动即报）。
3. **§15.1 判据 3（运维能力）**：只用了已在栈内的 OB 与 Nacos，没有引入 ZK / 新中间件。§15.2 的"不用 ZK"保持不变。

### 6.3 落地时必须一起做的四件事（缺一件就不是这个方案）

1. C-1 + C-3 **必须同时做**：把号段换到 OB 而不做双 buffer，同步取号开销从 10% 涨到 30% 线程占用（§3.3 的表）。
2. C-2 的实例绑定必须和 §6.3 的 `sharding key = convId` **统一成一条规范**，不能只在广播模式生效。
3. C-4 的 `worker_node` 表要落成 DDL 的一部分（含 `last_issued_ts_ms` 与 `worker_id` 唯一索引），不能只在方案里提一句。
4. C-5 的冲突分类处置要写进消费端的幂等规范，否则唯一索引依然是吞消息的黑洞。

---

## 7. 未取得原文 / 待实测

| 项 | 状态 | 处置 |
|---|---|---|
| InfoQ 原文《微信序列号生成器架构设计及演变》 | 本机访问返回 **HTTP 451**，未就原文域名取得正文 | 正文取自阿里云开发者社区与 harries.blog 的全文转贴（同一作者、同一正文、含容灾篇），已按转载来源标注；结论只用两处转贴一致的数字 |
| Twitter Snowflake `IdWorker.scala`（`snowflake-2010` tag） | 仓库已 retired，tag 的 git tree 返回空，**未取得源码** | 位宽 41/10/12 与容量 409.6 万/s 引自美团 Leaf 原文的算式；回拨行为以 Leaf / uid-generator 两个公认忠实实现的源码为准 |
| Redis `INCR` 本机实测吞吐 | 本机未安装 `redis-server`/`redis-benchmark`，**未实测** | 引用官方 benchmark 文档的 `SET` 数字作为同量级（O(1) 单 key 写）代理，已明确标注为代理而非 INCR 实测 |
| OceanBase 单行 `UPDATE` 同城 p99 | **未取得公司内压测基线** | §3.3 / C-1 的 1–3 ms 是估值，标注待压测；该数字只影响"同步取号开销 10% vs 30%"的量级对比，不影响结论（双 buffer 后两者都是 0） |
| OB 是否支持 `UPDATE ... RETURNING` 或需 `LAST_INSERT_ID(expr)` trick | 未核实 | Leaf 用的是 `UPDATE` + `SELECT` 同事务，照抄即可，不依赖 OB 方言特性 |
| 微信 `AllocSvr` 的租约 N 秒具体值 | 原文只写"N 秒"，未给数字 | 本票未引用具体值 |

---

## 8. 参考来源

**一手实现 / 源码**

1. 美团 Leaf 源码（`SegmentIDGenImpl.java`、`SnowflakeIDGenImpl.java`）—— <https://github.com/Meituan-Dianping/Leaf>
2. 百度 uid-generator（`README.md`、`DefaultUidGenerator.java`）—— <https://github.com/baidu/uid-generator>
3. Twitter Snowflake（已 retired）—— <https://github.com/twitter-archive/snowflake>

**厂商官方文档**

4. 美团技术团队《Leaf——美团点评分布式ID生成系统》—— <https://tech.meituan.com/2017/04/21/mt-leaf.html>
5. 微信《微信序列号生成器架构设计及演变》（曾钦松），原文 —— <https://www.infoq.cn/article/wechat-serial-number-generator-architecture/>
   可读全文转贴 —— <https://developer.aliyun.com/article/650782>、<https://www.harries.blog/archives/360283.html>
6. Redis 持久化 —— <https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/>
7. Redis 复制 —— <https://redis.io/docs/latest/operate/oss_and_stack/management/replication/>
8. Redis `INCR` —— <https://redis.io/docs/latest/commands/incr/>
9. Redis benchmark —— <https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/benchmarks/>
10. OceanBase 高可用概述（Paxos 多副本，RPO=0 / RTO<8s）—— <https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000003976557>
11. OceanBase 数据分区和分区副本概述（同分区多副本 Paxos 强一致）—— <https://www.oceanbase.com/docs/enterprise-oceanbase-database-cn-10000000000943941>
12. Nacos 配置管理概览（配置内容是不透明整体）—— <https://nacos.io/docs/latest/manual/user/config/overview/>

**基线文档（本仓库）**

13. `docs/architecture/im-v2-architecture.md`（`rewrite/v2`）：§6.2（第 418 行 ACK 早于落库）、§6.3（第 437 行 sharding key）、§6.4/§6.6（`fromSeq` 增量拉取）、§6.5（第 465 行未读 = `last_sequence − read_sequence`）、§7.1（第 546 行）、§8.1（第 558 行）、§8.2（第 594–596 行 DDL、第 630–633 行 `uk_server_msg` 取舍）、§8.3（第 648–654 行）、§9（性能预算）、§10（第 805、808 行）、§15.1–§15.4

# 缓存与在线状态调研：Redis 结构选型、大 key 与集群形态（票 #109）

> 调研对象：基线 `docs/architecture/im-v2-architecture.md` §8.1 的六个 key（`route:u:{uid}` / `route:n:{node}` / `seq:seg:{convId}` / `idem:{uid}:{clientMsgId}` / `group:members:{gid}` / `rel:{uid}`）。
> 结论全部有来源：**官方文档 / 源码 / 本仓实测**优先，厂商规范（阿里云、华为云）与厂商技术分享（网易云信、企业微信、钉钉）作为二手佐证并明确标注。
> 实测环境与命令见 §1、附录 A，可直接复现。

---

## 0. 结论摘要

| # | 结论 | 依据 |
|---|---|---|
| 1 | 六key里**只有 `route:u` 天生是对的**（小 hash、每设备一 field、可原子增删）；其余五个要么结构错、要么规模算错 | §2 |
| 2 | **三个大 key**：`route:n:{node}`（100 万连接 = 64 MB 单键）、`group:members:{gid}`（万人群 90 KB blob）、`rel:{uid}`（5000 好友 47 KB blob） | §3 |
| 3 | `HGETALL` 1M field = **104 ms 服务端阻塞**；`DEL` 同键 = **94 ms**，`UNLINK` = **7 µs**。运维/GC 路径一律 SCAN 家族 + UNLINK | §4 |
| 4 | `idem` 的**总量**（不是结构）是最大内存项：基线 10 min TTL 在 L1 峰值下是 **504 GB / 45 亿 key**，且超过单实例 `2^32` key 上限。TTL 降到 90 s → 76 GB | §2.4、§9-C4 |
| 5 | `group:members` 用 **分片 intset** 替代 pb blob：内存 105 KB→84 KB，读从 O(N) 变 O(1)（3 µs），无大 key | §3.2 |
| 6 | Redis **重启后路由表不会自己回来**（长连接不断、客户端不重连），基线没写这一步 → 必须有 gateway 重推 + 投递降级 | §6.4、§9-C7 |
| 7 | **Cluster 而不是 Sentinel**：L2 峰值已需 ~11 万 ops/s（单实例余量 <2 倍），L1 需 ~900 万 ops/s（36–90 分片）。且按 key family 拆 3 个独立集群，因为持久化与淘汰策略互斥 | §8 |
| 8 | 持久化**按 key 分级**：路由/关系/成员不落盘；`idem` 不落盘；只有 `seq` 需要 AOF + 可重建兜底 | §7 |
| 9 | 与基线冲突 **9 条**，全部按四段格式写在 §9 | §9 |

---

## 1. 口径与实测环境

**环境**：Redis 7.4.12（`redis:7.4` 官方镜像，docker，aarch64 Linux 6.8），单实例、无持久化、无副本；宿主机 Apple M5 10 核 16 GB。

**取数方法**（避免客户端噪声）：

- 单键内存：`MEMORY USAGE` 与「1M 条写入前后 `used_memory` 差值 ÷ 条数」双重取数；单键分析用前者，容量投影用后者（把键名也算进去）。两者不总是相等，差异来自哈希表扩容的冗余桶（实测 10k 成员 set：`MEMORY USAGE` 677 KB vs 差值 490 KB），正文两数并列，不取平均；
- 单条 CPU：`CONFIG RESETSTAT` → 执行 → `INFO commandstats` 的 `usec_per_call`，是**服务端执行时间**，不受客户端往返影响；
- 阻塞时长 = `usec_per_call`；因为 Redis 服务端「一个时刻只服务一个请求」，这个数就是**其他所有客户端被卡住的时间**（[官方 latency 文档](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)）。

**版本默认值**（取自 [redis 7.4 官方 redis.conf](https://raw.githubusercontent.com/redis/redis/7.4/redis.conf)，容器内 `CONFIG GET` 已核对）：

| 配置 | 值 | 对本次结论的影响 |
|---|---|---|
| `hash-max-listpack-entries` / `-value` | 512 / 64 | `route:u`（2 field）走 listpack；≥513 field 的 hash 转 hashtable（64 B/field） |
| `set-max-intset-entries` | 512 | 成员集 ≤512 且全整数 → intset（8 B/成员）；否则 hashtable（49–52 B/成员） |
| `lazyfree-lazy-expire` | no | 大 key 到点过期由主线程同步释放 → 会阻塞（§4.3） |
| `appendonly` / `appendfsync` | no / everysec | 基线未指定，按默认无持久化评估，§7 给出分级建议 |

**口径边界**：绝对 ops/s 受硬件与网络影响，只能同机比较；**字节量、复杂度、阻塞时长是可迁移的**。所有 L1/L2 规模换算基于基线 §2.1 的指标（L2：100 万连接 / 10 万 msg/s；L1：5 亿 DAU / 750 万 QPS / 万人群），会话数、在线率等中间假设单独列出。

---

## 2. 六个 key 的逐条评估

### 2.1 总表

| Key（§8.1） | 基线结构 | 建议结构 | 单次操作复杂度 | 实测单条成本 | 最坏情况 | 处置 |
|---|---|---|---|---|---|---|
| `route:u:{uid}` | `pb{deviceId→nodeId}` 整块 blob | **hash** `{deviceId → "nodeId#epoch"}` | HSET/HDEL/HGET **O(1)**；HGETALL O(设备数≤3) | hash 2 field、23 B 键名：**104 B/键**（listpack，`MEMORY USAGE` 96 B） | 多端并发登录时 blob 读改写丢更新 | 改 hash；值带节点 epoch（§6） |
| `route:n:{node}` | `hash{uid→deviceBit}` | **计数器 + 可选分块反向索引** | HSET O(1)，但 HGETALL **O(N)**、HSCAN 全量 O(N) | 1M field：**64.4 MB 单键 / 64 B·field**；HGETALL = **104 ms 阻塞**；全量 HSCAN（COUNT 1000，1000 次）= **395 ms 服务端 CPU** | 单键 64 MB → 大 key、迁移与删除都阻塞 | 拆分：`route:n:cnt:{node}` 计数器 + `route:n:{node}:{chunk}`（64 块） |
| `seq:seg:{convId}` | string，`INCRBY 1000` | **hash 分桶** `seq:seg:{convId%1024}` + `HINCRBY`；可重建 | INCRBY / HINCRBY **O(1)** | 独立 string：**80 B/会话**；分桶 hash：**47 B/会话**（−41%），HINCRBY 30 µs | 会话数 × 永不过期 → L1 **296 GB 常驻**；Redis 丢数据则序号回退 | 分桶 + TTL + miss 时从 OB `max(seq)`+margin 重建（§7.3） |
| `idem:{uid}:{clientMsgId}` | string + SETNX，TTL 10 min | **`SET key val NX EX 90`**（单命令原子，带 TTL 抖动） | SET NX **O(1)** | **112 B/键**（含 expire 表开销），SET NX EX **6 µs** | 活键数 = QPS×TTL：L1 峰值 **45 亿键 / 504 GB**，超单实例 2^32 上限 | TTL 90 s（+±15% 抖动）→ **76 GB**；可选 hash 分桶 + HEXPIRE（92 B，但 2 次 RTT） |
| `group:members:{gid}` | `pb(memberList)` 单 blob | **分片 intset**：`group:m:{gid}:{crc16(uid)%N}`，SADD/SREM/SISMEMBER | blob：GET **O(N)**；intset：SISMEMBER **O(1)** | 万人群 blob(90 KB)：**105 KB**、GET 29 µs；分片 intset(20×500)：**84 KB**、SISMEMBER 2–4 µs；**单一大 set：490–677 KB**、SMEMBERS **285 µs 阻塞** | blob 每次读整块；成员变更重写整块；单 set 的 SMEMBERS 阻塞 | 分片 intset（整数 uid）；非整数 uid 用分片 hash |
| `rel:{uid}` | `pb{friends, blocks, groups}` 单 blob | **按关系类型拆分 + 好友集分片 intset** | blob：GET/HMGET **O(N)**；拆后 SISMEMBER **O(1)**、HEXISTS O(1) | 5000 好友的 blob：**51.4 KB/用户**、HMGET 单 field **23 µs**；单 set 5000：258 KB、SISMEMBER 2 µs；分片 intset 10×500：~42–54 KB | 重尾用户（5000 好友）每次发消息复制 47 KB；与 `group:members` 双写 groups 列表 | 拆 `rel:fr` / `rel:blk` / 不存 groups（成员判定归群） |

### 2.2 `route:u:{uid}`：结构错在 blob，不在 hash

基线值写的是 `pb{deviceId → nodeId}` —— 即整块 protobuf。问题不在大小（2 设备 ≈ 100 B），在**写入语义**：两个设备同时登录时，服务端逻辑是「GET → 反序列化 → 加一条 → SET」，两次并发写会丢一条设备路由（典型 lost update），要靠 CAS/Lua 兜，而 hash 的 `HSET key device node` 天生按 field 原子。

- 原子性来源：[HSET](https://redis.io/docs/latest/commands/hset/) 单命令 O(1)；多 key 事务才需要 hash tag，这里不需要（[cluster spec: Multi-keys operations](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)）。
- 内存实测：2 field、23 B 键名的 hash = 96–104 B；官方 FAQ 给的参照「100 万小 string ≈ 85 MB、100 万个 5-field hash ≈ 160 MB」同量级（[Redis FAQ](https://redis.io/docs/latest/develop/get-started/faq/)）。
- 规模：L2 100 万连接 = **104 MB**；L1 1.5 亿在线 = **15.6 GB**（若按 5 亿 DAU 全在线则 52 GB）。这一项**不是**瓶颈。
- 值里放 `nodeId#epoch`（而不是裸 nodeId），是 §6 清理方案的前提，零额外成本（字段长度 +6 B）。

### 2.3 `route:n:{node}`：唯一一个「写多、读少、却按大 key 存」的 key

它是 `route:u` 的反向索引：一台网关的全部连接 uid → deviceBit。实测：

| 规模 | 单键大小 | 单 field | 编码 |
|---|---|---|---|
| 10 万 field | 6.4 MB | 64 B | hashtable |
| **100 万 field** | **64.4 MB** | 64 B | hashtable |
| 1000 万 field | ~644 MB | 64 B | hashtable |

三个后果，都可量化：

1. **读即阻塞**：`HGETALL` 1M field = **104,023 µs**；`HGETALL` 是 O(N)（[HGETALL 文档](https://redis.io/docs/latest/commands/hgetall/)），且 Redis 单线程，这 104 ms 会卡住同一实例上的所有连接（[latency 文档](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)）。
2. **删除即阻塞**：`DEL` 同一键 = **93,941 µs**；`UNLINK`（异步释放）= **7 µs**，差 4 个数量级（[UNLINK 文档](https://redis.io/docs/latest/commands/unlink/)）。`lazyfree-lazy-expire` 默认是 `no`，所以大 key 到点过期走的是同步 DEL —— 阿里云规范里专门提醒过这个坑（[阿里云 Redis 键值规范](https://developer.aliyun.com/article/1009125)）。
3. **迁移即阻塞**：slot 迁移用 `MIGRATE` 逐键序列化（[cluster spec: Live reconfiguration](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)），64 MB 单键在源和目标两端各阻塞几十至上百 ms。

**更关键的一点：基线里这个 key 没有读方。** §6.1 的连接调度打分是 ip-conf 本地计算（「ip-conf 只保留上一秒分值」），§8.1 的 `route:n` 只在「连接建立/断开」时被写。一个 64 MB、只写不读、还会阻塞的 key，应当退化为：

- `route:n:cnt:{node}` —— 一个 `HINCRBY` / `INCRBY` 计数器，供调度与告警读，O(1)，8 B；
- （只在需要主动 GC 时）`route:n:{node}:{chunk}`（chunk = `crc16(uid)%64`），单块 ~1 MB / 1.5 万 field，页面迁移与删除都不阻塞。

### 2.4 `seq:seg:{convId}`：复杂度没问题，规模与持久性有问题

- 复杂度：`INCRBY` O(1)，基线「每次 1000」意味着 999/1000 次不进 Redis（§9 预算「取号段 ~0」），**CPU 侧完全正确**。
- 问题一：**key 数量**。一个会话一个 key、永不过期。按 L1 的 5 亿 DAU、人均 15 会话估 → ~37 亿会话 → **296 GB 常驻**。分桶后 174 GB，仍是一个纯为了「下一段号从哪开始」而付的存储账单。
- 问题二：**持久性**。Redis 丢数据（重启 / 主从切换）后号段回退 → 同一 `(conv_id, seq)` 可能被分配两次。基线 §10 的对策是「号段只增不减；Redis Lua 校验单调」，但 Lua 只能防逻辑回退，防不住数据本身丢失；且 Cluster 是异步复制、「last failover wins」，故障切换存在**已确认写入丢失的窗口**（[cluster spec: Write safety](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)）。
- 建议（成本见 §7.3）：分桶 hash（47 B/会话，−41%）+ 会话级 TTL + **miss 时从 OB `max(seq)` 重建并前移 margin**。`seq` 的语义是「先后」不是「连续」，空洞无副作用（基线 §8.3 已确认），所以重建只需保证单调，不需要精确。margin 的下界由「单会话在途消息数」决定：万人群 50 条/s × 秒级在途 ≈ 50，取 1000 有 20 倍余量。

### 2.5 `idem:{uid}:{clientMsgId}`：不是结构错，是 TTL 算错

活键数 = 写入 QPS × TTL。基线写 10 min：

| 规模 | 写入速率 | 活键数 | 内存 | 备注 |
|---|---|---|---|---|
| L2 峰值 | 10 万/s | 6000 万 | **6.7 GB** | 占 L2 内存预算 60% |
| L1 峰值 | 750 万/s | **45 亿** | **504 GB** | 且 45 亿 > 单实例 `2^32` 键上限（[FAQ](https://redis.io/docs/latest/develop/get-started/faq/)） |
| L1，TTL 90 s | 750 万/s | 6.75 亿 | **76 GB** | 仍须多分片，但量级可接受 |

90 s 够不够？基线 §10 已写明兜底是「`clientMsgId` + SETNX + 分区内 `uk_msg` 唯一索引」，即 SETNX 只是**省一次无效写入**的优化，不是正确性来源；客户端重传窗口是秒级（§7：3 次重传后转「离线待拉取」）。所以 TTL 可以按「重传窗口」定，而不是按「用户可能等多久」定。

实现细节两条：

- 用 **`SET key val NX EX 90`** 单命令，而非 `SETNX` + 单独 `EXPIRE`：后者两步之间进程崩溃会留下**永不过期**的幂等键（永久污染）。实测 SET NX EX = 6 µs。
- TTL 必须**加抖动**（±15%），否则同一秒建立的幂等键会在同一秒集中过期 —— 官方明确写了「大量键在同一秒过期、且占带 TTL 键的 25% 以上时，Redis 可能阻塞」（[latency: Latency generated by expires](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)）。
- hash 分桶 + `HEXPIRE` 能再省 18%（92 B vs 112 B，实测），但要多一次 RTT（HSET 4 µs + HEXPIRE 7 µs）并依赖 Redis ≥7.4（[HEXPIRE](https://redis.io/docs/latest/commands/hexpire/)）。**不推荐**：省下的 18% 不如改 TTL 省下的 85%。

### 2.6 `group:members:{gid}` 与 `rel:{uid}`：两个 blob 的读放大

两者是同一类问题，放在一起看（详细替代结构见 §3）：

| | `group:members:{gid}` | `rel:{uid}` |
|---|---|---|
| 基线形态 | 万人群 pb(memberList)，30 min | pb{friends, blocks, groups}，30 min |
| 单次读成本 | 90 KB GET = **29 µs** + 90 KB 出网 | 5000 好友 47 KB HMGET = **23 µs** + 47 KB 出网 |
| 触发频率 | 每条群消息 1 次（路由模式） | **每条消息**（发送前关系判定） |
| L2 下的读流量 | 2 万群消息/s × 4.75 KB（500 人小群）= **95 MB/s** | 10 万 msg/s × 2.5 KB（均值 200 好友）= **250 MB/s** |
| 最坏情况 | 万人群误走路由模式：50 条/s × 90 KB = **4.5 MB/s per 群** | 重尾用户 100 次/s × 47 KB = **4.7 MB/s**，且是同一条 shard |

两条都能靠「本地缓存 + 事件失效」把 90% 的读挡在进程内（基线 §8.1 的「事件失效」思路对），但**缓存不能代替正确的结构**：缓存未命中时的单次成本仍是 23–29 µs + 数十 KB，且重尾用户就是最常被读的那批。

---

## 3. 万人群视角：哪些 key 会变成大 key

### 3.1 判据

业界通行阈值（厂商规范，非 Redis 官方源码）：

- **阿里云 Redis 键值规范**：string ≤ 10 KB；hash/list/set/zset 元素数 ≤ 5000（[来源](https://developer.aliyun.com/article/1009125)）。
- **华为云 DCS**：单 string ≥ 10 KB，或集合类型总大小 ≥ 50 MB，或集合元素 > 5000，即判为大 key（[来源](https://support.huaweicloud.com/intl/zh-cn/bestpractice-dcs/dcs-bp-0220411.html)）。
- 腾讯云补充了大 key + 热 key 的组合症状（「含 2000 field 的 Hash 每秒大量 HGETALL」）（[来源](https://www.tencentcloud.com/document/product/239/54756)）。

按阿里云口径（string 10 KB / 集合 5000 元素）逐条判定：

| Key | 万人群 / 满载规模 | 估算字节 | 判定 | 替代结构（实测） |
|---|---|---|---|---|
| `group:members:{gid}` | 1 万成员 | **90 KB**（packed varint 9–10 B/uid） | **大 key**（超 string 阈值 9×） | 分片 intset 20×500：**84 KB**，单块 4.2 KB，SISMEMBER 2–4 µs |
| | 同上，若改用单一大 set | **490–677 KB** | **大 key**（元素 1 万 > 5000，且 SMEMBERS 285 µs 阻塞） | 同上 |
| `route:n:{node}` | 100 万连接 | **64.4 MB** | **大 key**（元素数超阈值 200×；超 50 MB 集合阈值） | 计数器 + 64 块反向索引（单块 ~1 MB） |
| `rel:{uid}` | 5000 好友 | **47–51 KB** | **大 key**（超 string 阈值 5×） | 拆 + 分片 intset 10×500：42–54 KB，SISMEMBER 2 µs |
| `idem:{uid}:{clientMsgId}` | 单键 112 B | — | 不是大 key，是**键数量**问题（§2.5） | 降 TTL + 抖动 |
| `seq:seg:{convId}` | 单键 80 B | — | 不是大 key，是**键数量 + 永远不过期**（§2.4） | 分桶 hash（47 B）+ TTL |
| `route:u:{uid}` | 104 B | — | 健康 | 保持，改 hash |

### 3.2 为什么「分片 intset」同时赢下内存与访问

实测（10 个万人群，int64 十进制 uid，20 块 × 500）：

| 方案 | 每群内存 | 每成员 | 编码 | 单成员判定 | 单次全量读 | 单键大小 |
|---|---|---|---|---|---|---|
| 单一大 set | **490 KB**（`MEMORY USAGE` 677 KB） | 49 B | hashtable | SISMEMBER 3 µs | SMEMBERS **285 µs 阻塞** | 490 KB |
| pb blob 90 KB | **105 KB** | 10.5 B | string | 不支持（只能整块读） | GET 29 µs + 90 KB 出网 | 90 KB |
| **分片 intset 20×500** | **84 KB** | 8.4 B | intset | **SISMEMBER 2–4 µs** | 20 次 SMEMBERS，各 9 µs | **4.2 KB/块** |

为什么差这么多：intset 对全整数集合按 int64 紧凑存（8 B/成员），而 hashtable 每个元素要 `dictEntry` + SDS 头 + 桶指针（49–52 B/成员，官方阈值 `set-max-intset-entries 512` 就是这两条路径的分界）。**500 元素/块**是有意的：正好卡在 intset 上限内。

代价与约束：

1. **成员必须是整数**。uid 是 64 位整数 → 成立；若存在字符串 ID（如外部 unionId），退化成分片 hash（64 B/field）。
2. **分片函数必须确定**：`chunk = crc16(uid) % 20`，这样 `SISMEMBER` 只需 1 次、`SADD`/`SREM` 也是 1 次，不需要「在哪一块」的索引。
3. **全量遍历变 N 次往返**。但「需要全量成员」的只有路由模式（≤500 人 = 1 块）和群管理页；万人群走广播模式（§6.3）**根本不需要成员列表**，只需要「某个 uid 是否在群里」——正好是 O(1) 判定。

> 顺带印证：基线把广播阈值定在 500 人，恰好让路由模式的成员 blob ≤ 500 × 9.5 B ≈ **4.75 KB**，落在阿里云 10 KB 阈值内。这个阈值选得对不对另说，但它和「blob 只在人群时读」是自洽的。真正不自洽的是：**万人群的成员 blob 谁在读？** 广播模式不该读它，可它仍以 90 KB/群 常驻。

### 3.3 读放大与写放大的量化

- **`group:members` 写放大**：blob 方案下，任何一次入群/退群都要「读 90 KB → 改 → 写 90 KB」；1000 个万人群、每群每秒 1 次成员变更 = 180 MB/s 的纯重写流量。分片 intset 下是 `SADD`/`SREM` 一次 8 B 级的写入。
- **`rel` 双写**：基线同时维护 `rel:{uid}.groups` 与 `group:members:{gid}`。同一份「谁是群成员」写两处，事件失效时两边都要失效，缺一边就出现「群里有他、他的关系表里没这个群」。建议 group 成员判定只认 `group:members`（O(1)），`rel` 不存 groups。
- **群投递路由读**：基线 §2.1 的群投递预算是 ≤10 万投递/s，对应 ≤10 万次 `route:u` 读/s。按小 GET ≈ 0.35 µs 服务端成本（`redis-benchmark -P 16` 测得 2.88M ops/s）→ **0.035 核**，不构成瓶颈。真正要注意的是**不许逐 uid 一次 RTT**：10 万次往返/s 是不可接受的，必须 pipeline 批量化（基线 §14 待决问题 C4 已识别同类问题）。

---

## 4. `SCAN` / `HGETALL` 的真实成本

### 4.1 复杂度口径

| 命令 | 官方复杂度 | 含义 |
|---|---|---|
| `SCAN` / `HSCAN` / `SSCAN` | **每次调用 O(1)，完整迭代 O(N)**（[SCAN 文档](https://redis.io/docs/latest/commands/scan/)） | 单次调用不阻塞；总成本与集合大小成正比 |
| `HGETALL` / `SMEMBERS` / `HVALS` | **O(N)** | 单次调用内跑完，N 大就阻塞 |
| `DEL` | O(N)（N = 集合元素数） | 大 key 删除阻塞主线程 |
| `UNLINK` | O(1)（回收在后台线程） | 大 key 删除不阻塞 |

SCAN 文档明确写了 `KEYS`/`SMEMBERS`「可能把服务器阻塞数秒」，SCAN 家族是为「生产环境不阻塞」设计的；但代价是**弱保证**：元素可能重复返回（应用必须幂等）、迭代期间新增的元素行为未定义。

### 4.2 实测（1M field hash / 1 万元素 set）

| 操作 | 服务端耗时 | 判读 |
|---|---|---|
| `HGETALL`（1M field） | **104,023 µs** | 单次 104 ms 全实例阻塞 → 生产禁用 |
| `HSCAN COUNT 1000` × 1000 次（完整迭代） | 394,693 µs 总计（394 µs/次） | 总 CPU 相当，但**分散在 1000 次调用**，每次阻塞 0.4 ms |
| `SMEMBERS`（10k 成员） | 285 µs | 大群禁用；分片后每块 9 µs |
| `SISMEMBER`（10k set） | 3 µs | O(1)，可放心用 |
| `DEL`（1M field） | **93,941 µs** | 禁用 |
| `UNLINK`（1M field） | **7 µs** | 默认用它 |
| `EXPIRE` × 20 万 | 41,210 µs（0.21 µs/次） | TTL 刷新/批量过期本身很便宜（§6 会用到） |

### 4.3 集群下 SCAN 的三个额外约束

1. **`SCAN` 只扫当前节点**：cluster 节点不做代理（[cluster spec](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)），所以要遍历全部 master 才能覆盖整个 keyspace —— 运维脚本要连 N 个节点各自 SCAN 一遍。（推断，依据：`SCAN` 定义是「当前被选数据库的键空间」+ cluster 无代理。）
2. **keyspace notifications 在集群里是逐节点的**，且是 fire-and-forget：官方原文「要收到集群全部事件，客户端必须订阅每个节点」，「投递是 fire-and-forget，断连期间的事件全部丢失」（[keyspace notifications](https://redis.io/docs/latest/develop/pubsub/keyspace-notifications/)）。
3. **过期事件的时间不可信**：`expired` 事件是在**键被删除时**发出，而不是 TTL 归零时；无访问的键可能延迟很久（同上文档）。

→ 结论：**任何「清理/对账/失效」逻辑都不能依赖 keyspace notifications**，只能依赖显式的业务事件，或者可重复的 SCAN 巡检。这条直接决定 §6 的清理方案。

---

## 5. 集群模式下的 slot 倾斜

### 5.1 先排除伪问题：哈希本身的均匀性

16384 个槽、`CRC16(key) mod 16384`（[cluster spec: Key distribution model](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)）。按我们 L1 的 key 量估算：

| 场景 | 总 key | 每槽平均 | 标准差 ≈ √均值 | 相对波动 |
|---|---|---|---|---|
| `route:u` 1.5 亿 | 1.5e8 | 9,155 | 96 | ±1.0% |
| `idem` 6.75 亿（90 s TTL） | 6.75e8 | 41,199 | 203 | ±0.5% |
| `seq:seg` 37 亿 | 3.7e9 | 225,830 | 475 | ±0.2% |

**结论：key 数量本身不会倾斜。** 阿里云把「某分片指标高出其他分片 20% 以上」定义为倾斜（[来源](https://www.alibabacloud.com/help/zh/redis/user-guide/deal-with-data-skew-issues)），我们按均匀哈希估算的波动是 1% 量级，差两个数量级。

### 5.2 真实倾斜只有三种，且都能定位到具体 key

| 类型 | 我们的具体位置 | 量化 | 对策 |
|---|---|---|---|
| **大 key 倾斜**（单键体积压在某个 shard） | `route:n:{node}` 64 MB；万人群 blob 90 KB | 3 master 时一个 shard 只占 5461 槽；64 MB 键落在谁头上谁多 64 MB。1 万个 90 KB blob 只占 0.9 GB，**不构成**倾斜 | 分块（§3.2） |
| **写倾斜**（单键承载一台节点的全部连接变更） | `route:n:{node}`：1 台网关 100 万连接、1%/s 重连 = **1 万次 HSET+HDEL/s 全部打到同一个 slot** | 单实例 10–25 万 ops/s，占 4–10%；CPU 可承受，但该 shard 的其他业务被迫共享同一个单线程 | 分块后分散到 64 个 slot |
| **读倾斜 / 热 key** | `rel:{uid}`（重尾用户）；若万人群误走路由模式则 `group:members:{gid}` | 重尾用户 100 次/s × 47 KB = 4.7 MB/s 单 shard 出网 | 进程内缓存 + single-flight；只读场景可走 replica（`READONLY`，允许脏读，[cluster spec](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)） |

阿里云对「访问倾斜」的判词是：**重分配 slot 解决不了热点**，只能多副本或本地缓存（[来源](https://www.alibabacloud.com/help/zh/redis/user-guide/deal-with-data-skew-issues)）。我们的热 key 都是「可缓存的只读数据」，所以缓存 + 事件失效是正解，不需要副本打散。

### 5.3 一条会给未来埋坑的规则：hash tag 会把倾斜固化

`{...}` 里的内容参与分槽（[cluster spec: Hash tags](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)）。一旦为了「多 key 原子」给 key 加 tag，就把这些 key **绑死在同一个 slot**：

- 好用法：`idem:{uid}:{cid}` 单 key 操作，不需要 tag；只有「route 写入 + 反向索引写入必须原子」时才需要，而那件事我们决定不做（§6）。
- 坏用法：把 `group:members:{gid}` 的所有分块加同一个 `{gid}` tag → 20 块回到同一个 slot，分片的收益归零。**分块的 tag 必须是块号，不是 groupId**：`group:m:{gid}:{c}` 的裸键名本身已经足够（不同 gid 自然分散），但如果要保证「同一群的块在同一个 slot 以便一次 SMEMBERS 批处理」，就得接受倾斜。我们的选择是**不要求同 slot**（20 次 pipeline 往返，而非 1 次）。

---

## 6. 节点宕机后的孤儿路由清理

### 6.1 问题的真实形状

网关崩溃后，`route:u:{uid}` 里仍然写着「用户在 gw-07」。这不只是垃圾：如果 gw-07 重启后**复用了同一个 nodeId**（同 IP 的 Pod、同机进程重启），旧路由「看起来仍然有效」，投递会把 NOTIFY 发给一个「活着但没有这条连接」的节点 —— 消息静默丢弃，且服务端无法区分「用户在线只是路由过期」和「用户真的离线」；拉取链路也不会被触发，因为 NOTIFY 从未到达客户端。丢了消息的用户要等自己重连（§6.8 还是错峰退避 5 s）才恢复。

先确认基线现状：§8.1 只写了 `route:u` 的过期是「连接断开时删」，§6.8 明确心跳「纯本地，不查 Redis、不写日志」，§6.9 的平滑重启依赖「向所有连接下发 SERVER_DRAIN」。**即：进程被 kill -9、OOM、机器掉电这些非优雅路径，基线没有任何清理机制。**

### 6.2 三层方案

**第 0 层（正确性，必须）：路由值带节点代次，读侧校验。**

```
route:u:{uid} = hash{ deviceId → "gw-07#17" }     # 17 = 该节点本次启动的代次
route:nodes   = hash{ "gw-07" → "17#alive" }      # 节点注册表（也可放 Nacos，Redis 只做本地缓存源）
```

投递侧维护一份**进程内**的 `nodeId → epoch/state` 映射（1 s 刷新，来源是 Nacos 事件或 `route:nodes`），发现 `epoch` 不一致或 `state != alive` → **直接判定用户离线**，不投递、不读它的 `route:u`。

- 生效延迟：≤1 s（刷新周期），与节点数量无关；
- 成本：**0 次额外 Redis 操作**（写侧多 6 B 值长度，读侧走本地缓存）；
- 重启复用 nodeId 的坑被堵住：新进程的 epoch 变了，旧路由自动失效。

**第 1 层（垃圾回收）：分块反向索引 + UNLINK。**

```
route:n:cnt:{node}            # INCRBY/DECRBY，连接数，给调度和告警
route:n:{node}:{chunk}         # hash{ uid → deviceBit }，chunk = crc16(uid) % 64
```

节点被判死之后：直接 `UNLINK` 它的 64 个块键（64 次 O(1) 调用，实测 UNLINK 大键 7 µs）。**不要逐 uid 删 `route:u`**：那需要「读-比对-删」的 CAS（否则会把用户刚在新节点建好的路由删掉），100 万连接 = 100 万次往返，还会产生竞态。让 `route:u` 自己过期（第 2 层）。

**第 2 层（兜底 TTL）：滚动刷新，而不是全量刷新。**

给 `route:u` 一个远大于刷新周期的 TTL（取 2 h），并在「节点负载上报」这条既有的 1 s 定时任务里，**每秒只刷新 1/3600 的切片**（1 h 覆盖全部连接一轮，TTL 2 h → 安全边际 2 倍）。

- 成本实测：`EXPIRE` 0.21 µs/次；L1 1.5 亿连接 ÷ 3600 s ≈ **4.2 万次/s** → 8.8 ms CPU/s（≈0.9% 单核），可忽略；1000 条一批的 pipeline → ≈42 次 RTT/s。
- 为什么不「每条连接一个定时器」：1.5 亿个定时器在 JVM 里的堆与 CPU 成本远超 Redis 侧；为什么不「TTL 到期靠客户端重连兜底」：**长连接不会因为路由过期而断开**，所以 TTL 只是垃圾回收，不是正确性。

> 反例说明：如果改成「TTL = 存活判据」（比如 TTL 60 s + 每 60 s 全量刷新），L1 需要 1.5 亿/60 = **250 万次 EXPIRE/s**，是 idem 写入量的 1/3，纯属浪费 —— 这就是为什么不把 TTL 当心跳。

### 6.3 为什么不能用 keyspace notifications 做清理

官方三条硬限制（[keyspace notifications](https://redis.io/docs/latest/develop/pubsub/keyspace-notifications/)）：

1. **fire-and-forget**：订阅端断连期间的事件全部丢失，没有补发；
2. **集群里逐节点**：事件不广播，要收全量必须订阅每个 master；
3. **`expired` 事件时间是「删除时刻」而非「TTL 归零时刻」**：无访问的键可能延迟很久才发。

任何依赖它的回收逻辑都会在「正好需要它的时候」（节点故障、连接风暴）丢事件。

### 6.4 一个基线没覆盖的场景：Redis 自身重启

路由表不持久化（§7 会建议这么做），那么 Redis 重启后路由表是空的。此时：

- 网关与客户端的 TCP 连接**都还在**（Redis 挂了不影响长连接）；
- 客户端**不会**重连，因此不会重新注册路由；
- 投递侧发现路由 miss → 按 §6.2 第 0 层判定「用户离线」→ **所有 NOTIFY 静默丢弃**，直到用户碰巧重连。

这不是理论风险：Redis 重启 + 主从切换都是常规运维动作。

**解法（二选一，建议两者都做）**：

1. **重推**：Redis 启动时把 `route:gen`（代次计数器）自增；网关轮询发现 `gen` 变化 → 把自己本地的连接表（网关是连接的唯一真源）**分批 pipeline 重推**。100 万连接 / 每次 1000 条 = 1000 次 RTT，1–2 s 内恢复；网关自身不需要断开任何连接。
2. **降级**：路由 miss 期间，投递切到「广播 topic」，每个网关本地过滤自己的连接（这正是 §6.3 广播模式已有的能力），限流保护，恢复后自动切回。

代价：重推 100 万 × (HSET+EXPIRE) 的 2 次调用 = 200 万 ops/节点，pipeline 后约 2000 次 RTT，2 s 内摊完；相比「整网用户被静默漏消息」，这个代价必须付。

### 6.5 清理方案成本汇总

| 环节 | 方案 | 触发 | L1 成本（1.5 亿连接 / 100 节点） |
|---|---|---|---|
| 正确性 | epoch 校验（本地缓存） | 每次投递 | 0 次 Redis 操作 |
| 立即失效 | 节点注册表置 dead | 节点崩溃 | 1 次写 + 全部投递侧 1 s 内生效 |
| 垃圾回收 | 64 块 `UNLINK` | 节点被判死 | 100 节点 × 64 = 6400 次 O(1) |
| 兜底 | 滚动 TTL 刷新（每秒 1/3600 切片，TTL 2 h） | 每 1 s | 4.2 万 EXPIRE/s ≈ 0.009 核 |
| 重启恢复 | gateway 重推 + miss 降级广播 | `route:gen` 变化 | 200 万 ops/节点，2 s 内 |

---

## 7. 持久化（RDB / AOF）对写延迟的影响

### 7.1 RDB：fork 是唯一的大头，且与内存成正比

官方给的实测 fork 成本（[latency: Fork time in different systems](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)）：

| 环境 | 6 GB RSS 的 fork 时间 | 折算 |
|---|---|---|
| 物理机（Xeon 2.27 GHz） | 62 ms | **9 ms/GB** |
| VMware 上的 Linux VM | 77 ms | 12.8 ms/GB |
| KVM | 360 MB → 8.2 ms | 23.3 ms/GB |
| EC2 老 Xen 实例 | 6.1 GB → **1460 ms** | **239 ms/GB** |

原因写在文档里：页表本身要复制，24 GB 实例的页表 = 24 GB / 4 KB × 8 = **48 MB**。推论：

- 我们 20 GB 的 Redis 实例 → fork ≈ **180–260 ms 的全实例停顿**（物理机/现代 VM），每做一次 BGSAVE 付一次；
- 还要预留 **COW 内存**：fork 后父进程的写入会复制页，最坏接近 2 倍。这也是官方建议 `maxmemory` 留余量的原因（[eviction 文档](https://redis.io/docs/latest/develop/reference/eviction/)：持久化/复制的缓冲区不计入 `maxmemory`）。
- **必须关 THP**：官方明确「THP 开启时 fork 后会出现巨大延迟与内存膨胀」（同上 latency 文档）。

### 7.2 AOF：延迟来自 `fdatasync`

官方描述（[latency: Latency due to AOF and disk I/O](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)）：

- `fdatasync` 在某些内核/文件系统组合下**「几毫秒到几秒」**；Redis 2.4 起把它放到后台线程做；
- `everysec`：后台线程 fsync，若 fsync 尚未完成，Redis 用缓冲区把 `write(2)` **延迟最多 2 秒**；fsync 拖太久时仍会照样 write → 这就是延迟尖峰的来源；
- `always`：每次写都在返回 OK 前 fsync（会合并多命令做 group commit），文档判词是「性能非常低」；
- 官方给的稳定优先顺序：`always` < `everysec` < `everysec + no-appendfsync-on-rewrite` < `no` < `RDB`。

对我们的量化：

| 场景 | 单写额外成本 | 10 万写/s 是否可行 |
|---|---|---|
| `appendfsync no`（交给内核） | ~0 | 可行 |
| `everysec` | 均摊 ~0，但**每秒一次 fsync + 可能的 2 s write 延迟窗口** | 可行，代价是 p99 尖峰 |
| `always` | 每次 1 次 fsync：NVMe 约 50–200 µs（此为通用经验值，未在本机实测） | **不可行**（上限 5k–20k ops/s） |

AOF rewrite 会**再 fork 一次**（[persistence 文档](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/)），所以「AOF everysec + 周期 rewrite」= 周期性 fork 停顿；官方建议 `no-appendfsync-on-rewrite yes` 压制 rewrite 期间的 fsync 抖动。

### 7.3 结论：按 key 分级，而不是全局一个配置

| Key family | 是否需要落盘 | 理由 | 配置 |
|---|---|---|---|
| `route:u` / `route:n` | **不需要** | 纯派生数据，网关是唯一真源；丢失后靠 §6.4 重推恢复 | `save ""` + `appendonly no`；`maxmemory-policy noeviction` |
| `group:members` | 不需要 | 可以从 OB 重建；30 min TTL 已经是缓存语义 | 同上 |
| `rel` | 不需要 | 同上，事件失效 + TTL | 同上（`volatile-lru` 也可，因为它只是缓存） |
| `idem` | **不需要** | 基线 §10：正确性由 `uk_msg` 唯一索引兜底 | 同上；必须与 `seq` 分开集群，否则 `allkeys-lru` 会误删 seq |
| `seq:seg` | **需要单调性**，不需要精确值 | 号段回退 = 重复 seq | `appendonly yes` + `appendfsync everysec` + 副本；**外加** miss 时从 OB 重建 |

两条容易踩的坑：

1. **`maxmemory-policy` 必须是 `noeviction`**（路由/号段集群）：被淘汰的路由 = 用户静默离线，被淘汰的号段 = 序号回退。`volatile-*` 系列在没有 TTL 的键上退化成 `noeviction`，可作过渡（[eviction 文档](https://redis.io/docs/latest/develop/reference/eviction/)）。代价：写满时**写入报错**（不是静默降级）—— 这符合基线 §15.1「宁可要会告警的失败」。
2. **seq 的兜底必须能自愈**：`SETNX`-式重建 —— 号段 miss 时读 `SELECT max(seq) FROM t_message WHERE conv_id=?`（走 `(conv_id, seq)` 索引，O(log N)），加 margin 后 `SET` 回 Redis。margin 由「单会话在途消息数」定：万人群限速 50 条/s、在途按秒级算 → 取 1000 有 20 倍余量。这条同时让 `seq` 从「源」降级成「缓存」，于是它可以有 TTL、可以被淘汰、丢失也不致命 —— 与 §2.4 的内存结论合流。

---

## 8. Redis Cluster 还是哨兵

### 8.1 两者的硬差别（官方口径）

| 维度 | 哨兵（Sentinel + 主从） | Cluster |
|---|---|---|
| 写入能力 | **单主**，一核一线程；文档口径「几乎单线程服务所有请求」 | 线性扩展到「建议 ~1000 master」（[cluster spec: goals](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)） |
| 容量 | 单机内存上限 | 按 shard 横向扩 |
| 多 key 原子 | 任意 key 都能进同一事务/Lua | **必须同 slot**，靠 hash tag；resharding 期间多 key 操作可能返回 `-TRYAGAIN` |
| 数据库编号 | 支持多 db | **只支持 db 0**，`SELECT` 不允许 |
| `SCAN` / keyspace notifications | 全库一次 | **逐节点**，要遍历/订阅每个 master |
| 故障切换 | Sentinel 选主 | gossip + epoch 选举；**异步复制，存在已确认写入丢失窗口**（last failover wins） |
| 客户端复杂度 | 一个连接池 + Sentinel 感知 | 需要 slot 路由、`MOVED`/`ASK` 处理 |

来源：[cluster spec](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)、[replication 文档](https://redis.io/docs/latest/operate/oss_and_stack/management/replication/)。

### 8.2 用我们的数字算

单实例写能力：本机实测 **24.9 万 ops/s**（`SET`，c=50，无 pipeline，无持久化，M5 loopback）；生产 Xeon/云主机按 **10–25 万 ops/s** 取区间（10 万为保守假设，25 万为本机实测上限）。

| 规模 | Redis 写 ops/s 估算 | 单实例余量 | 结论 |
|---|---|---|---|
| L2 峰值 | idem 10 万 + route 变更 ~1 万 + seq 100 ≈ **11.1 万/s** | 本机 2.2 倍；云主机 **~1 倍（无余量）** | 哨兵已到边缘 |
| L1 峰值 | idem 750 万 + route 150 万 + seq 7500 ≈ **900 万/s** | **36–90 分片**（按单实例 10–25 万 ops/s） | 哨兵不可能 |

内存侧同样判死：L1 的 `idem` 一项在基线 TTL 下就是 504 GB，单机装不下（§2.5）。

### 8.3 结论与代价清单

**选 Cluster**，但把它当成「必须付出的复杂度」而不是「顺手拿的好处」：

1. **所有热路径操作必须是单 key**。六个 key 里五个天然单 key；唯一的多 key 场景是「写路由 + 写反向索引」，我们选择**不要求原子**（第 0 层的 epoch 校验让它不必原子），从而不需要任何 hash tag。
2. **按 key family 拆 3 个逻辑集群**（不是 1 个大集群，也不是 100 个）：`route`（无持久化 / noeviction）、`seq`（AOF / noeviction）、`idem + rel + group`（无持久化 / 可淘汰）。理由是三者的持久化策略**互斥**（§7.3）—— 同一个实例上开 AOF，会让纯缓存的 key 也付 AOF 的抖动；同一个实例上开 `allkeys-lru`，会静默淘汰号段。
3. 接受这些代价：
   - 客户端要处理 `MOVED`/`ASK`（Lettuce / Jedis-cluster 已实现，但要测 reshard 期间的行为）；
   - reshard 期间多 key 操作可能 `-TRYAGAIN` → 我们不用多 key 操作，无影响；
   - 运维/GC 脚本要逐节点 SCAN（§4.3）；
   - failover 丢写窗口 → `seq` 必须有重建兜底（§7.3），`route` 有重推（§6.4）。
4. **不选「哨兵 + 客户端按 uid 取模分 4 个独立集群」**：它确实能绕开 CROSSSLOT，但扩容要自己搬数据、没有自动 rebalance、每个集群仍要一套 Sentinel。从 L2 到 L1 的迁移成本远高于一开始就用 Cluster。

---

## 9. 与基线的冲突（四段格式）

> 按「改起来最贵」排序：C1–C3 是结构错（大 key / 读放大），C4–C6 是容量与部署形态，C7–C9 是正确性缺口。

### C1 `route:n:{node}` 用单个 hash 承载一台节点的全部连接

**① 原方案**（§8.1「节点连接数 `route:n:{node}` = `hash{uid → deviceBit}`，随连接」）
**② 新结论**：拆成 `route:n:cnt:{node}`（计数器，给调度/告警）+ `route:n:{node}:{chunk}`（64 块反向索引，给 GC）。基线形态在满载时是**只写不读的 64 MB 大 key**。
**③ 理由**：§6.1 的连接调度由 ip-conf 本地计算，§8.1 的 `route:n` 没有任何读方；而它的大小与单节点连接数成正比（100 万连接 = 64.4 MB 实测），触发三个阻塞：`HGETALL` 104 ms、`DEL` 94 ms、`MIGRATE` 数十 ms，且 1 万次/s 的连接变更全打在同一个 slot。
**④ 性能/成本差异**：内存 64.4 MB → 计数器 8 B + 64 × 1 MB（仅 GC 需要时）；删除 94 ms → 7 µs（UNLINK）；写热点从 1 个 slot 分散到 64 个 slot。出处：§4.2 实测表；[HGETALL 复杂度](https://redis.io/docs/latest/commands/hgetall/)、[UNLINK](https://redis.io/docs/latest/commands/unlink/)。

### C2 `group:members:{gid}` 用 pb(memberList) 单 blob

**① 原方案**（§8.1「大群成员 = `pb(memberList)`，30 min」，§6.3「大群清单放 Redis，避免每条群消息都去数成员」）
**② 新结论**：改分片 intset（`group:m:{gid}:{crc16(uid)%20}`，500 元素/块）。blob 只适合「≤500 人的路由模式」，万人群不该有这个 key。
**③ 理由**：万人群 blob = 90 KB，超阿里云 string 阈值 9 倍；`GET` 是 O(N) 且每次复制整块；成员变更要整块重写（写放大 90 KB/次）。而广播模式（§6.3）**不需要成员列表**，只需要「某 uid 是否在群」的 O(1) 判定，blob 连这个都做不到。分片 intset 同时更小、更快、无大 key。
**④ 性能/成本差异**：内存 105 KB → 84 KB（−20%）；单成员判定从「读 90 KB + 反序列化」→ `SISMEMBER` 2–4 µs；成员变更写入 90 KB → 8 B 级；单键体积 90 KB → 4.2 KB。出处：§3.2 实测表；intset/hashtable 分界见 [redis.conf `set-max-intset-entries 512`](https://raw.githubusercontent.com/redis/redis/7.4/redis.conf)。

### C3 `rel:{uid}` 用 pb{friends, blocks, groups} 单 blob，且 groups 与 `group:members` 双写

**① 原方案**（§8.1「关系判定 = `pb{friends, blocks, groups}`，30 min，事件失效」）
**② 新结论**：拆成 `rel:fr:{uid}:{chunk}`（分片 intset，O(1) 判定）+ `rel:blk:{uid}`（小集合）+ **不再存 groups**（群成员判定只认 `group:members`）。
**③ 理由**：关系判定在**每条消息**的发送路径上，blob 一次复制 47 KB（5000 好友）；重尾用户就是最常发消息的人，形成热 key。`groups` 列表与 `group:members` 是同一份事实的两份副本，事件失效漏一边就产生「群里有他、他关系表里没这个群」的不一致。
**④ 性能/成本差异**：单次判定 23 µs + 47 KB 出网 → `SISMEMBER` 2 µs + 0 出网（缓存命中时 0）；内存 51.4 KB → 42–54 KB（相当，但无大 key）；省掉一条双写链路。出处：§2.6、§3.2 实测；L2 读流量估算 250 MB/s → 缓存命中后趋近 0。

### C4 `idem` 的 TTL 定 10 min

**① 原方案**（§8.1「`idem:{uid}:{clientMsgId}`，10 min」）
**② 新结论**：TTL 改为 **90 s + ±15% 抖动**，用 `SET NX EX` 单命令写入。
**③ 理由**：活键数 = QPS × TTL 是唯一的成本函数；基线的 10 min 在 L1 峰值下是 45 亿键 / 504 GB，且**超过单实例 2^32 键上限**，任何「按 uid 分片」的方案都要先把这个数算对。而幂等的正确性兜底是 `uk_msg` 唯一索引（基线 §10 自己写的），SETNX 只负责省无效写入，所以 TTL 只需覆盖客户端重传窗口（秒级）。
**④ 性能/成本差异**：L1 峰值 504 GB / 45 亿键 → **76 GB / 6.75 亿键（−85%）**；L2 峰值 6.7 GB → 1.0 GB；顺带修掉 `SETNX` + `EXPIRE` 两步之间崩溃导致幂等键永不过期的漏洞（写侧 2 次 RTT → 1 次，6 µs）。出处：§2.5；单实例 2^32 键上限见 [Redis FAQ](https://redis.io/docs/latest/develop/get-started/faq/)，string 值 512 MB 上限见 [Redis strings](https://redis.io/docs/latest/develop/data-types/strings/)；集中过期阻塞见 [latency: expires](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)。

### C5 `seq:seg:{convId}` 永不过期

**① 原方案**（§8.1「`seq:seg:{convId}`，过期：无」；§8.3「Redis 号段：进程重启丢弃未用完的号 → 产生空洞但不回退」）
**② 新结论**：改 hash 分桶 `seq:seg:{convId%1024}` + `HINCRBY`，加会话级 TTL，并支持 **miss 时从 OB `max(seq)` + margin 重建**。
**③ 理由**：37 亿会话 × 80 B = 296 GB 常驻，而 Redis 里存的值只是「下一段从哪开始」，可以从数据库重建；基线只考虑了「进程重启丢弃未用号」，没考虑 **Redis 自身丢数据**（重启/主从切换）——那时号段回退，`(conv_id, seq)` 会重复。基线 §10 的「Redis Lua 校验单调」只能防逻辑回退，防不住数据丢失。
**④ 性能/成本差异**：单会话 80 B → 47 B（−41%，实测）；L1 常驻 296 GB → 174 GB，且可被 TTL 收缩到活跃会话；重建成本 = 每次 miss 一次 `max(seq)` 索引查询（O(log N)），只在缓存冷启动时发生。出处：§2.4、§7.3；丢写窗口见 [cluster spec: Write safety](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)，重建思路对齐 [persistence 文档](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/) 的恢复语义。

### C6 把 Redis 当一个组件（全仓一个实例），且未指定持久化/淘汰策略

**① 原方案**（§15.2「路由 / 号段 / 幂等 = Redis」一行；§8.1 一张表；§15.4「L1：按 uid 分片，多集群；号段按 convId 分片」）
**② 新结论**：从一开始就按 key family 拆 **3 个逻辑集群**（route / seq / idem+rel+group），各自独立的持久化与淘汰策略；部署形态选 **Cluster**，不选哨兵。
**③ 理由**：三类 key 的持久化需求互斥（§7.3）：`seq` 必须 AOF、`route/idem/rel` 必须不落盘；淘汰策略也互斥：缓存类要 `allkeys-lru`，号段必须 `noeviction`。放在一个实例上，只能取交集 = 全都不满意。而 L2 峰值已经是 11.1 万 ops/s，哨兵单主在云主机上已无余量。
**④ 性能/成本差异**：L2 11.1 万 ops/s vs 单实例 10–25 万 → 拆 3 集群后每集群 <10 万，余量 ≥2 倍；L1 900 万 ops/s 需 36–90 分片，拆分后每集群 12–30 分片，故障域隔离（路由集群内存膨胀不会挤掉号段）。出处：§8.2；单实例上限实测 24.9 万 ops/s（本机），云主机 10–25 万区间中 10 万档为**假设**。

### C7 路由表的重启恢复没写

**① 原方案**（§8.1「在线路由，过期：连接断开时删」—— 隐含「路由不需要持久化，丢了等重连」；§6.9 只覆盖优雅重启）
**② 新结论**：补一套恢复机制：`route:gen` 代次计数器 + 网关发现 gen 变化后**分批重推本地连接表**；同时投递侧在路由 miss 时降级为「广播 topic + 网关本地过滤」，限流保护。
**③ 理由**：路由表不落盘是对的，但「丢了等客户端重连」不成立 —— **Redis 重启不会断开任何客户端长连接**，客户端没有任何理由重连，服务端却已经没有它的路由，于是 NOTIFY 静默丢弃。这是基线没有覆盖的一处「按设计会发生的数据丢失」。
**④ 性能/成本差异**：恢复成本 = 每节点 100 万 × (HSET + EXPIRE) ≈ 200 万 ops，pipeline 1000 条/批 → 2000 次 RTT，1–2 s 摊完；不做的代价 = 该窗口内全部下行通知丢失（用户侧表现为「消息发不出去、也不报错」）。出处：§6.4；网关是连接唯一真源（基线 §3「gateway 之间无状态，唯一状态是连接本身」）。

### C8 序号回退的对策只有「Lua 校验单调」

**① 原方案**（§10 可靠性表「序号回退 → 号段只增不减；Redis Lua 校验单调」）
**② 新结论**：加两条：`seq` 集群开 AOF `everysec` + 至少一个副本（必要时对号段写入用 `WAIT` 提高边际，但官方明确 `WAIT` 不构成 CP 保证）；并用「miss 重建 + margin」把 `seq` 降级成缓存，配合 `(conv_id, seq)` 唯一索引兜底。
**③ 理由**：Lua 校验只能在「值还在」时防回退；Redis 数据丢失/故障切换后值本身回退了，Lua 无从校验。Cluster 异步复制明确存在已确认写入丢失的窗口。
**④ 性能/成本差异**：AOF `everysec` 的均摊写成本 ≈0，代价是每秒一次后台 fsync（官方：`fdatasync` 可能「几毫秒到几秒」）+ rewrite 时的 fork 停顿（20 GB 实例 ≈180–260 ms）；换来的是一条「不会重复发号」的硬保证。出处：§7.1–7.2；[latency: AOF](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)。

### C9 `route:u` 的值用 pb blob

**① 原方案**（§8.1「在线路由 = `pb{deviceId → nodeId}`」）
**② 新结论**：改 hash（field = deviceId，value = `"nodeId#epoch"`）。
**③ 理由**：blob 的写入是「读-改-写」，同账号两端并发登录会丢一条路由（lost update），要靠 CAS/Lua 兜；hash 的 `HSET`/`HDEL` 按 field 原子，且按设备粒度清理（踢一端不影响另一端）。
**④ 性能/成本差异**：内存 2 field、23 B 键名 = 96–104 B（实测，listpack 编码），与 blob 同量级；写路径从 2 次 RTT（GET+SET）→ 1 次（HSET）；多端并发正确性无需额外机制。出处：§2.2 实测；[HSET O(1)](https://redis.io/docs/latest/commands/hset/)。

---

## 10. 未取得原文 / 待验证

| 项 | 状态 | 处理 |
|---|---|---|
| 52im.net 相关贴（如「支持百万人超大群聊的 Web 端 IM 架构」） | 仅见搜索引擎摘要，**未取得原文** | 未作为任何结论的依据；相关论点改用可读的一手/厂商文档 |
| 网易云信万人群方案（「一个成员 200 B → 万人群约 2 MB 缓存开销」） | 读到的为掘金/思否转载版，**非原始发布页** | 仅作旁证（与我们实测 105 KB–677 KB 量级不同，因口径含对象开销），不用于计算 |
| 阿里云 / 华为云大 key 阈值（10 KB / 5000 元素 / 50 MB） | 厂商规范文档（可点开），非 Redis 官方 | 明确标注为「厂商规范」，只用于**判定口径**，不用于性能结论 |
| 生产环境 Xeon/云主机单实例 10–25 万 ops/s | **区间**，其中 25 万为本机 M5 + loopback 实测（无持久化），10 万为保守假设 | L1/L2 分片数按区间给（36–90），不取单点 |
| L1 会话数 37 亿、在线 1.5 亿 | **假设**（5 亿 DAU × 人均 15 会话 / 2；DAU 的 30% 同时在线） | 内存表按「每亿 X GB」同时给出，便于换算 |
| Redis 7.4 `HEXPIRE` 的字段级 TTL 在 reshard 后的行为 | 未验证 | 因此不作为推荐方案（§2.5 已给出「不推荐」的理由不止这一条） |
| `MIGRATE` 64 MB 单键的具体阻塞时长 | 未实测（只测了 DEL/HGETALL 同键 94–104 ms） | §2.3 的结论不依赖具体毫秒数，依赖「单键越大迁移越慢」这一机制 |

---

## 11. 参考来源

**一手（Redis 官方 / 源码 / 规范）**

1. Redis cluster specification（16384 slots、hash tags、多 key 限制、write safety、迁移/MOVED/ASK）— https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/
2. SCAN / HSCAN / SSCAN（O(1)/次、O(N)/全量、弱保证、COUNT）— https://redis.io/docs/latest/commands/scan/
3. HGETALL（O(N)）— https://redis.io/docs/latest/commands/hgetall/
4. UNLINK（异步释放）— https://redis.io/docs/latest/commands/unlink/
5. HEXPIRE / 字段级 TTL（Redis ≥7.4）— https://redis.io/docs/latest/commands/hexpire/ ；hash 文档的 Field expiration 段 — https://redis.io/docs/latest/develop/data-types/hashes/
6. Redis persistence（RDB fork/COW、AOF 多部分、fsync 三档、rewrite）— https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
7. Diagnosing latency issues（单线程、fork 时间表、AOF fdatasync、THP、expires 集中过期）— https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/
8. Redis replication（异步复制、WAIT 的边界、副本不自行过期键）— https://redis.io/docs/latest/operate/oss_and_stack/management/replication/
9. Keyspace notifications（fire-and-forget、集群逐节点、expired 事件时间不可靠）— https://redis.io/docs/latest/develop/pubsub/keyspace-notifications/
10. Key eviction（noeviction 行为、volatile-* 退化、expire 占内存）— https://redis.io/docs/latest/develop/reference/eviction/
11. Pub/Sub 与 sharded Pub/Sub（集群内普通 PUBLISH 走全节点总线）— https://redis.io/docs/latest/develop/pubsub/
12. Redis FAQ（2^32 键上限；100 万小 string ≈ 85 MB；fork/COW）— https://redis.io/docs/latest/develop/get-started/faq/
13. redis 7.4 默认配置（listpack/intset 阈值、lazyfree、appendfsync）— https://raw.githubusercontent.com/redis/redis/7.4/redis.conf

**厂商规范（二手，仅作判定口径）**

14. 阿里云 Redis 键值设计规范（string ≤10 KB、集合元素 ≤5000、非字符串大 key 不要用 DEL）— https://developer.aliyun.com/article/1009125
15. 华为云 DCS 大 key / 热 key 判定（10 KB / 50 MB / 5000 元素）— https://support.huaweicloud.com/intl/zh-cn/bestpractice-dcs/dcs-bp-0220411.html
16. 阿里云 Tair 处理集群数据倾斜（热点不能靠重分 slot、多副本/本地缓存）— https://www.alibabacloud.com/help/zh/redis/user-guide/deal-with-data-skew-issues
17. 腾讯云大 key 与热 key 排查（HGETALL 大 hash 的症状）— https://www.tencentcloud.com/document/product/239/54756

**厂商技术分享（旁证）**

18. 网易云信万人群方案（按群维护在线成员、成员缓存开销量级）— https://juejin.cn/post/6844903912646901773
19. 企业微信 IM 架构（扩散写 vs 扩散读、万人群 1:9999 转发）— https://segmentfault.com/a/1190000040368812
20. 钉钉超级大群（写扩散 → 读扩散，扩散量降到万分之一）— https://segmentfault.com/a/1190000021118546

**本仓实测**：Redis 7.4.12 + `redis:7.4` 镜像，脚本见附录 A。

---

## 附录 A：实测脚本（可复现）

```bash
docker run -d --name redis-survey -p 6380:6379 redis:7.4
R="docker exec redis-survey redis-cli"

# 1) route:u 单键成本（1M 键，2 device field，19 位 uid 键名）
$R FLUSHALL >/dev/null; $R CONFIG RESETSTAT >/dev/null
BASE=$($R INFO memory | grep ^used_memory: | cut -d: -f2 | tr -d '\r')
$R EVAL "local pad='0000000000000000000' for i=1,1000000 do local s=tostring(i)
  redis.call('HSET','route:u:'..string.sub(pad,1,19-#s)..s,'d1','gw-07') end return 1" 0
AFTER=$($R INFO memory | grep ^used_memory: | cut -d: -f2 | tr -d '\r')
echo "route:u per-key = $(( (AFTER-BASE)/1000000 )) B"      # → 104

# 2) route:n 1M field：内存 + HGETALL/DEL/UNLINK 阻塞
$R FLUSHALL >/dev/null; $R CONFIG RESETSTAT >/dev/null
$R EVAL "local pad='0000000000000000000' for i=1,1000000 do local s=tostring(i)
  redis.call('HSET','route:n:gw-07',string.sub(pad,1,19-#s)..s,'3') end return 1" 0
$R MEMORY USAGE route:n:gw-07        # → 64,388,720
$R CONFIG RESETSTAT >/dev/null; $R HGETALL route:n:gw-07 >/dev/null
$R INFO commandstats | grep hgetall  # → usec_per_call ≈ 104,000
$R CONFIG RESETSTAT >/dev/null; $R DEL route:n:gw-07 >/dev/null
$R INFO commandstats | grep cmdstat_del      # → usec_per_call ≈ 94,000
# UNLINK 对照重建同键后：$R UNLINK route:n:gw-07 → ≈ 7 µs

# 3) seq / idem 单键成本
#    seq:seg:{convId} = SET 一个 int64 → 80 B/键
#    seq 分桶 = HINCRBY 'seq:seg:h:'..(i%1024) field  → 47 B/会话
#    idem = SET key val 'EX' 600 → 112 B/键；分桶 HSET+HEXPIRE → 92 B/field

# 4) 成员集合三种形态（int64 十进制 uid 必须用字符串拼接，Lua double 会丢精度）
#    单一大 set 10k        → 490 KB / SMEMBERS 285 µs
#    分片 intset 20×500    →  84 KB / 块 4.2 KB / SISMEMBER 2–4 µs
#    pb blob(90 KB 载荷)   → 105 KB / GET 29 µs

# 5) 单实例写吞吐
docker exec redis-survey redis-benchmark -p 6379 -t set -n 100000 -c 50 -q      # → ~249k rps
docker exec redis-survey redis-benchmark -p 6379 -t set -n 300000 -c 20 -P 16 -q # → ~2.88M rps
```

> 注：Lua 里用数字拼 uid 会被 double 精度截断（1e18 量级步长 256），导致只写入几十个键 —— 附录脚本因此用 `string.sub` 补零拼字符串。这个坑会让人以为内存测量「异常小」，实测时务必用 `DBSIZE` 校验键数。

# 可靠性投递调研（ACK / 重传 / 离线补偿）

- 票：#108
- 基线：`rewrite/v2@0acbc9e` → `docs/architecture/im-v2-architecture.md`（下称"基线"，引用按其章节号）
- 调研方式：一手文档 / 源码优先。厂商官方文档 > 协议规范 > 开源服务端源码 > 社区一手实现拆解 > 二手综述。**读不到原文的条目单列在 §11，不进结论。**
- 结论标记：无标记 = 有可点开来源支撑；`[推算]` = 由本仓基线的约束做算术得出，非外部事实；`[推断]` = 由来源推理，来源未直述。

---

## 0. 结论先行

1. **业界没有"端到端 exactly-once"。** 标准组合是「至少一次投递 + 幂等去重 + 缺口可检测」三件套。Kafka 官方把三个语义拆成"发给 broker 的持久性"和"消费位置"两个独立问题，并明确警告：很多系统的 exactly-once 声明在消费者/生产者故障、多消费者、磁盘丢数据三种情况下不成立（[Kafka Design · Message Delivery Semantics](https://kafka.apache.org/44/design/design/)）。

2. **ACK 必须分层，且每层不能冒充上一层。** 传输 ACK（MQTT PUBACK / MQ `SEND_OK`）只表示"所有权转移给接收方"，接收方**不需要**完成处理就可以回 PUBACK（[MQTT 5.0 §4.3.2](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html)）。把传输 ACK 当业务受理 ACK，是所有"假确认"的源头。

3. **基线的"推通知、拉内容"在 5 类具体序列下会漏消息**（§8 的 F1–F5），其中 F1、F2 是基线自己的条款互相矛盾造成的，不是实现失误。

4. **缺口可检测性是本设计最缺的一块。** 基线允许 `sequence` 有空洞（§8.3），于是"丢了一条"和"号段浪费 999 个"在客户端不可区分。业界用连续计数器（XMPP `h` 每 stanza +1、MQTT Packet Identifier）就是为了让缺口可见（[XEP-0198 §4](https://xmpp.org/extensions/xep-0198.html)）。**不要**改成连续分配（会打死 Redis 号段），而是服务端额外暴露 `committed_seq` 单调水位。

5. **"落库失败但 MQ 已 ack"的推荐解不是事务消息。** 事务消息解决的是"本地事务提交了但消息没发出"，本项目在 MSG 侧没有本地事务可绑定，用了只有成本没有收益（§7.3）。推荐：保留早 ACK + `committed_seq` + DLQ 重放；若要把 ACK 升级为"已落库"，选"先落库后 ACK"，而不是 outbox（消息表本身就是事实表，不需要再建一张 outbox 和 relay）。

6. **离线消息不要 Redis 离线箱。** 按基线自己的约束（§2.1：万人群 ≤ 50 条/s、最大群 10000 人）做算术，逐成员写离线箱是 `50 × 10000 = 5×10^5` 次写/秒，只为离线箱 `[推算]`。基线拒绝参考实现的 `offline_msg:u:{uid}` zset 是对的。

7. **最小改造 5 项**见 §10，其中前两项（`committed_seq`、per-device cursor）是正确性问题，其余是契约与运维问题。

---

## 1. ACK 的五层：每层到底承诺了什么

一条 IM 消息的"确认"在业界被拆成至少五层，每层的失败后果完全不同。把这五层混为一谈，就会出现"客户端显示已发送、对端永远收不到"。

| 层 | 名称 | 谁发 / 发给谁 | 确切语义 | **不代表** | 来源 |
|---|---|---|---|---|---|
| L0 | 写出成功 | 内核 / Netty | 字节进了 socket 缓冲区 | 对端收到、对端应用处理 | — |
| L1 | 传输 ACK | 接收方协议栈 | 接收方**接管了所有权** | 业务处理完成；持久化 | MQTT 5.0 §4.3.2 脚注：*"The receiver does not need to complete delivery of the Application Message before sending the PUBACK… ownership of the Application Message is transferred to the receiver"*（[MQTT 5.0](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html)） |
| L1' | broker ACK | MQ broker → 生产者 | 已写入 CommitLog（默认仅 PageCache） | 已 fsync、已被消费 | RocketMQ 官方：`SEND_OK` *"successful message delivery does not mean it is reliable. To ensure that no messages are lost, you should also enable sync Master or sync flush"*（[RocketMQ Best Practices](https://rocketmq.apache.org/docs/4.x/bestPractice/01bestpractice/)） |
| L2 | 受理 ACK | 服务端 → 发送方 | 已分配 `serverMsgId`/`seq`，且进入了持久化投递链路 | 已可被读取 | 基线 §6.2 定义即为这一层 |
| L3 | 落库确认 | 存储 → 服务端 | 该消息在时间线上可被 `convId+seq` 读到 | 已被任何设备看到 | Signal：ack 到 GUID 才 `removeFromStorage`（[MessageDispatcher](https://github.com/signalapp/Signal-Server/blob/main/service/src/main/java/org/whispersystems/textsecuregcm/grpc/MessageDispatcher.java)） |
| L4 | 设备 ACK | 设备 → 服务端 | **这一台设备**已持久化到本地 | 用户看到、其它设备收到 | XEP-0198 的 `h`（已处理 stanza 计数）；Signal 的 `server_guid_ack` |
| L5 | 送达回执 | 接收方客户端 → 发送方 | 消息已投递到**接收方的某个客户端** | 被阅读、被理解 | XEP-0184 明确：*"notifies the sender only that a message has been delivered to a client, not that it has been read or understood by a human user"*（[XEP-0184](https://xmpp.org/extensions/xep-0184.html)） |

XEP-0184 的版本史本身就是"分层"的教科书：0.3 版 **"Removed reliability features, which belong at a different level"** —— 回执协议一开始想把可靠性塞进回执里，后来被拆走（同上）。

**对基线的直接含义**：基线 §6.2 的 `SEND_ACK` 是 L2，§6.4 的 `NOTIFY` 只是"有东西可拉"的提示，L4 在基线里没有被定义（`ACK 1046` 帧存在于 `docs/protocol/wire-protocol-v1.md`，但 §6.4 明确"服务端无状态"，没有地方消费它）。于是 §7 状态机里的"下行 ACK / 重传 / 离线待拉取"三条边没有实现依托 —— 见 §9 的 C2。

---

## 2. 三种视角的可靠性保证（丢 / 重 / 乱 布尔组合）

先给判定用的两个前提，否则布尔值没有意义：

- **前提 P1（发送方）**：客户端在超时后**复用同一个 `clientMsgId`** 重发，且重发次数不设硬上限（或失败后进入本地待发箱继续重试）。基线 §7 给的是"累计 3 次 → 发送失败"，这会让结论退化，见 §8 F8。
- **前提 P2（服务端）**：MQ 未开启 `SYNC_FLUSH`/`SYNC_MASTER`（RocketMQ 默认异步刷盘，官方部署文档：*"asynchronous flush loses a small number of messages, synchronous flush does not lose a single message"*，[部署方式](https://rocketmq.apache.org/docs/4.x/deployment/01deploy/)）。

### 2.1 发送方视角

> 问题：我收到 `SEND_ACK(clientMsgId, serverMsgId, seq)` 之后，能认为什么？

| 维度 | 取值 | 依据 / 条件 |
|---|---|---|
| 可能丢 | **是** | ① broker 仅写 PageCache 时断电丢失已 ack 的消息（P2 + RocketMQ 官方 SEND_OK 说明）；② 落库消费重试耗尽转 DLQ 且无重放（§8 F2） |
| 可能重 | **否**（对"发送方自己"而言） | 同一 `clientMsgId` 在服务端被 `SETNX` + `uk_msg(conv_id, from_uid, client_msg_id)` 约束到单条记录（基线 §8.1/§8.2）；发送方拿到的 `seq` 唯一。**但**若客户端在"发送失败"后换新 `clientMsgId` 重发，则服务端无法去重，此时"重"由客户端制造（§8 F8） |
| 可能乱序 | **是** | `seq` 在取号时分配，多实例/多线程下取得顺序 ≠ 客户端业务顺序；同一会话内 `seq` 单调，但"谁先拿到号"由调度决定。客户端不能把 `seq` 当作"我按这个顺序发的"的凭证 |

**一句话语义**：`SEND_ACK` = 「这条消息在服务端获得了一个唯一身份，且最多存在一份；但它可能从未变得可读」。

### 2.2 接收方视角

> 问题：我拉到的消息，完整吗？重复吗？有序吗？

| 维度 | 取值 | 依据 / 条件 |
|---|---|---|
| 可能丢 | **是** | §8 的 F1（通知早于落库）、F2（DLQ 无重放）、F3（通知丢失且无重连补拉）、F4（多端共享已读水位）、F5（30 天冷热边界） |
| 可能重 | **是** | 同一 `seq` 可被多次 `NOTIFY`（合并窗口/广播模式重投）；分页重拉与补拉区间重叠；重连后的 catch-up 与在线推送重叠。**这是良性重复**，客户端按 `seq` 去重即可 |
| 可能乱序 | **否** | 单会话内 `seq` 严格单调（基线 §8.3），客户端按 `seq` 应用即有序。`seq` 有空洞不影响顺序判定 |

**一句话语义**：接收方能保证「拿到的一定有序且带唯一序号」，**但不能保证"拿到的就是全部"** —— 而"是否全部"正是本调研要补的东西。

### 2.3 服务端视角

> 问题：我"受理"了的消息，是不是一定可见？

| 维度 | 取值 | 依据 / 条件 |
|---|---|---|
| 可能丢 | **是** | 受理发生在 L2，可见发生在 L3；两者之间是 MQ 投递 + 落库，存在有限重试（RocketMQ 默认 16 次）后转 DLQ 的终态（[消费重试](https://rocketmq.apache.org/zh/docs/featureBehavior/10consumerretrypolicy/)） |
| 可能重 | **是** | MQ 至少一次投递；客户端重发；广播消费下每个实例都收到全量 |
| 可能乱序 | **是** | 消费并发与重投导致**落库顺序** ≠ `seq` 顺序。对 `WHERE conv_id=? AND seq BETWEEN ?` 的 range 查询无影响；但任何"按到达顺序推送/计数"的实现（如未读 counter 投影）会看到乱序 |

**一句话语义**：服务端能保证「受理即唯一、可去重」，**不能保证"受理即持久、即有序"**。要补的是：让"受理"与"可见"之间的缺口**可观测**（`committed_seq` + DLQ 重放），而不是假装缺口不存在。

### 2.4 端到端合成

| 链路 | 语义 | 对应业界名称 |
|---|---|---|
| 发送方 ↔ 服务端 | 至少一次 + 幂等 ⇒ **有效一次** | at-least-once + idempotent consumer（[Kafka Design](https://kafka.apache.org/44/design/design/)） |
| 服务端 ↔ 接收方 | **尽力一次**（notify）+ 拉取补齐 | 低于 at-most-once 的"零次或一次"，且无通知兜底 |
| 端到端 | **可能丢 / 可能重 / 单会话有序** | 不是 exactly-once，也不是 at-least-once |

基线 §10 表格里的 "投递丢失 → 三次重传后落'离线待拉取'，由拉取链路兜底" 这一行，在 §6.4 的实现前提下不成立（服务端无投递状态，没有"三次"可数）—— 详见 §9 C2。

---

## 3. 业界五种组合对照

| 维度 | Kafka | MQTT 5.0 | XMPP + XEP-0198 | Signal Server | 微信 sync（Web 协议） |
|---|---|---|---|---|---|
| 幂等键 | producer id + sequence（幂等生产者）；业务主键由消费端保证 | Packet Identifier（16 bit，1–65535） | stanza `id`（客户端自选） | **仅**服务端生成的 `serverGuid`；发送请求体里没有客户端幂等键（`IncomingMessage` 只有 type / destinationDeviceId / destinationRegistrationId / content） | `ClientMsgId`（时间戳左移 4 位 + 4 位随机）+ `LocalID` 同值 |
| ACK 层级 | broker ack（`acks=all` 才有"已提交"） | PUBACK / PUBREC+PUBCOMP（传输层，非业务层） | `<a h="N"/>` 流管理 + `<received/>` 回执 | gRPC stream 里 ack `server_guid` | 服务端返回新 `SyncKey` 即游标推进 |
| 重传窗口 | 生产者重试（`delivery.timeout.ms`）+ 消费者位置回退 | **仅**在 session resume（Clean Start=0）时重发未确认 PUBLISH | 未确认队列 + `max` 协商的恢复时间窗 | 在途窗口 `MAX_UNACKED_MESSAGES = 256` | 无服务端重传：客户端靠自己轮询 sync 补齐 |
| 缺口检测 | offset 连续（单分区严格连续） | 不需要：packet id 只在 ACK 前占用 | `h` 每 stanza **+1**，连续 → 缺一个就能发现 | 队列是唯一数据源，ack 即删 → 无"缺"的概念 | `SyncKey` 版本向量；连续拉直到 `ContinueFlag=0` |
| 离线形态 | 保留期内的日志（按时间/大小删除） | session state（含未确认消息），`Clean Start` 决定是否保留 | 服务端 unacked 队列；超窗后转**离线存储** | **每设备一条持久队列**，按字节裁剪 + TTL | 服务端保存变更，客户端按 SyncKey 补拉 |
| 关键代价 | 无 | 无 | 需要服务端保存 per-stream 状态 | 每设备一份队列（写放大 = 设备数） | 需要 per-device 变更日志（写放大 = 设备数） |

**读法**：这五种系统在"收件方"一侧走了两条完全不同的路——
- **有状态投递**（XMPP-SM / Signal / MQTT session）：服务端记"哪台设备确认到哪一条"，未确认就重发；代价是 per-device 状态。
- **无状态 + 游标**（Kafka / 微信 sync）：服务端只存事实，客户端拿着游标来拉；代价是**游标必须连续或可验证**，否则缺口不可见。

基线选了第二条路（§6.4、§6.6），但**没有把游标的连续性做实**：`seq` 允许空洞（§8.3），`read_sequence` 又是已读语义（§6.5）。这是本调研认为最需要修的地方。

---

## 4. 微信 sync 协议：分段拉取的确切形状

微信（Web 协议，逆向文档）的同步不是"推消息"，而是"客户端拿版本向量来拉变更"：

1. **登录后拿到 `SyncKey`**：一个 `{Count, List[{Key, Val}...]}` 的**多条目版本向量**，7 项量级。
2. **长轮询探测**：`GET /cgi-bin/mmwebwx-bin/synccheck` 带 `synckey=...`，返回 `window.synccheck={retcode, selector}`，`selector=2` 表示"有新消息"（0 正常 / 2 新消息 / 7 进入离开聊天界面）。
3. **分段拉取**：`POST /webwxsync` 带当前 `SyncKey`，返回：
   - 新的 `SyncKey`（游标前进）
   - **`ContinueFlag`**（是否还有更多，需立刻再调）
   - `AddMsgCount` / `AddMsgList`（**直接带消息内容**）
   - 以及非消息类变更：`ModContactList` / `DelContactList` / `ModChatRoomMemberList`
   
   字段与结构见 [WeixinBot 协议文档](https://github.com/Urinx/WeixinBot)（`同步刷新`、`消息接口` 两节）。
4. **游标只在成功后替换**：ItChat 的实现是每次 `webwxsync` 成功后把 `SyncKey` 覆盖为新值，并把向量拼成 `Key_Val|Key_Val|...` 作为下一次 `synccheck` 的参数（[ItChat `components/login.py`](https://github.com/littlecodersh/ItChat/blob/master/itchat/components/login.py)）。

**从这套协议能拿到的三条设计结论**：

- **优点一：一次 RPC 覆盖全部会话。** 登录补拉不是"对每个会话查一次"，而是一个全局变更流拉到 `ContinueFlag=0`。基线 §6.6 的"对每个会话取 read_sequence 之后的部分"在 500 个会话时就是 500 次查询（或一次大 IN 的 scatter），登录尖峰成本随会话数线性涨 `[推断]`。
- **优点二：`ContinueFlag` 把"分批"写成协议的一部分**，客户端不需要猜"是不是拉完了"。基线 §6.6 只定义了 `fromSeq&limit`，**没有 hasMore / nextSeq**，这是 §8 F6 的根因。
- **不可直接迁移：全局流需要 per-device 变更日志。** 微信能这么做，是因为服务端为每台设备维护同步状态（`SyncKey` 的每一项对应一个"集合"的版本）。对本项目就是"每条消息 × 每台设备"的写放大，与 §6.6 拒绝离线箱的理由同源。**结论：不引入全局变更流，改为 per-device cursor + 批量多会话拉取。**

**未取得原文**：`ContinueFlag` 的官方语义（微信未公开协议文档）。上面的解读只基于字段名、返回值结构与社区实现行为，属于 `[推断]`，不作为结论的依据强度使用。

---

## 5. 重传窗口与在途窗口：能查到的确切数字

"窗口"必须说清三件事：**窗口里存的是谁的状态、窗口满了怎么办、窗口过期后谁兜底**。业界数据如下：

| 场景 | 数值 | 状态存哪 | 满了 / 过期怎么办 | 来源 |
|---|---|---|---|---|
| 客户端上行重试（参考实现） | 阶梯 1 s / 3 s / 5 s | 客户端本地库 | 用户可见"发送失败" | [52im·消息不丢](http://www.52im.net/thread-4889-1-1.html)（引用尼恩分享，二手整理） |
| 客户端上行重试（基线 §7） | 最多 3 次 | 客户端 | **未定义**（§7 写"发送失败"，§10 写"不丢"，互相冲突） | 基线 §7 / §10 |
| MQ 客户端发送重试 | 默认 **3 次**，对应用不可见 | MQ 客户端内存 | 抛异常给业务；官方要求业务侧自己兜底 | [RocketMQ 参数约束](https://rocketmq.apache.org/zh/docs/introduction/03limits/) |
| MQ 消费重试 | 默认 **16 次**；阶梯 10 s、30 s、1 min…2 h | broker（重试 Topic） | 进 **DLQ**，不再重试 | [RocketMQ 消费重试](https://rocketmq.apache.org/zh/docs/featureBehavior/10consumerretrypolicy/) |
| 上一条的总时长 | **≈ 4.76 小时**（16 次间隔求和 17140 s） | — | 之后彻底停止 | 按上表数值求和 `[推算]` |
| 事务消息回查 | 间隔 **60 s**；半消息最大超时 **4 h**（超时强制回滚，不可配） | broker 半消息存储 | 强制回滚 = 消息丢弃 | [RocketMQ 参数约束](https://rocketmq.apache.org/zh/docs/introduction/03limits/) |
| SimpleConsumer 不可见时间 | 10 s ~ 12 h（必填） | broker | 超时=可重投 | 同上 |
| 一次拉取最大批次 | 默认 **32 条** | — | 需循环直到空 | 同上 |
| Signal 在途窗口 | **`MAX_UNACKED_MESSAGES = 256`** | 服务端（每设备流） | 用信号量限流，未 ack 不继续发 | [MessageDispatcher](https://github.com/signalapp/Signal-Server/blob/main/service/src/main/java/org/whispersystems/textsecuregcm/grpc/MessageDispatcher.java) |
| Signal 断开兜底 | 1 分钟后推推送唤醒（`CLOSE_WITH_PENDING_MESSAGES_NOTIFICATION_DELAY`） | 服务端 | 靠 push 让客户端回来 | 同上 |
| XMPP 恢复窗口 | 客户端 `enable@max` / 服务端 `enabled@max` 协商（秒） | 服务端 + 客户端 unacked 队列 | 超窗则状态可丢弃；**丢弃前应把未处理 stanza 转离线存储或返回错误** | [XEP-0198 §5](https://xmpp.org/extensions/xep-0198.html) |
| MQTT 重传时机 | **只有** Session Resume 时允许重发 | session state | 会话过期 = 丢弃 | [MQTT 5.0 §4.4](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html) |
| MQTT packet id | 1–65535，ACK 后即可复用 | 双方各一份 | 复用后旧 id 视为新消息 | [MQTT 5.0 §4.3.2](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html) |

**对本项目的映射**：
- 上行的"重传窗口"= 客户端本地待发箱 + `idem` TTL（基线 10 min）。窗口必须 **≥ 客户端最长重试时长**，否则 10 分钟后重发会被当成新消息（§8 F7）。
- 下行的"重传窗口"在基线里**不存在**（无 per-device 状态）。要么承认"零重传"并把 §7 的条款删掉，要么补状态 —— 见 §9 C2。
- MQ 消费的 4.76 小时是"最终可见"的最大延迟上界，**必须和产品 SLA 对齐**：如果产品容忍的是"几分钟内必达"，4.76 h 的静默重试等于丢。

---

## 6. 离线消息的存储形态：三种模型与算账

| 模型 | 代表 | 写放大（每条消息） | 读放大（登录时） | 缺口可见性 | 上限 | 适合 |
|---|---|---|---|---|---|---|
| A. **无离线箱**（时间线 + 游标扫描） | 基线 §6.6 | O(1) | O(会话数) 次查询 | **差**（`seq` 有空洞 → 见 §8 F2） | 只受拉取条数限制 | 大群、写多读少、会话数适中 |
| B. **每用户队列**（Redis zset / DB 队列） | 参考实现 `offline_msg:u:{uid}`；52im 六方案之一 | **O(成员数)** | O(用户) 一次 | 好（队列空=已拉完） | 队列长度/TTL | 小群、1:1、量大到撑不住扫描 |
| C. **每设备 ack 队列**（服务端持有内容直到 ack） | Signal | O(设备数) | O(设备) 一次，且带在途窗口 | 好（queue empty 语义明确） | 按字节裁剪 + TTL | 1:1 为主、设备数小、E2EE |

**基线的选择（A）是对的，理由可以用数字说清**：按基线自己的约束 §2.1（最大群 10000 人、群消息 ≤ 50 条/s），模型 B 需要 `10000 × 50 = 500000` 次 zset 写/秒，只为离线箱 `[推算]`。这还没有算上"成员数变化时要清理旧成员队列"的成本，也已经超出 §2.1 自己给的"群投递 ≤ 10 万投递/s"预算 5 倍。

**但模型 A 有一个必须补的洞**：A 的正确性完全依赖"游标 + 事实表的完整性"，而事实表的写入是异步的（MQ 消费），所以"游标推到哪里是安全的"必须由服务端回答。模型 C 用"服务端持有一份内容直到 ack"绕开了这个问题（代价是状态），A 没有这个奢侈 —— 所以 A 必须额外给出 `committed_seq`。

**52im 的六方案盘点**（MySQL 分表 / Redis / HBase / Cassandra / RocketMQ / 对象存储+元数据）本质是"把模型 B 的队列放在不同介质上"，其结论是"中小规模 Redis + 定时落盘；大规模 HBase/Cassandra + 混合冷热"（[52im·离线消息存储](http://www.52im.net/thread-4920-1-1.html)，二手综述，仅作全景参考）。**这篇的对比表主体是图片，文字部分是代码示例，因此不作为任何数值结论的依据。**

**Signal 的裁剪参数**（可借鉴的形状，不是可抄的数值）：`MessagesManager` 提供 `trimQueue(maxQueueSizeBytes, targetQueueSizeBytes, ...)` 与 `deleteMessagesBefore(cutoffTime)`，即**按字节裁剪 + 按时间过期**双保险（[MessagesManager](https://github.com/signalapp/Signal-Server/blob/main/service/src/main/java/org/whispersystems/textsecuregcm/storage/MessagesManager.java)）。**默认字节数未取得**（在动态配置里）。

---

## 7. 「落库失败但 MQ 已 ack」竞态

### 7.1 竞态的精确定义

基线 §6.2 的时序是：

```
MSG → MQ 同步发送 MessageAccepted →（拿到 SEND_OK）→ 返回 seq → GW → 客户端 SEND_ACK
MQ → TL 消费者 → 幂等落库 (conv_id, seq)
```

**竞态点**：`SEND_OK` 之后、TL 落库成功之前的任意时刻，一旦落库永久失败，客户端已经拿到 `SEND_ACK`，而消息永远不会出现在时间线上。具体有三种落法：

| 形态 | 触发 | 是否自愈 |
|---|---|---|
| a. 消费抛异常后被重试耗尽 | 代码 bug、payload 解析失败、`uk_server_msg` 全局索引冲突、分区 leader 长时间不可用 | **否** —— 16 次（≈4.76 h）后进 DLQ |
| b. 消费进程崩溃 | 宕机、发布、OOM | 是（broker 重投，未 ack 不丢） |
| c. 落库成功但读不到 | OB 弱读路由到 follower | 基线已禁（§8.2 读一致性红线）；这条坑已经踩过并写明 |

**所以真正的洞只有形态 a，且它的终态是 DLQ 而不是"重试成功"。** 基线 §10 的 "RocketMQ 自动重投" 描述的是 b，不是 a。

### 7.2 四种业界解法与适配性

| 解法 | 机制 | 适用性（针对本项目） | 出处 |
|---|---|---|---|
| **① 事务消息** | 生产者先发半消息（对消费者不可见）→ 执行本地事务 → `Commit`/`Rollback` → 未定则服务端按 60 s 间隔回查；半消息 4 h 未定强制回滚 | **不适用**。事务消息的价值在于"A 本地事务与 B 消息发送原子"；本场景 MSG 侧**没有本地事务可绑定**（落库发生在另一个进程的消费者里），用了只是把"消息发送"包了一层半消息 + 回查，收益为零、成本是两次 RPC 与半消息存储 | [事务消息](https://rocketmq.apache.org/zh/docs/featureBehavior/04transactionmessage/) |
| **② 本地消息表（outbox）+ relay** | 业务事务里同时写 outbox 行；relay（轮询或 log tailing）把 outbox 投给 MQ；官方明确指出 relay 可能重复发布，消费端必须幂等 | **价值有限**。outbox 存在的意义是"业务库与 MQ 不共享事务"。本项目的消息表**本身就是事实表**，直接同事务写它就够了，多一张 outbox 和一套 relay 只是把同一份数据写两遍 | [Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html) / [Transaction log tailing](https://microservices.io/patterns/data/transaction-log-tailing.html) |
| **③ 先落库后 ACK** | MSG 在同一事务里写 `message` + 更新 `conversation.last_seq`，成功后才回 `SEND_ACK`；MQ 退化为"通知扇出"通道 | **可行，且比 ② 简洁**。等于把 L2 升级为 L3。代价：每条消息在热路径多一次同步 DB 往返 | 社区一手实践明确主张"ACK 必须在落盘之后"，并把这当成"虚假确认"的分界线（[52im·消息不丢](http://www.52im.net/thread-4889-1-1.html)）；Signal 也是先写队列再让发送方认为成功 |
| **④ 保留早 ACK，但让缺口可验证** | ① `conversation.committed_seq`：TL 落库成功后才推进（单调），拉取接口返回；② 客户端游标**只推进到 `committed_seq`**；③ DLQ 消费者自动重放 + 告警；④ 超过 N 分钟仍未 committed 的 seq 触发补偿扫描 | **推荐**。热路径零额外写；把"承诺"从"会被处理"降级为"会被处理或会被观测到" | 缺口可检测是 XEP-0198 用连续计数器、Kafka 用连续 offset 的共同动机 |

### 7.3 推荐

**默认选 ④，关键通道可升级到 ③。**

- **④ 的落地形状**（三件小事，都不是新基建）：
  1. `conversation` 增加 `committed_seq`（或独立水位表），由 TL 消费者在**与消息插入同一事务**内推进 `max(old, seq)`；
  2. `GET /messages` 返回 `committedSeq`；客户端 `cursor = min(读到的最大 seq, committedSeq)`，**不得**用 `NOTIFY.maxSeq` 推进；
  3. 新增 DLQ 消费者：重放 + 落 `message_gap` 表 + 告警。**没有这一步，④ 只是把洞变得更可见，仍然漏。**
- **③ 的适用条件**：产品要求"发送方看到成功 ⇒ 一定可读"（比如支付/工单类消息），或某类会话的 SLA 严于 MQ 重试窗口。此时不要引 outbox，直接同步落库。
- **① 明确排除**；**② 只有在"消息需要落到与 OB 不同的库、且必须与业务写原子"时才重新评估**。

**成本对比（可核对的只有量级，不做虚假精度）**：③ 让每条消息在 §9 的"api→MQ 1–3 ms"之外再加一次同城 DB 往返，且把"批量写"变成"逐条写"（MQ 消费者原本可以批量提交）；④ 的成本是每次拉取多返回一个字段 + 一个低频消费者 `[推断]`。

---

## 8. 基线「推通知、拉内容」会漏消息的具体失败序列

以下每条都按 `触发序列 → 为什么漏 → 判定 → 修法` 写。判定分三档：**真丢**（服务端不会再补）/ **延迟无界**（最终能补，但触发条件不可控）/ **重复**。

### F1 通知早于落库（真丢 · 最严重）

```
1. A 发消息 → MSG 取号 seq=105 → MQ 发 MessageAccepted → 回 SEND_ACK
2. MQ 两个独立消费者：TL（落库）与 ST（投递意图）
3. ST 先消费成功 → 查 route → 向 B 推 NOTIFY{convId, maxSeq=105}
4. B 收到 NOTIFY → GET /messages?convId&fromSeq=read_seq+1
5. TL 尚未落库（消费者 lag / DB 抖动 / 重试中）→ 查询结果里没有 105
6. B 依据 NOTIFY.maxSeq 把本地游标推进到 105
7. TL 稍后落库成功，但**不会再有 NOTIFY**（没有新消息）
→ 105 对 B 永久不可见
```

- **为什么漏**：服务端把"通知"当成了"已可读"的凭证，而 §6.2 的两个消费者之间没有顺序保证。基线没有任何地方要求"TL 落库成功后才发 NOTIFY"。
- **判定**：真丢（除非有重连补拉或下一条消息把游标带回来）。
- **修法**：客户端游标只能推进到**实际读到的最大 seq**或服务端 `committedSeq`（§7.3 ④）；或把 NOTIFY 的发送移到"落库成功后"（牺牲 0–数百 ms 实时性）。

### F2 消费重试耗尽进 DLQ，且 seq 空洞掩盖了它（真丢）

```
1. 某条 seq=200 的消息在 TL 侧反复抛非幂等异常（如 payload 版本不兼容、uk_server_msg 冲突）
2. RocketMQ 按阶梯重试 16 次，约 4.76 h 后进入 %DLQ%<group>
3. 基线没有任何 DLQ 消费者/重放作业（§10 只写了"自动重投"）
4. 会话内从此存在空洞 200
5. B 拉 [190, 210] 拿到 190..199, 201..210，看不到"缺了 200"
→ 200 永久丢失，且**不可检测**
```

- **为什么漏**：① 重试有上限，DLQ 是终态；② 基线 §8.3 明确"`sequence` 允许空洞"，于是"丢一条"和"号段浪费"在客户端完全等价 —— 这是 §9 C4 的直接后果。
- **判定**：真丢 + 不可观测。
- **修法**：① DLQ 重放消费者（必须）；② 暴露 `committed_seq`，让"空洞在 committed 之上"与"空洞在 committed 之下"可区分（前者是潜在丢失，后者是号段浪费）；③ 补偿扫描：`committed_seq` 推进时若跳跃超过阈值（如 >1），落 `message_gap` 并告警。

### F3 NOTIFY 丢失 + 只在登录补拉（延迟无界）

```
1. B 在线，ST 把 NOTIFY 交给 B 所在网关的合并器（500 ms / 100 条窗口）
2. 网关进程崩溃 / 连接在写出后被 RST / 客户端 app 被系统杀死
3. NOTIFY 随内存/连接消失；基线 §6.4 明确服务端不保存投递状态，不会重发
4. B 重新建连 → §6.8 只规定"退避重连"，**没有规定"重连后补拉"**
5. B 的会话列表里该会话停在旧游标，直到：该会话来了下一条消息，或用户重开 App 触发登录补拉
→ 单聊/低活群可能静默停滞数天数小时
```

- **为什么漏**：补拉只有一个触发点（登录）。在线态的一次通知丢失没有任何兜底。
- **判定**：延迟无界（对用户等价于丢）。
- **修法**：① 重连成功后对"最近活跃 + 有未读"的会话做一次 catch-up（`cursor < committedSeq` 或 `cursor < lastSeq`）；② 或服务端在 LOGIN_ACK 里带 `conversation → lastSeq` 快照，客户端自行比对（一次查询，不用 per-device 状态）。

### F4 `read_sequence` 被当成投递游标（真丢 · 多端）

```
1. 用户 A 有手机 M 和 PC P（基线 §14.2 B2：同端互踢、异端共存 = 支持异端共存）
2. M 在线，把 msg 50..100 拉完并上报 READ → conversation_member.read_seq = 100
3. P 离线期间产生 50..100
4. P 登录 → §6.6 "离线 = 对每个会话取 read_sequence 之后的部分" → fromSeq = 101
5. 50..100 永远不会被 P 拉到（除非用户往上翻历史）
→ P 永久缺 50..100
```

- **为什么漏**：`read_sequence` 是**用户级已读**语义（§6.5：未读数 = `last_sequence − read_sequence`），跨端共享是正确的；但 §6.6 把它当成**设备级投递游标**用了。同一字段承担两种语义，必然在其中一种上错。
- **判定**：真丢（对设备而言）。
- **修法**：拆成两个量 —— `conversation_member.read_seq`（已读，跨端共享，用于未读数）+ `device_sync(conv_id, device_id, cursor)`（投递游标，per-device）。或干脆让客户端自带 `cursor` 参数（服务端不存），登录时由客户端提交"我本地到哪了"。后者零新表，但需要客户端可信。

### F5 30 天冷热边界（真丢）

```
1. P 离线 40 天，本地 cursor 停在 seq=1000
2. 期间 1001..9000 产生，且已被 §8.4 的归档流程 DROP PARTITION
3. P 登录 → GET /messages?fromSeq=1001 → 热表里 1001 已不存在 → 返回空
4. 客户端得到"没有新消息"的结论（因为接口没有 hasMore / archivedBeforeSeq）
→ 1001..9000 永久丢失
```

- **为什么漏**：§8.4 一期明确"在线冷读先靠延长热窗口顶着"，即**冷读未实现**；而 §6.6 的拉取契约没有表达"你要的东西已被归档"的手段。这不是性能问题，是正确性问题。
- **判定**：真丢。
- **修法**：拉取接口返回 `archivedBeforeSeq`（或 `hasGap: true`），客户端走"提示加载更早/重置到 latest"。Signal 的同类表达是 `deleteMessagesBefore(cutoffTime)` + `queue empty` 语义分离 —— 客户端能区分"队列空了"与"我要的没了"。

### F6 拉取契约缺 `hasMore` / `nextSeq`（真丢 · 取决于客户端实现）

```
1. 会话积压 5000 条，服务端 limit 取一个固定值（基线未规定，实现取 50/100 这类）
2. 客户端调用一次 GET /messages?fromSeq=1&limit=50，拿到 50 条
3. 契约里没有"还有更多"的信号 → 客户端把游标推进到 50 就停
→ 4950 条要等下一次 NOTIFY 或用户滚动才补
```

- **为什么漏**：§6.6 只定义了 `fromSeq&limit`，没有 `hasMore`/`nextSeq`/`committedSeq`。客户端只能靠"返回条数 < limit"猜，而这个猜测在"恰好整页"时多一次空请求，在"实现偷懒只拉一页"时直接漏。
- **判定**：真丢（若客户端只拉一页）/ 可恢复（若循环拉）。
- **修法**：协议里显式三件套 `{messages, nextSeq, hasMore}` + `committedSeq`。微信 sync 的 `ContinueFlag` 就是这个位置的东西（[WeixinBot](https://github.com/Urinx/WeixinBot)）。

### F7 `SETNX` 在取号之前，并发同 `clientMsgId` 行为未定义（可能重 / 可能丢）

```
1. §6.2 步骤顺序：幂等 SETNX(idem:{uid}:{clientMsgId}) → 取号段 → MQ 发送 → 返回 seq
2. 客户端超时（1 s）重发同一 clientMsgId，原请求仍在途
3. 第二个请求 SETNX 失败 → 基线未定义此时返回什么
   - 返回"处理中"的占位 → 客户端再次重试 → 可能重复取号
   - 返回错误 → 客户端把消息标记失败（丢）
   - 若实现是"SETNX 成功后写值"，而第一个请求在 SETNX 之后、取号之前崩溃 → 键存在但无 seq → 后续重发拿到空 seq
→ 要么重（两个 seq 指向同一逻辑消息），要么丢（客户端放弃）
```

- **为什么漏**：SETNX 与取值之间不是原子的，且"并发重复请求"的返回语义未定义。
- **判定**：未定义（实现相关）。
- **修法**：改成一次 Lua（`SET key value NX` 之后 `GET` 的一致性读）或"SETNX 抢占 + 短轮询等待首个结果（≤ 幂等 TTL）"；并在协议里定义 `DUPLICATE_IN_FLIGHT` 返回码，客户端见到它**保持重试**而不是判失败。

### F8 上行"累计 3 次 → 发送失败"后 UI 换新 `clientMsgId`（重复）

```
1. 客户端按 §7 重试 3 次均超时（服务端实际已受理并落库，只是 ACK 丢了）→ 标记"发送失败"
2. 用户在 UI 上点"重发" → 客户端生成**新的** clientMsgId
3. 服务端 uk_msg(conv_id, from_uid, client_msg_id) 认为是新消息 → 落第二条
→ 接收方看到两条一样的消息
```

- **为什么漏**：不是服务端去重失败，而是**客户端在"发送失败"时丢弃了幂等键**。业界一致做法是 `clientMsgId` 的生命周期一直持续到"确认送达/明确放弃"，重发复用同一个 id（MQTT 也是这个思路：同一个 Packet Identifier 在 ACK 前复用 = 同一条消息）。
- **判定**：重复（用户可见）。
- **修法**：`clientMsgId` 绑定到本地消息行，重发永不变；只有"用户主动删除/撤回"才终结。

### F9 合并窗口内的 NOTIFY 随进程崩溃消失（延迟无界）

```
1. §6.4：同一连接 500 ms 内 / 积攒 100 条 NOTIFY → 合并成一条下发
2. 合并缓冲在网关进程内存里
3. 进程崩溃 / 平滑重启（§6.9 的 SERVER_DRAIN 若未 drain 合并器）→ 缓冲丢失
4. 该网关上所有连接同时断开 → 客户端重连 → 回到 F3 的处境（无补拉规定）
→ 窗口内的通知全部消失
```

- **判定**：延迟无界（与 F3 同源）。
- **修法**：① §6.9 的 drain 流程必须包含"冲刷合并器"；② 重连补拉（同 F3）。

### 8.x 汇总

| 编号 | 序列 | 判定 | 修法归属 |
|---|---|---|---|
| F1 | 通知早于落库 | 真丢 | `committed_seq` |
| F2 | DLQ 无重放 + 空洞不可见 | 真丢 | DLQ 重放 + `committed_seq` |
| F3 | 通知丢失 + 无重连补拉 | 延迟无界 | 重连 catch-up |
| F4 | 已读水位当投递游标 | 真丢 | per-device cursor |
| F5 | 30 天冷热边界 | 真丢 | `archivedBeforeSeq` |
| F6 | 契约缺 hasMore | 真丢 / 可恢复 | 协议三件套 |
| F7 | SETNX 与取号非原子 | 未定义 | Lua / 在途语义 |
| F8 | 失败后换幂等键 | 重复 | `clientMsgId` 生命周期 |
| F9 | 合并窗口丢失 | 延迟无界 | drain + 重连补拉 |

---

## 9. 与基线的冲突（四段格式）

### C1 §6.2 / §10：把"MQ 同步发送"当成可靠性保证

**① 原方案（§6.2 + §10）**
§6.2："**ACK 早于落库**。ACK 语义是'服务端已受理'……可靠性由 RocketMQ 同步发送 + 消费端幂等保障，不靠同步写库。" §10 风险表："MQ 发送成功但落库失败 → RocketMQ 自动重投；消费端按 (conv_id, seq) 幂等"。

**② 新结论**
两条都不成立为"保证"：
- **"同步发送"不等于持久。** RocketMQ 官方对 `SEND_OK` 的原话是 *"successful message delivery does not mean it is reliable. To ensure that no messages are lost, you should also enable sync Master or sync flush"*，且默认刷盘方式是 `ASYNC_FLUSH`；`FLUSH_DISK_TIMEOUT` 的含义是"消息只进了服务端内存，机器宕机就丢"。官方部署文档对比更直接：*"asynchronous flush loses a small number of messages, synchronous flush does not lose a single message"*。
- **"自动重投"有界。** 消费重试默认 16 次（阶梯到 2 h，合计 ≈4.76 h）后进 DLQ，基线没有任何 DLQ 消费/重放路径。所以终态是"静默丢弃"，不是"最终一致"。

**③ 理由**
见来源：[Best Practices（SEND_OK 语义）](https://rocketmq.apache.org/docs/4.x/bestPractice/01bestpractice/)、[部署方式（刷盘与丢失）](https://rocketmq.apache.org/docs/4.x/deployment/01deploy/)、[消费重试（16 次 + DLQ）](https://rocketmq.apache.org/zh/docs/featureBehavior/10consumerretrypolicy/)、[参数约束（重试次数默认值）](https://rocketmq.apache.org/zh/docs/introduction/03limits/)。这三条合起来说明：基线 §6.2 的表述在**默认配置**下是"至少一次但可能丢 + 有限重试"，不能写成"可靠性由它保障"。

**④ 性能 / 成本差异**
- 开 `SYNC_FLUSH`/`SYNC_MASTER`：官方要求刷盘超时阈值 5 s，并明确"相比 ASYNC_FLUSH 有性能损失"；基线 §9 给"api → MQ 投递 1–3 ms"的预算，同步刷盘会把这条预算变成不可控（刷盘 RT + 主从同步 RT）。
- 不开：接受"broker 断电丢已 ack 消息"的窗口（官方：丢失"少量"消息，未给数字）。
- 补 DLQ 重放：一个低频消费者 + 一张 `message_gap` 表，稳态成本≈0，只在重试耗尽时触发。

### C2 §6.4 与 §7/§10 互相矛盾：下行到底有没有重传

**① 原方案**
§6.4："服务端只推 NOTIFY……重传归客户端拉取逻辑处理，**服务端无状态**"。
§7 状态机："`下行中 --对端未确认--> 下行重传 --> 接收成功`；`下行重传 --累计 3 次--> 下行失败 --> 离线待拉取`"。
§10："投递丢失 → 三次重传后落'离线待拉取'，由拉取链路兜底"。

**② 新结论**
两者不能同时实现。若按 §6.4 实现（无 per-device 投递状态），§7 的"对端 ACK / 下行重传 / 三次后失败"三条边**永远不会触发**，§10 的"投递丢失"对策是空条款。实际保证退化为"尽力写出一次"，等价于 at-most-once，而不是 §10 声称的"不丢"。

**③ 理由**
"重传"的前提是"知道哪条没被确认"。XEP-0198 为此要求双方各维护一个 unacked 队列和 `h` 计数器；Signal 为此维护每设备队列与 `MAX_UNACKED_MESSAGES = 256` 的在途窗口（未 ack 就不再发）。没有状态就没有重传，这是协议层面的必然，不是实现选择。而基线 §6.4 拒绝状态的动机是正当的（避免为每条消息维护定时器与缓存），但"不维护定时器"不等于"不维护游标"。

**④ 性能 / 成本差异**
- 补 per-device 状态：每设备每会话一个 cursor（`device_sync` 一行）或每设备一个 `last_notify_seq`（Redis 一个 key）。写放大 = 消息数 × 设备数，与 §6.6 拒绝离线箱的理由同源；万人大群不可接受。
- **建议方向相反**：删掉 §7 的下行重传条款，把 §10 那行改成"通知丢失由重连 catch-up + `committed_seq` 兜底"。代价：`conversation` 多一列、登录/重连多一次查询；收益：设计自洽，且 §11 的"投递失败率"指标才有定义（否则按 TCP write 计的成功率是假的）。

### C3 §6.6：用 `read_sequence` 当"离线补偿游标"

**① 原方案（§6.6 + §6.5）**
§6.6："离线 = 登录后对每个会话取 `read_sequence` 之后的部分，不需要写入时给每个成员写离线箱。"
§6.5："未读数 = `last_sequence − read_sequence`；群聊 `conversation_member.read_sequence` 上做聚合。"

**② 新结论**
`read_sequence` 是**用户级已读**语义，跨端共享是正确的；把它当**设备级投递游标**会在多端场景下真丢消息（§8 F4）。且它无法表达"消息已拉取但未读""消息在途未确认"这两态。

**③ 理由**
已读是人的行为（跨端一致），投递是设备的进度（必须独立）。基线 §14.2 B2 又明确支持"异端共存（PC + 手机同时在线）"，两者叠加就必然出现"手机读到 100，PC 永远看不到 50–100"。Signal 与 XEP-0198 都把游标放在设备/流粒度上，而不是用户粒度。

**④ 性能 / 成本差异**
- 新增 `device_sync(conv_id, device_id, cursor)`：每会话每设备一行，登录时一次批量查询取代"每会话一次查询"（§6.6 当前的 N 次）——**读放大反而下降**。
- 若不想存服务端：登录时让客户端提交自己的 cursor 列表（`{convId: cursor}`），服务端零新表，但多一个客户端可控输入，需要校验（cursor > lastSeq 时钳制）。

### C4 §8.3：`sequence` 允许空洞 ⇒ 缺口不可检测

**① 原方案（§8.3）**
"`sequence` 的语义是'先后'不是'连续'，空洞无副作用。单调性由'每个 convId 的号段单点递增'保证。"

**② 新结论**
空洞本身对"排序"确实无副作用，但它同时**摧毁了唯一的丢失检测手段**。`sequence` 是客户端唯一能看到的连续性锚点；一旦允许空洞，"丢了一条"（DLQ）与"号段浪费 999 个"（进程重启）在客户端不可区分（§8 F2）。

**③ 理由**
所有靠"游标 + 事实表"做补偿的系统，都要求游标连续或可验证：Kafka 单分区 offset 连续；XEP-0198 的 `h` 每收到一个 stanza **严格 +1**（规范原文："incremented to one for the first stanza handled and incremented by one again with each subsequent stanza handled"）；MQTT 用 `Packet Identifier` 做窗口内唯一。**不要**因此改成"连续分配"——那会让每次取号都要一次 Redis 往返，§8.3 的"1/1000 走 Redis"优势消失，10 万/s 直接打爆。正确做法是**在空洞之上再加一个单调水位**。

**④ 性能 / 成本差异**
- 连续分配（不推荐）：每消息一次 Redis RTT；按 §9 预算"取号段（本地命中）~0（1/1000 次走 Redis）"变成 100% 走 Redis，10 万/s 下 Redis 单键热点的吞吐成为新瓶颈。
- `committed_seq`（推荐）：`conversation` 多一列 BIGINT，消费者在同一事务里 `UPDATE ... SET committed_seq = GREATEST(committed_seq, ?)`——与消息插入同事务，无额外往返；拉取接口多返回一个字段。

### C5 §8.4：30 天归档与增量拉取的正确性冲突

**① 原方案（§8.4）**
"冷热边界：按时间，默认 30 天，可配"；"一期建议：DuckDB 跑离线与后台查询，在线冷读先靠'延长 OB 热窗口'顶着。"

**② 新结论**
这不是延迟问题，是正确性问题：离线超过 30 天的设备按 `fromSeq` 拉取会拿到空结果，客户端会判定"没有新消息"（§8 F5）。Signal 在同类问题上用 `deleteMessagesBefore(cutoff)` 显式裁掉并靠"queue empty"与"没有数据"的语义区分来避免歧义。

**③ 理由**
`MessageRepository.range(convId, fromSeq, ...)` 的契约没有"你要的范围已归档"这个返回值。接口形状定错了以后改不动（§8.4 自己的话），所以必须在接口里加。

**④ 性能 / 成本差异**
- 加 `archivedBeforeSeq`：零成本（一个分区边界查询，可缓存）。
- 若真要支持冷读：SLA 是 800 ms（§8.4 自定），且"禁止跨边界全局排序" —— 意味着只能"先冷后热两段拼接"，客户端要能接受两段延迟不同。

### C6 §7："累计 3 次 → 发送失败" 与 §10 的"不丢"冲突

**① 原方案**
§7："发送失败 ← 累计 3 次重传"；§10："投递丢失 → ……由拉取链路兜底"（隐含"不丢"）。

**② 新结论**
上行侧"3 次后失败"是**客户端主动放弃**，服务端无法兜底：消息可能从未到达服务端，拉取链路无从兜起。要兑现"不丢"，必须由客户端承担（本地待发箱 + 幂等键复用），而不是服务端。

**③ 理由**
这正是业界把可靠性放在**发送方**（本地库 + 阶梯重试 + 幂等键直到确认）的原因：服务端不知道"没发出来的消息"存在。参考实现的阶梯 1 s/3 s/5 s，"只有等到服务端明确说'我收到了'，才把这条消息从本地删掉"（[52im](http://www.52im.net/thread-4889-1-1.html)）。

**④ 性能 / 成本差异**
- 客户端：一个本地待发表 + 后台重试任务，零服务端成本。
- 服务端：需保证幂等键窗口 ≥ 客户端最长重试时长（基线 `idem` TTL 10 min，若客户端重试跨越 10 min 就会破防）。

---

## 10. 最小改造清单（按优先级）

| # | 改造 | 修的序列 | 成本 | 验收 |
|---|---|---|---|---|
| 1 | `conversation.committed_seq`（与消息插入同事务推进），拉取接口返回 | F1, F2 | 一列 + 一个字段 | e2e：注入 TL 落库延迟 5 s，客户端不推进游标；落库后自动可见 |
| 2 | per-device 游标（表或客户端提交）+ **重连后 catch-up** | F3, F4, F9 | 一张小表或一次批量查询 | e2e：双端登录，一端读完后另一端仍能拉到全部消息 |
| 3 | DLQ 重放消费者 + `message_gap` + 告警 | F2 | 一个低频消费者 | 故障演练：让 TL 持续抛异常，消息最终仍可见或被记录为 gap |
| 4 | 拉取契约补 `{nextSeq, hasMore, committedSeq, archivedBeforeSeq}` | F5, F6 | 协议字段 | 契约测试覆盖"恰好整页"与"跨归档边界" |
| 5 | 删掉 §7 的下行重传条款（或补 per-device 状态），并把 §10 对应行改写 | C2 | 文档 | 设计自审：§6.4/§7/§10 三处口径一致 |
| 6 | `clientMsgId` 生命周期定义（直到 L3 或用户放弃）；`SETNX` 改 Lua + `DUPLICATE_IN_FLIGHT` | F7, F8 | 客户端 + 一个返回码 | e2e：并发同 clientMsgId，只产生一条；ACK 丢失后重发不产生第二条 |

**明确不做**（有充分理由）：
- Redis 离线箱（写放大 = 群规模 × 消息速率，万人群 ≈ 5×10⁵ 次/秒写 `[推算]`）；
- 事务消息（无本地事务可绑定，见 §7.2 ①）；
- 端到端 exactly-once（业界在没有"消费端与存储在同一个事务域"时都做不到，Kafka 官方也只在 Kafka→Kafka 场景提供）。
- 全局变更流（微信 sync 形态）：需要 per-device 变更日志，写放大 = 设备数。

---

## 11. 未取得原文 / 存疑项（**不作为结论依据**）

| # | 项 | 状态 | 影响 |
|---|---|---|---|
| 1 | 微信 `ContinueFlag` 的官方语义 | **未取得原文**：微信未公开协议；只能从字段名、返回值结构与社区实现推断 | §4 对它只做 `[推断]`；结论不依赖它 |
| 2 | 微信客户端本地积压上限 / 最大补拉窗口 | 未取得（无公开文档） | 不引用 |
| 3 | Signal 队列裁剪的默认字节数（`maxQueueSizeBytes`） | 代码里是动态配置注入，**未取得默认值** | 只引用"按字节裁剪 + 按 TTL 过期"的形状 |
| 4 | Telegram `updates.getDifference` 的 `pts` 缺口检测 | **未取得原文**：`core.telegram.org` 在本机网络不可达（curl / 内置抓取均 40 s 超时） | 故不纳入结论；它本可作为"游标 + 缺口检测"的第三个实例 |
| 5 | 52im《消息不丢》一文的原始出处 | 该站注明"引用尼恩的技术分享，即时通讯网有修订和重新排版"，属**二手整理** | 只用于"ACK 应在落盘之后"这类与官方文档互证的实践主张，不作为唯一依据 |
| 6 | RocketMQ 异步刷盘"丢失少量消息"的具体比例 | 官方未给数字 | 不引用具体数字 |
| 7 | OB 单行写入 p99 | 未在本轮测得（约束：不跑压测） | §7.3 的成本对比只给量级，不给毫秒数 |

---

## 12. 来源清单

**协议 / 规范（一手）**
1. Apache Kafka — Design · Message Delivery Semantics：https://kafka.apache.org/44/design/design/
2. MQTT 5.0（OASIS Standard）§4.3 QoS 0/1/2、§4.4 Message delivery retry：https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html
3. XEP-0198: Stream Management（ack / `h` 计数器 / 恢复窗口 / unacked 队列）：https://xmpp.org/extensions/xep-0198.html
4. XEP-0184: Message Delivery Receipts（回执 ≠ 已读；可靠性特性属于另一层）：https://xmpp.org/extensions/xep-0184.html

**厂商官方文档（一手）**
5. RocketMQ · 事务消息（半消息 / 回查 / 超时回滚）：https://rocketmq.apache.org/zh/docs/featureBehavior/04transactionmessage/
6. RocketMQ · 消费重试（16 次阶梯、DLQ、状态机）：https://rocketmq.apache.org/zh/docs/featureBehavior/10consumerretrypolicy/
7. RocketMQ · 参数约束和建议（发送重试 3 / 消费重试 16 / 事务检查 60 s / 半消息 4 h / 拉取批次 32）：https://rocketmq.apache.org/zh/docs/introduction/03limits/
8. RocketMQ · Best Practices（`SEND_OK` 不等于可靠；需 SYNC_MASTER / SYNC_FLUSH）：https://rocketmq.apache.org/docs/4.x/bestPractice/01bestpractice/
9. RocketMQ · 部署方式（异步刷盘丢少量消息，同步刷盘不丢）：https://rocketmq.apache.org/docs/4.x/deployment/01deploy/

**开源服务端源码（一手）**
10. Signal-Server · `MessageDispatcher`（`MAX_UNACKED_MESSAGES=256`、ack 即删、queue empty 语义、断开 1 min 后推推送）：https://github.com/signalapp/Signal-Server/blob/main/service/src/main/java/org/whispersystems/textsecuregcm/grpc/MessageDispatcher.java
11. Signal-Server · `MessagesGrpcService`（流式拉取 + `server_guid_ack`）：https://github.com/signalapp/Signal-Server/blob/main/service/src/main/java/org/whispersystems/textsecuregcm/grpc/MessagesGrpcService.java
12. Signal-Server · `MessagesManager`（`trimQueue` 按字节裁剪、`deleteMessagesBefore` TTL）：https://github.com/signalapp/Signal-Server/blob/main/service/src/main/java/org/whispersystems/textsecuregcm/storage/MessagesManager.java
13. Signal-Server · `IncomingMessage`（发送请求体字段：type / destinationDeviceId / destinationRegistrationId / content，**无客户端幂等键**）：https://github.com/signalapp/Signal-Server/blob/main/service/src/main/java/org/whispersystems/textsecuregcm/entities/IncomingMessage.java
14. ItChat · `components/login.py`（每次 sync 成功后替换 `SyncKey`，版本向量拼成 `Key_Val|...`）：https://github.com/littlecodersh/ItChat/blob/master/itchat/components/login.py

**社区一手实现拆解**
15. WeixinBot 协议文档（`synccheck` / `webwxsync` / `SyncKey` / `ContinueFlag` / `ClientMsgId`）：https://github.com/Urinx/WeixinBot
16. 52im《如何保障分布式 IM 聊天系统的消息可靠性（即消息不丢）》（ACK 在落盘之后；客户端阶梯重试 1/3/5 s；`SETNX` 去重）——二手整理：http://www.52im.net/thread-4889-1-1.html

**模式文献 / 二手综述**
17. microservices.io · Transactional outbox：https://microservices.io/patterns/data/transactional-outbox.html
18. microservices.io · Transaction log tailing：https://microservices.io/patterns/data/transaction-log-tailing.html
19. 52im《IM 离线消息该怎么存？全面盘点主流存储方案》——二手综述（对比表主体为图片）：http://www.52im.net/thread-4920-1-1.html

**本仓基线**
20. `docs/architecture/im-v2-architecture.md` @ `rewrite/v2@0acbc9e`：§2.1 指标、§2.4 大群妥协、§6.2 上行、§6.4 下行、§6.5 已读、§6.6 离线与历史、§6.8 重连、§7 状态机、§8.1–8.4 存储、§9 性能预算、§10 可靠性、§11 可观测性
21. `docs/protocol/wire-protocol-v1.md`（`ACK 1046` / `P2P_MESSAGE 1103` / `message_id` 与 `message_key` 分离）

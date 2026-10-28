# baize ↔ IM 通道契约研究

> 路径前缀：`[B]` = `/Users/zz/AiProjects/baize`，`[I]` = `/Users/zz/AiProjects/im-whale-shark`。行号取读取当时版本（baize 文档 mtime 2026-10-28；IM 仓工作区）。
> 本文件只回答一件事：**要把 baize 的智能客服挂到 im-whale-shark 的 P2P 单聊上，IM 侧必须提供什么**，以及现行契约定到哪一步、还差哪几刀。

---

## 0. 结论摘要

1. **契约定在「方法级」，没定在「实现级」**。baize 侧 `ImInboundService.onMessage` / `ImOutboundService.send` 两个方法、字段、幂等键、超时都是写死的（`[B] .scratch/intelligent-cs-agent/issues/19-java-stack-im-adaptation.md:74-116`），载体（SCF / Feign）刻意留空。本期 IM 通道推迟，契约仅留档（`[B] …/spec.md:32`、`[B] …/tech-design.md:1277`）。
2. **「bot 直发无需好友关系」只对投递成立，对双向会话不成立**。`send()` 确实不校验关系链，但入站 TCP 链路强制 `checkFriendShip`（`[I] im-infrastructure/.../strategy/command/message/P2PMsgCommand.java:41-52` → `[I] im-domain/.../service/check/CheckSendMessageImpl.java:68-110`），且 P2P 会话列表只在「加好友成功」时建档（`[I] im-domain/.../friendship/service/impl/ImFriendServiceImpl.java:286-291`）。所以 bot 账号必须先是一条合法的关系链。
3. **现役 `send()` 丢三样 baize 契约依赖的东西**：`extra`（请求体里根本没有这个字段）、`messageSequence`（只由 `processor()` 分配）、离线消息（不写离线队列）。三项都在本文 §7 缺口表里（G3/G4/G8）。
4. **`im_message_body.message_body` 是 `VARCHAR(5000)`，`extra` 是 `VARCHAR(1000)`**（`[I] docker/mysql/db/im_core.sql:163,167`），而落库异常被 `catch` 吞掉只打日志（`[I] im-message-store/.../service/StoreMessageService.java:49-61`）——AI 单条成文超长 = 静默丢消息，不是报错。
5. **兜底链路（`pullSince`）现役不可用**：写离线队列的 Redis key 与读的 key 拼法不一致，且水位用 `messageKey` 而不是 `messageSequence`（§7.3 给出证据）。
6. **IM 侧承载 blocks 的唯一现实方式是「信封塞进 `messageBody` + 全部可降级为纯文本」**：IM 现役没有任何卡片/按钮渲染与 action 回调通道，`fallback_text` 不是锦上添花而是主通道。

---

## 1. 证据基线

### 1.1 baize 侧：文档状态与冲突

| 文档 | 关于 IM 的断言 | 定位 |
|---|---|---|
| `spec.md` | 本期不做 IM；P2P 单聊原计划进 MVP，现推迟；方法级契约留档票 19 | `[B] .scratch/intelligent-cs-agent/spec.md:28-35`（§3 产品形态，IM 段在 :32） |
| `tech-proposal.md` | 传输无关方法级契约 + 5 行维度表（幂等/超时/富消息/流式/丢消息兜底） | `[B] …/tech-proposal.md:94-116`（§7.3） |
| `issues/19` | 已核实 IM 事实 9 行 + 入站契约 + 出站契约 + 身份接管 | `[B] …/issues/19-java-stack-im-adaptation.md:15-26`、`:74-95`、`:97-116`、`:128-134` |
| `tech-design.md` | §3.4 明确**本期不建** `channel`/`external_*`/`inbound_event`；§16 1c 登记「删除 IM 契约」 | `[B] …/tech-design.md:400-412`、`:1277` |
| 白皮书 05 | 接入层仍是双通道（Web + IM 适配器），语义上层统一 | `[B] …/whitepaper/05-system-design.xml:33`、`:102` |
| 白皮书 10.8 | IM 渠道成本在入站语义；只接自有 IM；单条成文 | `[B] …/whitepaper/10-web-protocol.xml:87-93` |
| 白皮书 10.9 | 富媒体选型：**自有 JSON blocks + 强制 `fallback_text`，不发 HTML**；代价是自维护 `schema_version` 与未知类型降级 | `[B] …/whitepaper/10-web-protocol.xml:105` |
| 白皮书 10.12 | 单一消息模型 `{id, role, parts[], status, created_at, server_seq}`，Web/坐席台/IM 共用；差异只在渲染器层 | `[B] …/whitepaper/10-web-protocol.xml:184-190` |
| `GLOSSARY.md` | `channel ∈ {web, im}`，渠道只决定呈现，不改会话语义 | `[B] GLOSSARY.md`（「渠道」条目） |

**已登记的文档冲突（不改，只标注）**：

| # | 冲突 | 位置 |
|---|---|---|
| C1 | `tech-design` §16 1c 说「删除 IM 契约」，票 19 说「契约留档保留」。实际状态是「契约存档、实现删除」 | `[B] …/tech-design.md:1277` vs `[B] …/issues/19…md:158` |
| C2 | 白皮书 05/10 仍按双通道写（含 IM 适配器与回调），`spec.md` 已改「本期不做」 | `[B] …/whitepaper/05-system-design.xml:102` vs `[B] …/spec.md:32` |
| C3 | 票 19 说 IM「无 channel/bot 概念」，实际 DDL 有 `im_user_data.user_type`（1 普通 / 2 客服 / 3 机器人），只是全仓无代码读它 | `[B] …/issues/19…md:26` vs `[I] docker/mysql/db/im_core.sql:200`、`[I] im-domain/.../user/dao/ImUserDataEntity.java:51` |
| C4 | 票 19 说出站幂等「先查 messageId 缓存、再靠唯一键防重」；实际缓存查重只在 `processor()`（入站），`send()` 无查重，且 DB 主键是雪花 `message_key`，表里根本没有 `message_id` 列 | `[B] …/issues/19…md:23` vs `[I] im-domain/.../message/service/P2PMessageService.java:148-167`、`[I] docker/mysql/db/im_core.sql:158-183` |
| C5 | blocks 字段命名不一致：`citations.items` 用 snake（`chunk_id`/`doc_id`/`docName`），card 用 camel（`kind`/`title`/`fields`） | `[B] …/tech-design.md:386-397` |

### 1.2 IM 侧：已核实事实

| 项 | 事实 | 证据 |
|---|---|---|
| bot 账号位 | `im_user_data.user_type` 1 普通 / 2 客服 / 3 机器人 | `[I] docker/mysql/db/im_core.sql:200` |
| bot 建档通道 | `POST v1/user/importUser` 透传整个 `ImUserDataEntity`（含 `userType`），一次 ≤100 条 | `[I] im-domain/.../user/service/impl/ImUserServiceImpl.java:59-88`、`[I] .../req/ImportUserReq.java` |
| user_type 使用方 | **零处**（仅实体字段，无一处读取） | grep `user_type|userType` 全仓仅命中实体定义 |
| 出站 send | `POST v1/message/send` → `SendMessageResp{messageId, messageTime}`；请求体无 `extra` | `[I] im-domain/.../message/controller/MessageController.java:40-46`、`[I] .../req/SendMessageReq.java:11-36` |
| send 路径行为 | 只做 4 件事：`storeP2PMessage`（投 MQ 异步落库）、`syncToSender`、`dispatchMessage`、返回；**不校验、不分配 seq、不写离线、不 ack、不查重** | `[I] im-domain/.../service/P2PMessageService.java:148-167` |
| 投递失败静默 | `sendToUserAllClient` 对离线用户返回空列表，`send()` 不看返回值 → 离线即丢弃 | `[I] im-infrastructure/.../sendmsg/MessageProducer.java:49-61` |
| 落库失败静默 | `doStoreP2PMessage` 整体 `try/catch` 只 `log.error` | `[I] im-message-store/.../service/StoreMessageService.java:49-61` |
| 入站链路 | 客户端 TCP 包 → `checkP2PSendMessage` Feign → MQ → `P2PChatOperateReceiver.doStrategy` → `processor()` | `[I] im-infrastructure/.../strategy/command/message/P2PMsgCommand.java:28-56`、`[I] im-domain/.../message/mq/P2PChatOperateReceiver.java:54-63` |
| 入站幂等 | 仅 `processor()` 内基于 Redis `appId:cacheMessage:messageId`（TTL 5 分钟）判重，过期后按新消息处理 | `[I] im-domain/.../service/P2PMessageService.java:66-83`、`[I] .../store/MessageStoreServiceImpl.java:60-107` |
| 回调通道 | `CallbackService.afterCallback` 单地址同步 HTTP，异常吞掉不重试；`SendMessageAfter` / `SendMessageBefore` 常量已定义、**全仓零引用** | `[I] im-infrastructure/.../service/callback/CallbackServiceImpl.java:48-60`、`[I] im-common/.../constant/Constants.java:133-135`、`[I] im-infrastructure/.../config/AppConfig.java:52-54` |
| 消息撤回 | `MSG_RECALL` / `MSG_RECALL_NOTIFY` / `MSG_RECALL_ACK` 三个命令常量已占位，**无实现** | `[I] im-common/.../enums/command/MessageCommand.java:27-34` |
| 存储上限 | `message_body VARCHAR(5000)`、`extra VARCHAR(1000)`；主键 `message_key`（雪花），无 `message_id` 列 | `[I] docker/mysql/db/im_core.sql:158-183` |
| 关系链配置 | `appconfig.sendMessageCheckFriend: true`（两份 yml 都是 true） | `[I] im-domain/src/main/resources/application.yml:63`、`application-docker.yml:53` |
| 离线队列 | ZSet，key 按用户，score = `messageKey`，上限 `offlineMessageCount: 1024`，超限删最旧一条 | `[I] im-domain/.../store/MessageStoreServiceImpl.java:124-142`、`[I] application.yml:80` |

---

## 2. 账号形态：bot 在 IM 里怎么存在

### 2.1 现状：位置有，语义没有

- 有 `user_type`（3 = 机器人），但无任何分支读取 → 今天把 `userType=3` 插进去，系统行为与普通用户完全相同（`[I] docker/mysql/db/im_core.sql:200`、`ImUserDataEntity.java:51`）。
- bot 不需要登录（登录是为了拿 TCP 连接收消息）；`send()` 也不校验 `fromId` 是否在线（`[I] P2PMessageService.java:148-167`）。
- 但**用户侧要能看见并给 bot 发消息**，需要三件事同时成立，缺一不可：

| # | 条件 | 现役实现位置 | bot 场景的问题 |
|---|---|---|---|
| A1 | `im_user_data` 有该 bot 行 | `importUser` | 可行，但 `userType` 无行为差异 |
| A2 | 双方 `im_friendship` 关系链存在（`from→to` 与 `to→from` 两行） | `checkFriendShip` 双查 + 黑名单 | bot 不能自己发起加好友；要么人工为用户批量建关系，要么给 bot 豁免 |
| A3 | `im_conversation_set` 有 P2P 会话行 | 只在 `addFriend` 成功时创建（`ImFriendServiceImpl.java:286-291`） | 没走加好友链路 → 客户端会话列表里没有这个会话 |

结论：**「bot 直发无需好友」是投递层的事实，不是会话层的事实**。baize 票 19 的第 22 行结论需要按此修正。

### 2.2 三种账号形态与代价

| 方案 | 做法 | 代价 | 判定 |
|---|---|---|---|
| A：普通用户账号 + 预置好友 | bot 当普通用户，上线前为用户批量导入双向关系 | 用户量 N → 2N 行关系记录；新用户注册需同步建关系；与其他业务好友列表混在一起 | 可用但脏 |
| B：`user_type=3` 特权账号 | 落地 `user_type` 语义：免好友校验、免会话建档（或自动建档）、不受禁言/黑名单限制 | 需要 IM 改 `checkFriendShip` 与 `createConversation` 的调用点；要在 3 处判类型 | **推荐**，改动小、语义干净 |
| C：独立通道（不走 P2P） | IM 新增「服务号 / bot 通道」 | 第二套入站语义 + 客户端改造 | 超范围，否决 |

B 的最小实现只需一个判定点：`CheckSendMessageImpl.checkFriendShip` 命中 `fromId` 或 `toId` 的 `user_type=3` 时短路返回成功；会话建档在 bot 首次投递时补建。**注意：短路投递会同时放过「用户给 bot 发消息」与「bot 给任意用户发消息」，后者属于越权风险，必须限定 bot 只能给自己会话的用户发（白名单 = baize 侧 `session.user_ref` 的存在性）。**

### 2.3 baize 侧已定的身份字段

| 概念 | baize 定义 | 证据 |
|---|---|---|
| 会话主键 | `session = (channel, app_id, external_user_id)` | `[B] …/tech-proposal.md:116`、`[B] …/issues/19…md:130` |
| 出站 fromId | 白泽 bot 账号 | `[B] …/issues/19…md:114` |
| 出站 toId | 用户 `external_user_id` | 同上 |
| 对齐键 | `message.external_message_id` / `external_seq`（本期不建） | `[B] …/tech-design.md:405-406` |
| 渠道常量 | `web` / `im`（本期不建列） | `[B] GLOSSARY.md`、`[B] …/tech-design.md:98` |

→ IM 侧需要回答的只有一个**未定项**：`appId` 与 bot 是一对一还是一对多。baize 契约假设 `appId` 是应用维度常量、bot 由 `fromId` 标识；若 IM 后续按 appId 隔离多 bot，baize 的 `ImSendCommand.appId` 语义要跟 `fromId` 绑定成配置项。

---

## 3. 消息体结构（blocks，字段级草案）

### 3.1 baize 侧现有定义与缺口

baize 的 blocks 只在 Web 通道里被完整定义（`[B] …/tech-design.md:384-398`），三条硬约束「单条 ≤20 blocks、单卡 ≤4 actions、必须有可读 `text`」（同节 + `[B] …/spec.md:69-74`）。IM 通道额外需要白皮书 10.9 的两条（`fallback_text`、`schema_version`），但 baize 现文档**没有**把它们写进 blocks 结构，只在选型表里提了一句代价。

### 3.2 传输信封（`messageBody` 内容）

`messageBody` 是 `String`，可放 JSON（`[I] im-common/.../model/message/content/MessageBody.java`、`[B] …/issues/19…md:25`）。信封草案：

```json
{
  "schema_version": 1,
  "kind": "ai_reply",
  "fallback_text": "你的订单 88231045 已于 10-27 由顺丰揽收，预计 10-30 送达。",
  "blocks": [ ... ],
  "meta": {
    "session_id": "uuid",
    "turn_id": "uuid",
    "message_id": "uuid",
    "trace_id": "hex32",
    "sender_role": "ai",
    "operator": null,
    "server_seq": 12,
    "part_index": 1,
    "part_total": 1,
    "handoff_reason": null,
    "handoff_trigger": null
  }
}
```

| 字段 | 必填 | 说明 | 依据 / 理由 |
|---|---|---|---|
| `schema_version` | 是 | 整数，从 1 起；未知版本降级为 `fallback_text` | 白皮书 10.9 富媒体行的代价列（`[B] …/10-web-protocol.xml:105`） |
| `kind` | 是 | `ai_reply` / `slot_ask` / `handoff_notice` / `agent_reply` / `system_notice` | baize 一轮可能产出多条消息（见 §6.2），IM 侧据此决定渲染与前缀 |
| `fallback_text` | 是 | 纯文本 ≤1000 字符，语义完整、可独立发送 | 白皮书 10.9；`extra` 上限 1000 字符同量级（`[I] im_core.sql:167`） |
| `blocks` | 否 | ≤20，见 §3.3 | `[B] …/tech-design.md:398` |
| `meta.trace_id` | 是 | baize 的 trace 主键；原契约放 `extra`（`[B] …/issues/19…md:114`），但 `extra` 现役传不过去（§7.2 G3），故**先落在信封里** | `[B] …/tech-design.md:1179` |
| `meta.sender_role` | 是 | `ai` / `human`；决定 IM 客户端前缀「人工客服 · 张三：」 | `[B] …/issues/19…md:131-132` |
| `meta.part_index/total` | 否 | 单条成文被长度上限切分时使用（§6.3） | 本文推导 |

**`meta` 为什么不能只靠 `extra`**：`SendMessageReq` 没有 `extra` 字段（`[I] .../req/SendMessageReq.java:11-36`），Spring Boot 默认 `FAIL_ON_UNKNOWN_PROPERTIES=false`，提交 `extra` 会被**静默丢弃**（推断，仓库内无 ObjectMapper 自定义，`[I] im-infrastructure/.../config/RedisConfig.java:35` 只改 Redis 序列化器）。因此 IM 侧不补 `extra` 之前，`meta` 必须内嵌在 `messageBody` 里。

### 3.3 block 类型与字段

| type | 字段 | 约束 | 来源 |
|---|---|---|---|
| `text` | `text: string` | 单条 ≤800 字符；不引入 Markdown 方言，仅换行 | `[B] …/tech-design.md:1126` |
| `citations` | `items: [{chunk_id, doc_id, doc_name, snippet, score, rank}]` | ≤3 条；`snippet` ≤120 字符 | `[B] …/tech-design.md:386-392`、`[B] …/spec.md:71` |
| `card` | `kind`、`title`、`fields[{label,value}]`、`actions[{id,label,style?,value?}]`、`deep_link?` | `kind ∈ {order, logistics, refund, qc, handoff, slot}`；`fields` ≤8；`actions` ≤4 | `[B] …/tech-design.md:392-397`、`[B] …/spec.md:74` |
| `notice` | `level ∈ {info, warn}`、`text` | 用于兜底话术与限流提示，纯展示无交互 | 本文推导（对应 `error`/`status` 事件在 IM 无落点） |

字段级示例（与 `[B] …/tech-design.md:386-397` 对齐，命名统一为 snake_case —— 修正 C5）：

```json
{"blocks": [
  {"type": "text", "text": "你的订单已由顺丰揽收，预计 10-30 送达。"},
  {"type": "citations", "items": [
    {"chunk_id": "c_018", "doc_id": "d_07", "doc_name": "发货时效说明",
     "snippet": "现货商品在付款后 48 小时内出库", "score": 0.82, "rank": 1}
  ]},
  {"type": "card", "kind": "logistics", "title": "物流轨迹",
   "fields": [{"label": "最新节点", "value": "派送中"}, {"label": "运单号", "value": "SF1234567890"}],
   "actions": [{"id": "handoff", "label": "转人工", "value": "人工"}]},
  {"type": "card", "kind": "handoff", "title": "需要人工确认",
   "fields": [{"label": "原因", "value": "赔付金额需人工核定"}],
   "actions": [{"id": "handoff", "label": "转接人工", "value": "人工"}]}
]}
```

### 3.4 IM 渲染能力与降级映射

IM 现役没有任何卡片的传输/渲染层（`messageBody` 是不透明字符串，客户端私有协议只透传 `data`，`[I] im-codec/.../proto/MessagePack.java`），也没有 `action` 回调通道。因此**每个 block 都必须有确定的纯文本降级**：

| block | IM 能否原生渲染 | 降级形态 | 备注 |
|---|---|---|---|
| `text` | 是（气泡） | — | 直接进 `messageBody` 亦可，但为统一仍放 blocks |
| `citations` | 否 | 正文后追加「依据：①《发货时效说明》现货商品在付款后 48 小时内出库…」 | 不展示 `score`/`rank`，避免用户误解 |
| `card(kind=order/logistics/refund/qc)` | 否 | 多行 `label：value` 列表，标题加「【标题】」 | 字段 ≤8 保证不刷屏 |
| `card(kind=handoff)` | 否 | 「这个问题需要人工帮你确认。回复「人工」可转接客服。」 | 对应 `[B] …/spec.md:62-65` 的用户可见话术 |
| `card(actions)` | 否 | 全部转为**文本指令**，见下表 | 用户按键变成用户打字 |
| `notice` | 否 | 一句纯文本 | — |

**action → 文本指令映射表**（IM 通道必配，否则按钮不可达）：

| action.id | 用户可发送的文本 | baize 侧接收路径 |
|---|---|---|
| `handoff` | 「人工」 | 规则闸显式转人工关键词（`[B] …/tech-design.md:869`） |
| `retry` | 「再试一次」 | 新一轮同文本入站，状态机按新轮次处理（需补同义关键词） |
| `confirm_slot` | 用户直接回填槽位值 | 槽位提取（`[B] …/tech-design.md:687`） |
| `end` | 「结束」 | 会话结束意图（baize 现无对应规则，**缺口**） |

---

## 4. 转人工信号：谁写、写在哪、用户看到什么

### 4.1 写入方与落点（baize 单方所有）

`handoff_reason` **只由 baize 写**，模型自述不算触发（`[B] …/spec.md:67`、`[B] …/tech-design.md:700`）。三个字段独立落库：

| 字段 | 类型 | 取值 | 位置 |
|---|---|---|---|
| `session.handed_off_at` | timestamptz | 触发时刻 | `[B] …/tech-design.md:210-212` |
| `session.handoff_reason` | text | `policy` / `tool_unavailable` / `data_conflict` / `user_request` / `repeat_unresolved` | 同上 |
| `session.handoff_trigger` | text | `rule` / `user` | 同上 |
| `biz_tickets` | 行 | `reason_code`、`summary`、`queue`、`eta_minutes`、`idempotency_key`（唯一） | `[B] …/tech-design.md:381-383` |

### 4.2 信号表

| 判据 | 信号来源 | `reason` | `trigger` | Web 用户看到 | IM 用户看到 | IM 侧是否参与 |
|---|---|---|---|---|---|---|
| 政策外 / 需裁量（赔付、价保争议、投诉定责） | 规则黑名单 + 风险词 | `policy` | `rule` | `handoff.required` 事件 + 转人工卡 | `kind=handoff_notice` 一条 + 「回复「人工」」 | 否 |
| 工具查不到 / 数据矛盾 / 工具失败 ≥2 | `ToolResult` 结构化 error / 矛盾检测 | `tool_unavailable` / `data_conflict` | `rule` | 同上 | 同上 | 否 |
| 用户明确要人 | 关键词 | `user_request` | `user` | 同上 | 同上 | 否 |
| 同意图连续两轮未解决 | 状态机计数 | `repeat_unresolved` | `rule` | 同上 | 同上 | 否 |
| 检索最高分 < 0.35 | 检索阈值 | （拒答，非转人工；带转人工入口） | — | 兜底话术 + 转人工按钮 | 兜底文本 + 「回复「人工」」 | 否 |
| 模型自述「我解决不了」 | `OutputGuard` 改写 | **不写** `handoff_*` | — | 兜底话术 + 按钮 | 兜底文本 | 否 |

依据：`[B] …/tech-design.md:690-701`（触发矩阵）、`:869-875`（规则闸）、`[B] …/spec.md:58-67`。

**关键结论**：IM 侧对转人工**零参与**——不写信号、不落库、不路由；它只需要把「转人工」这条消息送达，并让用户能靠回文本「人工」二次触发。这与「IM 承担投递、baize 承担语义」的分层一致（`[B] …/issues/19…md:68-70`）。

### 4.3 一个必须显式化的字段

baize 要求「界面上必须显示 `handoff_reason` 与触发者，否则复盘时分不清模型偷懒与规则命中」（`[B] …/spec.md:67`）。Web 由坐席台/大盘消费；IM 通道下这个要求**只能落在 `meta.handoff_reason`**（用户不可见，供 IM 侧日志与 baize 对账），因此信封里 `meta.handoff_reason / handoff_trigger` 不是可选装饰。

---

## 5. 坐席接管后的消息回流路径

### 5.1 现状路径（Web）

```
用户(IM) --TCP--> im-tcp --MQ--> im-domain --onMessage--> baize
                                                              │ status=human_takeover 时只落库不生成
坐席(baize /agent) --HTTP /reply--> baize --send(fromId=bot, sender_role=human)--> im-domain --> 用户(IM)
```

| 步骤 | 谁做 | 触发条件 | 幂等键 | 证据 |
|---|---|---|---|---|
| 接管 | 坐席点「接管会话」 | `pending_human → human_takeover`，若有活跃流先 `stop` | 状态条件更新 | `[B] …/tech-design.md:715-719` |
| 用户续聊 | IM → `onMessage` | 命中「已接管」规则 → 只落库不生成 | `appId + messageId` | `[B] …/tech-design.md:873` |
| 坐席回复 | baize → `send` | `kind=agent_reply`、`meta.sender_role=human` | `messageId` | `[B] …/issues/19…md:131`、`:134` |
| 结束 | 坐席「结束」 | `human_takeover → ended`，AI 不再自动发言 | 状态条件更新 | `[B] …/tech-design.md:436` |

### 5.2 IM 侧必须保证的四件事

| # | 能力 | 现役是否具备 | 说明 |
|---|---|---|---|
| R1 | 任意 `fromId` 向任意 `toId` 投递单聊消息 | ✅ | `send()` 不校验关系链（`[I] P2PMessageService.java:148-167`） |
| R2 | 同一 `fromId` 连续发多条、每条独立 `messageId` | ✅ | 无每会话速率限制 |
| R3 | 用户离线时坐席回复不丢 | ❌ | `send()` 不写离线队列（§7.2 G8） |
| R4 | 坐席消息在用户侧**可区分于 AI** | ⚠️ 仅靠文本前缀 | IM 无发送者身份字段可用（`extra` 传不过去），前缀由 `fallback_text` 承载，属体验折衷（`[B] …/issues/19…md:132`） |

### 5.3 已知风险（baize 已登记 + IM 侧新增）

| 风险 | 缓解 | 状态 |
|---|---|---|
| 接管瞬间 AI 正在生成 → 双写 | 单活跃流 + 接管时先 `stop` | baize 已解（`[B] …/tech-design.md:719`） |
| 坐席发错无法撤回 | IM 有 `MSG_RECALL` 常量但无实现 | **未实现**（`[I] im-common/.../MessageCommand.java:27-34`） |
| 用户不知道「人工「谁」」 | 文本前缀「人工客服 · 张三：」 | 接受为 P1 体验差距（`[B] …/issues/19…md:132`） |
| 坐席回复与用户消息顺序错乱 | IM 的 seq 由服务端分配，但 `send()` 不分配 seq | **缺口 G4**（§7.2） |

---

## 6. AI 单条成文对 IM 的影响

### 6.1 时延：首字预算直接失效

baize 的非功能要求是「首次响应 P95 ≤ 3s（首 token 到达即渲染）」（`[B] …/spec.md:105`）。IM 通道不推增量（`[B] …/tech-proposal.md:111`），用户看到的时间 = **完整生成时间**，含：槽位追问（瞬时）、检索 + 工具 3s 超时、模型生成（数秒至数十秒）。

| 影响 | 结论 |
|---|---|
| 「正在输入」状态 | IM 无 typing 命令（`MessageCommand` 枚举只有 P2P/ACK/READ/RECALL 九项，`[I] .../MessageCommand.java:9-35`）→ 用户侧只能靠原生「对方正在输入」（需 IM 新增命令）或界面提示 |
| 占位消息 | 明确不发（发出去改不了，`[B] …/issues/19…md:116`） |
| P95 指标 | 3s 阈值在 IM 通道不适用；需按渠道分别定义（**baize 侧未定义**） |

### 6.2 一轮 → 多条消息

Web 一轮是「一条 assistant 消息 + 10 个事件」；IM 一轮至少要 1～3 条独立消息：

| 场景 | 消息条数 | 内容 |
|---|---|---|
| 槽位追问 | 1 | `kind=slot_ask`，单句 |
| 正常回答 | 1 | `kind=ai_reply` |
| 转人工 | 2 | `kind=ai_reply`（解释）+ `kind=handoff_notice`（转接提示） |
| 工具失败兜底 | 1 | `kind=ai_reply`（兜底话术 + 转人工入口） |
| 坐席接管后的回复 | N | `kind=agent_reply` |

**幂等键因此不够用**：baize 现定 `messageId = UUID.nameUUIDFromBytes(session_id + ":" + turn_id)`（`[B] …/issues/19…md:113`），一轮只产一个 id。一轮多消息时必须派生：

```
messageId = UUID.nameUUIDFromBytes(session_id + ":" + turn_id + ":" + kind + ":" + part_index)
```

这是**契约级修订**，需要回写票 19 §4。IM 侧无需改，只要接受「id 由调用方决定且确定性生成」。

### 6.3 长度上限：5000 字符是硬墙

`im_message_body.message_body VARCHAR(5000)`（`[I] docker/mysql/db/im_core.sql:163`）。VARCHAR 的长度单位是**字符**，所以中文本身不吃亏；真正的膨胀来自 blocks 的 JSON 结构（引用的 `snippet` + 卡片的 `label/value` + `meta`，键名与括号都是纯 ASCII 开销）。超限后 MySQL 报错 → 被 `StoreMessageService` 的 `catch` 吞掉（`[I] .../StoreMessageService.java:49-61`）→ **消息静默丢失**（MQ 已 ack，无重投）。一条「订单事实 + 物流卡 + 3 条引用」的回答逼近上限是完全可能的，这不是理论风险。

| 约束 | 建议 |
|---|---|
| 单条 `messageBody` ≤ 4000 字符 | 留 20% 余量 |
| 超长时按 block 边界切分 | `meta.part_index/part_total`；切分不切断单个 block |
| 兜底：仍超长则只发 `fallback_text` | 保证可送达优先于富消息 |

### 6.4 其他单条成文的连带影响

| 影响 | 说明 | 依据 |
|---|---|---|
| 无「停止生成」 | Web 有 `POST /stop`；IM 无消息编辑也无取消语义 → 用户无法中断 | `[B] …/tech-design.md:1160` |
| 无增量纠错 | 生成完才发现引用校验失败 → 只能整条降级（baize 已选整条降级，不剥句） | `[B] …/tech-design.md:655` |
| 无 `part`/`tool.started` | 这些事件在 IM 通道全部折叠进 blocks 或丢弃 | `[B] …/issues/19…md:115` |
| trace 与消息解耦 | 用户看到一条消息，baize 侧对应 N 个 `stream_event` | `[B] …/tech-design.md:1171-1181` |

---

## 7. 接口契约缺口表（方法级）

### 7.1 入站 `ImInboundService.onMessage`

baize 侧定义（`[B] …/issues/19…md:78-95`）：

```java
ImAck onMessage(ImMessageEvent ev);   // 幂等键 appId + messageId，<50ms
record ImMessageEvent(Integer appId, String messageId, String fromId, String toId,
                      String messageBody, String extra, long messageTime, long messageSequence) {}
record ImAck(boolean accepted, String reason) {}
```

| # | 维度 | baize 已定 | IM 侧现状 | 缺口 | 归属 |
|---|---|---|---|---|---|
| G1 | 调用时机 | 「消息落库后调用」 | 落库是 MQ **异步**（`storeP2PMessage` 只投递，`[I] P2PMessageService.java:98-113`、`[I] StoreMessageService.java:49-61`） | 「落库后」在 `processor()` 内不成立。要么改为 `StoreP2PMessageReceiver` 落库成功后调，要么把语义放宽为「受理后」 | **IM 定** |
| G2 | 调用线程 | 假设可能落在 MQ 消费线程（`[B] …/issues/19…md:92`、`:167`） | 是：`P2PChatOperateReceiver` `concurrency="1"`（`[I] …/P2PChatOperateReceiver.java:43-50`） | 50ms 上限必须成立，否则拖垮单并发消费 | IM 定（独立队列或异步投递） |
| G3 | `extra` 透传 | 入站事件含 `extra` | TCP 包经 `MessagePack` 透传，`MessageContent.extra` 存在但客户端一般不填（`[I] im-common/.../MessageContent.java`） | 需明确 `extra` 的业务定义（谁填、上限 1000 字符） | 双方约定 |
| G4 | `messageSequence` | 事件含 seq，用于水位 | `processor()` 分配 seq（`[I] P2PMessageService.java:92-96`），在调用点之前可得 | 调用点若早于 seq 分配则 seq 为 null → 契约要求「调用时必须已分配」 | IM 定 |
| G5 | 幂等 | baize 建 `inbound_event` 唯一键 `app_id + external_message_id`（本期不建表） | IM 侧 Redis TTL 5 分钟去重（`[I] StoreMessageServiceImpl.java:60-107`），DB 无 `message_id` 列 | 5 分钟后同一 `messageId` 会被当新消息 → baize 侧唯一键是最后防线（正确），但 IM 需说明「重复投递窗口」 | 双方 |
| G6 | 返回值语义 | `ImAck{accepted, reason}` | — | 未定义 `accepted=false` 时 IM 怎么办（丢弃？重试？进死信？） | **baize 定** |
| G7 | 失败重试 | 由 IM 决定（`[B] …/issues/19…md:93`） | 现役：`AbstractChatOperateReceiver` 异常 `basicNack(requeue=false)`（`[I] …/AbstractChatOperateReceiver.java:53-62`）→ **不重投、直接丢** | 契约必须写死：onMessage 返回 `accepted=false` 或超时 → IM 不重试（因为 baize 已幂等），还是投递到重试队列 | IM 定 |
| G8 | 身份 | 无 cookie | IM 侧有 `(appId, fromId)` 即可 | baize 规则闸第一行是「未登录 → 401」（`[B] …/tech-design.md:867`）→ IM 渠道要改判据 | **baize 定** |
| G9 | 通道识别 | 事件里没有 `channel` 字段 | — | `ImMessageEvent` 缺 `channel`，baize 靠「哪个入口进来的」判断过于隐式；建议加显式字段 | baize 定 |

### 7.2 出站 `ImOutboundService.send`

baize 侧定义（`[B] …/issues/19…md:101-116`）：

```java
ImSendResult send(ImSendCommand cmd);  // 幂等键 messageId，超时 500ms
record ImSendCommand(Integer appId, String fromId, String toId,
                      String messageId, String messageBody, String extra, long messageTime) {}
record ImSendResult(String messageId, long messageTime) {}
```

| # | 维度 | baize 已定 | IM 侧现状 | 缺口 | 归属 |
|---|---|---|---|---|---|
| G3 | `extra` | 必填，带 `trace_id` / `sender_role` / `operator` | **请求体没有该字段**（`[I] .../SendMessageReq.java:11-36`）；`MessageContent.extra` 从不被 `send()` 赋值（`[I] P2PMessageService.java:148-160` 无 `setExtra`） | IM 需给 `send` 加 `extra` 并写库/下发；当前只能塞进 `messageBody` | **IM 定（必修）** |
| G4 | `messageSequence` | `ImSendResult` 未含 seq，但 baize 需要 `external_seq` 对齐 | `send()` 不分配 seq → 客户端收到 `messageSequence=null` | ①IM 在 `send()` 分配 seq；②`ImSendResult` 回传 seq | **IM 定（必修）** |
| G5 | 幂等 | 靠 IM 防重（确定性 `messageId`） | `send()` **无任何查重**；DB 主键是雪花 `message_key`，无 `message_id` 列（`[I] im_core.sql:158-183`） | 重复调用 `send()` 同 `messageId` → 产生两条消息 | **IM 定（必修）** |
| G6 | 失败语义 | 500ms 超时 | 无业务错误码，异常直接 500；落库失败异步吞掉（`[I] StoreMessageService.java:49-61`） | 需返回可判别结果：`accepted` / `duplicate` / `rejected(reason)` | IM 定 |
| G7 | 重试语义 | baize 幂等重试 | 未定义 | 需写明「同 messageId 重复调用返回首次结果」 | 双方 |
| G8 | 离线投递 | 契约假设 IM 负责离线（`[B] …/issues/19…md:68`） | `send()` **不写离线队列**；`sendToUserAllClient` 对离线返回空（`[I] MessageProducer.java:49-61`） | 用户离线时 AI/坐席回复全部丢失 | **IM 定（必修）** |
| G9 | 长度上限 | 无 | `VARCHAR(5000)` + 静默失败 | 需 IM 暴露上限或在 `send` 内截断并告警 | 双方 |
| G10 | 超时 500ms 的现实性 | 500ms | `send()` 同步执行 `storeP2PMessage`（RabbitMQ publish）+ 两次下行遍历，通常 <50ms；MQ 拥塞时会超 | 需明确超时=「未受理」，baize 可安全重试 | 双方 |
| G11 | 会话可见性 | 假定消息立即出现在用户会话列表 | P2P 会话行只在加好友时创建（`[I] ImFriendServiceImpl.java:286-291`） | bot 会话需自动建档 | **IM 定** |

### 7.3 兜底 `pullSince`

baize 侧契约：`@Scheduled` 每分钟按 `external_message_id` 水位补齐（`[B] …/tech-proposal.md:112`、`[B] …/issues/19…md:95`）。

现役实现 `POST v1/message/syncOfflineMessageList`（`[I] .../MessageController.java:69-73` → `[I] .../sync/MessageSyncServiceImpl.java:72-104`）存在三处硬伤：

| # | 问题 | 证据 | 后果 |
|---|---|---|---|
| P1 | **读写 key 拼法不一致**：写 `appId + ":offlineMessage:" + userId`；读 `appId + ":" + ":offlineMessage:" + ":" + operater` | `[I] MessageStoreServiceImpl.java:127` vs `[I] MessageSyncServiceImpl.java:76` | 写入 `10001:offlineMessage:10002`，读取 `10001:::offlineMessage::10002` → **永远读不到** |
| P2 | 水位用 `messageKey`（雪花）而非 `messageSequence` | `[I] MessageStoreServiceImpl.java:140`（score=messageKey）、`[I] MessageSyncServiceImpl.java:101`（`maxSeq <= getMessageKey()`） | 与 baize 的「按 `external_seq` 水位」语义不符 |
| P3 | 拉取后不清理 ZSet | `[I] im-domain/.../sync/MessageSyncServiceImpl.java:72-104` 无 `removeRangeByScore` | 重复拉取同一批 |

→ **baize 契约里那条「丢消息兜底」在现役实现上不可用**。契约要求不因此失效（它要求的是「IM 提供一个按水位增量拉取的能力」），但 IM 侧必须重做该接口，否则 onMessage 一旦超时就永久丢消息。

---

## 8. IM 侧最小改造清单

标注：**类型** = 数据模型 / 接口 / 协议层。优先级 P0 = 阻塞单聊闭环；P1 = 可用性；P2 = 体验。

| # | 改造项 | 类型 | 优先级 | 为什么必须 | 依赖 | 粗估 |
|---|---|---|---|---|---|---|
| M1 | `im_message_body` / `im_message_history` 增 `message_id` 列 + 唯一键 `(app_id, owner_id, message_id)` | 数据模型 | P0 | 现无列、无唯一键，`send()` 重复调用产生重复消息（G5） | 需 DDL 迁移 | 1 天 |
| M2 | `send()` 补三件事：分配 `messageSequence`、写 `extra`、写离线队列 | 接口 | P0 | G3/G4/G8 三项同时缺失，缺一项即有静默丢失 | M1 | 2 天 |
| M3 | `SendMessageReq` 增 `extra` 字段；`ImSendResult` 等价增 `messageSequence` | 协议层 | P0 | baize 信封需要 `trace_id`/`sender_role`；序列号需回传 | M2 | 0.5 天 |
| M4 | `send()` 幂等：先查 `message_id` 唯一键，命中返回首次结果 | 接口 | P0 | G5 | M1 | 0.5 天 |
| M5 | `send()` 返回结构化结果（`accepted` / `duplicate` / `rejected`），不靠 HTTP 500 | 协议层 | P0 | G6/G7；baize 需要区分「可重试」与「该放弃」 | M2 | 0.5 天 |
| M6 | 落地 `user_type` 语义：`fromId`/`toId` 命中 `user_type=3` 时免好友与黑名单校验 | 接口 | P0 | A2；否则 bot 收不到任何用户消息 | 需确定 bot 白名单口径 | 1 天 |
| M7 | bot 会话自动建档：首条 `send()` 时补 `im_conversation_set` P2P 行 | 接口 | P1 | G11；否则用户会话列表看不到 bot | M6 | 0.5 天 |
| M8 | 入站调用点：在消息持久化成功后调用 `onMessage`，且不在 MQ 单并发消费线程上阻塞 | 接口 | P0 | G1/G2 | 需 IM 决定（新队列 / 异步线程池） | 1–2 天 |
| M9 | 入站失败语义：`ImAck.accepted=false` 或超时 → 不重投（baize 已幂等）+ 落死信可查 | 接口 | P1 | G7 | M8 | 0.5 天 |
| M10 | 重做离线增量拉取：key 修正 + 水位统一为 `messageSequence` + 拉取后清理 | 接口 | P1 | P1/P2/P3；baize 兜底链路唯一样本 | 无 | 1 天 |
| M11 | `messageBody` 长度契约：暴露上限（5000）或服务端截断 + 告警日志 | 协议层 | P1 | G9；现役超长静默丢 | 无 | 0.5 天 |
| M12 | 单向 `message.send.after` 回调接线（若不用 RPC，用回调作为过渡） | 接口 | P2 | 现 `SendMessageAfter` 常量零引用（`[I] Constants.java:133`），回调是现成骨架 | M8 二选一 | 1 天 |
| M13 | 消息撤回实现（`MSG_RECALL` 仅常量） | 接口 | P2 | 坐席发错无法撤回（§5.3） | 无 | 1 天 |
| M14 | `typing` 命令（IM 无此命令） | 协议层 | P2 | 单条成文下的等待体验（§6.1） | 需客户端配合 | 1 天 |
| M15 | bot 发消息限流/越权保护：bot 只能发给与其有会话的用户 | 接口 | P0（与 M6 同批） | M6 的免校验会放大越权面 | M6 | 0.5 天 |

**合计 P0：约 7–8 人日；P1 另加 2 人日（不含 IM 重构自身的消息模型调整）。**

### 8.1 明确不需要 IM 做的（防止范围膨胀）

| 不做 | 理由 | 依据 |
|---|---|---|
| 卡片渲染 / action 回调 | blocks 全部可降级为文本，action 转文本指令 | §3.4 |
| 转人工信号与状态 | baize 单方所有 | §4.2 |
| 会话语义、槽位、引用 | baize 领域内 | `[B] …/issues/19…md:69` |
| AI 表 / 双写 | 两边只对齐 `external_message_id` | `[B] …/issues/19…md:70` |
| 群聊渠道 | P2，第二套入站语义 | `[B] …/issues/19…md:139` |

### 8.2 需要回写 baize 文档的修订（供主会话入库时用）

| # | 修订点 | 位置 |
|---|---|---|
| B1 | `messageId` 派生规则从 `session:turn` 改为 `session:turn:kind:part_index`（一轮多消息） | `[B] …/issues/19…md:113` |
| B2 | 「`extra` 带 trace_id/sender_role」降级为「信封内 `meta`；`extra` 待 IM 补齐后迁移」 | `[B] …/tech-proposal.md:110`、`[B] …/issues/19…md:114` |
| B3 | 票 19 事实表两处修正：IM **有** `user_type` bot 位；`send()` **无** messageId 防重 | `[B] …/issues/19…md:23`、`:26` |
| B4 | blocks 字段命名统一（C5），并补 `schema_version` / `fallback_text` / `kind` / `meta` | `[B] …/tech-design.md:384-398` |
| B5 | 规则闸「未登录 → 401」按渠道分支（IM 渠道身份来自 `appId+fromId`） | `[B] …/tech-design.md:867` |
| B6 | 补 IM 渠道的首响指标口径（3s 阈值不适用） | `[B] …/spec.md:105`、`[B] …/tech-design.md:1080-1087` |

---

## 9. 未验证项

| # | 项 | 为什么没验证 |
|---|---|---|
| U1 | `send()` 提交未知字段 `extra` 的行为 | 未起服务实测；按 Spring Boot 默认 `FAIL_ON_UNKNOWN_PROPERTIES=false` 推断为静默忽略，仓内无 ObjectMapper 自定义（`[I] RedisConfig.java:35` 仅改 Redis 序列化器） |
| U2 | `VARCHAR(5000)` 在 utf8mb4 下的实际插入表现（报错 vs 截断） | 依赖 MySQL `sql_mode`，仓内未见显式设置；`docker/mysql` 配置未逐项读取 |
| U3 | IM 重构后的消息模型（`messageSequence` / `messageKey` 是否保留、是否引入会话维度） | 重构未发生，票 19 已登记为待确认（`[B] …/issues/19…md:166`） |
| U4 | RPC 载体（SCF / Feign）对 500ms 超时与 50ms 入站预算的可行性 | 载体未定（`[B] …/issues/19…md:165`） |
| U5 | IM 客户端是否已具备文本指令到 action 的映射能力 | 仓内无前端工程，只有 `prototype/`、`im-design-demo/`，未读 |
| U6 | `syncOfflineMessageList` 是否有其他调用方修过 key（P1 可能已被绕过） | 只读当前分支代码，未查提交历史 |

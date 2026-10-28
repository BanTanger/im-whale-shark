# 线协议 v1

> 状态：codec 层已实现并通过契约测试（`im-codec/src/test/java/com/bantanger/im/codec/wire/FrameCodecTest.java`，9 例）。
> 尚未替换 `im-tcp` 的既有 pipeline，旧 `Message`/`MessageHeader` 仍在服役，迁移分期见末节。

## 1. 为什么要重做

旧协议实测有三处硬伤：

| 问题 | 事实 |
| --- | --- |
| 上下行不对称 | 上行 28 字节头（7×int32）+ imei + body；下行只有 8 字节头（command + len）+ body（`WebSocketMessageEncoderHandler`）。SDK 必须写两套编解码 |
| 文档与代码不一致 | README 的字段顺序和 `ByteBufToMessageUtils` 对不上（`clientType`/`messageType`、`appId`/`imeiLength` 两组顺序相反） |
| 没有请求号 | `MessageHeader` 没有 sequence/requestId 字段，请求与响应只能靠业务字段猜 |

另外两条连带问题：WS 与 TCP 两条通道的下行载荷形状不同（WS 序列化整个 `MessagePack`，TCP 只序列化 `data`）；body 固定 JSON，媒体与富消息没有容身处。

## 2. 业界参照

- **定长头 + 变长体 + protobuf**：52im《一套基于 Netty 的分布式高可用 IM》给出的头部是 `headData(4) / version(4) / cmd(1) / msgType(1) / logId(4) / sequenceId(4) / dataLength(4)`，本文即按这个思路排布，但把 `cmd` 扩到 2 字节、把方向与编码各留 1 字节。
- **protobuf vs JSON**：52im 的 Protobuf 专题实测口径——同一条结构化消息 pb 约为 JSON 的 1/10、XML 的 1/20；解析速度高 4–5 倍。注意其 varint 只对小于 128 的整数省字节，长文本并不省，靠的是「不传字段名」。
- **什么时候该上 pb**：52im《开发IM是自己设计协议用字节流好还是字符流好？》的建议是「协议没稳定前用简单格式，稳定后再换 pb」。本项目命令集已收敛到 10 条、成员字段固定，满足这个前提；换语言（TS SDK）也正需要 IDL。
- **心跳**：应用层 ping/pong，客户端定时发、服务端原样回带 `client_time` 让客户端算 RTT，服务端用 Netty `IdleStateHandler` 兜底。52im 与多家厂商实践一致。

不做的事：协议级加密（另开）、多版本共存（version 不匹配直接断链）、压缩（`codec` 字节已预留）。

## 3. 帧格式

**上下行完全对称，大端**，头 20 字节：

| 偏移 | 宽度 | 字段 | 说明 |
| --- | --- | --- | --- |
| 0 | 4 | `magic` | `0x494D5753`（"IMWS"）；粘包/半包与协议识别 |
| 4 | 2 | `version` | 当前 `1`；不匹配直接拒帧 |
| 6 | 1 | `type` | `0` REQUEST / `1` RESPONSE / `2` NOTIFY |
| 7 | 1 | `codec` | `0` = protobuf（预留切换） |
| 8 | 2 | `command` | 业务命令码，见 §5 |
| 10 | 2 | `status` | `0` = 成功；仅 RESPONSE 与错误通知有意义 |
| 12 | 4 | `seq` | 请求-响应关联；NOTIFY 由服务端单调递增 |
| 16 | 4 | `bodyLen` | 帧体字节数，上限 4 MiB |
| 20 | n | `body` | protobuf 序列化的帧体 |

设计取舍：

- **imei 不再是头部的一段定长区域**，改为 `LoginRequest.imei` 字段。旧协议把变长字符串塞在头与体之间，是 28 字节这个数字的来源，也是分片 bug 的来源。
- **`seq` 由客户端生成**，服务端在 RESPONSE 里原样回带；这样 SDK 可以做请求-响应匹配与超时重试。
- **命令码沿用旧数值**（9000 / 9003 / 9999 / 1103 / 2104 / 1046 / 1106 / 1053 / 9005），服务端 `CommandFactoryConfig` 的策略表可 1:1 复用，迁移不需要重排。
- **帧体不包 oneof 信封**：`command` 已唯一确定类型，再包一层只多两字节和一次解包。代价是新命令要两端同时上线，因此未知命令返回 `ErrorResponse` 而不是抛异常。

## 4. 帧体

`im-codec/src/main/proto/im_wire.proto`（proto3，`package im.wire.v1`）。

| 命令 | REQUEST | RESPONSE | NOTIFY |
| --- | --- | --- | --- |
| LOGIN 9000 | `LoginRequest` | `LoginResponse` | — |
| MUTUAL_LOGIN 9002 | — | — | `LoginResponse` |
| LOGOUT 9003 | `LogoutRequest` | `LogoutResponse` | — |
| SEND_MESSAGE 9005 | `SendMessageRequest` | `SendMessageResponse` | — |
| P2P_MESSAGE 1103 | — | — | `MessageNotify` |
| GROUP_MESSAGE 2104 | — | — | `MessageNotify` |
| ACK 1046 | `AckRequest` | （空体，status 表结果） | — |
| READ 1106 | `ReadRequest` | （空体） | — |
| READ_NOTIFY 1053 | — | — | `ReadNotify` |
| PING 9999 | `PingRequest` | `PongResponse` | — |

错误一律走 `ErrorResponse`（`status != 0` 时按此解析），它带原始 `command` 便于日志定位。

**消息内容是一棵可扩展树**：

```
ChatMessage.content = MessageContent{ oneof { text | image | file | recall } }
```

旧模型只有一个 `VARCHAR(5000)`，图片/文件只能靠字符串里的约定字段表达。新模型把 `message_id`（客户端生成、重发不变）与 `message_key`（服务端分配）**分开**，对应 `GLOSSARY.md` 里 MessageId 与 MessageKey 两个词条，不再混用。

## 5. 实现位置

```
im-codec/src/main/proto/im_wire.proto            协议定义
im-codec/src/main/java/.../codec/wire/Frame.java          不可变帧模型
                                          FrameCodec.java  编解码 + 完整性校验
                                          FrameType.java   方向枚举
                                          WireCommand.java 命令码
                                          WirePayloads.java command → protobuf 类型
                                          WireFrameDecoder / WireFrameEncoder        TCP
                                          WireWebSocketDecoder / WireWebSocketEncoder WS
```

Netty 侧只做分帧，业务解析交给 `WirePayloads.parse(frame)`。

## 6. 兼容与演进规则

1. `version` 只增不减；客户端与网关版本不一致 → 网关回 `ErrorResponse` 后断链，不做多版本共存。
2. protobuf 字段只增不删、tag 不复用；删除字段必须 `reserved`。
3. 新命令码先在服务端注册，再放客户端；客户端遇到未知 `command` 静默丢弃并记日志。
4. 帧体上限 4 MiB（`FrameCodec.MAX_BODY_LENGTH`）。超限 = 协议违规 = 断链，不做截断。旧协议这里是 `VARCHAR(5000)` 超长后**静默丢消息**（`StoreMessageService` 把异常吞掉），新协议把这类问题前移到接入层。

## 7. 迁移分期

| 期 | 内容 | 状态 |
| --- | --- | --- |
| 1 | codec 层：协议定义、帧编解码、Netty 分帧、契约测试 | **已完成** |
| 2 | `im-tcp` pipeline 换成新的 decoder/encoder，登录/心跳/收发策略改成消费 `Frame` | 未开始 |
| 3 | 服务端下行改 `MessageNotify`，`send()` 补齐 `message_key`/`sequence`/`error` 语义 | 未开始 |
| 4 | TS SDK 按本规范实现（与 Web 前端同期） | 未开始 |

旧代码在 1–2 期之间并存：`com.bantanger.im.codec.proto.Message` / `MessageHeader` 继续被现有 handler 使用，第 2 期结束后删除。

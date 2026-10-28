# Web 前端骨架 · 原型结论

**问题**：im-whale-shark 的 Web 前端长什么样？

**裁决**：选 **B（四栏常驻 · D 方案同构）**。A、C 出局，代码保留在本分支作为 primary source，不进主分支。

## 变体与出局理由

| key | 结构 | 结论 |
| --- | --- | --- |
| A 微信三段 · 抽屉 | `60 / 300 / 1fr`，信息栏从右侧滑出 | 出局。信息密度最低，群成员、群公告、trace 都得点开才看得到，客服台尤其吃亏 |
| **B 四栏常驻** | `60 / 272 / 1fr / 344`，右栏常驻 | **采纳**。四种 view 共用一套骨架，右侧一个槽位按上下文换内容 |
| C 单列优先 · 覆盖面板 | 顶栏 + 可收起列表 + 744px 窄消息列 | 出局。消息列读起来舒服，但顶栏页签与左侧列表职责重叠，且覆盖面板挡住了会话上下文 |

## B 的硬参数

- 列宽：rail 60 / list 272 / main 1fr / insp 344，主栏 = 视口 − 676px。
- 塌陷：`< 1180px` 右栏收 54px 竖条（`writing-mode: vertical-rl`），`< 980px` list 收 60px 图标条。
- 右栏**一个槽位三种内容**：单聊资料 / 群成员+公告 / trace 瀑布。不新增第五列。
- A 的抽屉行为保留为塌陷态的降级方案，不单独实现。

## 颜色（三个变体共用，故不构成变体差异）

`#71D49D` 只做填充 / 自己气泡 / 未读点（白底 1.81:1）；
`#0B7F56` 做文字与主按钮底（白底 5.02:1）；
`#096A49` hover；`#EAFBF2` 选中底；`#0F1720` 近黑中性。

## 原型里已成型、可直接进规格的组件

`.crow` 会话行（五件套：头像 / 名称 / 摘要 / 时间 / 未读）、`.bubble` 气泡、`.toolcall` 工具调用卡（args / result 双列 mono）、`.cite` 引用 chip、`.handoff` 转人工卡、`.membergrid` 成员网格、`.spanrow` trace 瀑布行、`.kpis` KPI 条、`.kv` 属性表。

## 跑法

双击 `prototype/web-ui-prototype.html`，或：

```bash
open prototype/web-ui-prototype.html?variant=B
```

`?variant=A|B|C&view=chat|contacts|agent|metrics&conv=c1&info=open&list=closed`，底部浮条或 `←` `→` 切换。原型是只读 mock，发送/搜索/加好友都是死按钮。

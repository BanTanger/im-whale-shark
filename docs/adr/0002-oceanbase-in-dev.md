# 本地开发直接跑 OceanBase，不先用 MySQL 过渡

开发环境（docker compose）跑 OceanBase 社区版单机，而不是先写 MySQL、上线前再切。

## Context

这套设计里有两个能力只存在于 OB：**分区表**（`PARTITION BY HASH(conv_id)`）与**全局唯一索引**（`uk_server_msg ... GLOBAL`）。MySQL 两者都没有——在 MySQL 上开发，等于把最需要验证的东西（分区裁剪是否生效、全局索引的写入代价、局部唯一索引必须含分区键）全部推迟到切换那一刻。

## Considered Options

- **MySQL 过渡**：起步快（镜像小、启动快），但 DDL 要写两套，切换时重写迁移脚本与验证 SQL。
- **OB 社区版单机**：镜像重、启动慢，但 DDL 一次写对，分区与索引行为本地可验。
- **OB 云实例**：最省事，但引入外部依赖与费用，不适合随手跑测试。

## Consequences

- 本地起停更慢，CI 需要缓存镜像。
- `PARTITION BY`、`GLOBAL` 索引、`auto_split` 等语法在社区版与商业版之间可能存在差异，落地时要逐条验证（§8.2 已标注）。
- 若将来 OB 被换掉，DDL 里的分区子句是唯一需要重写的地方，业务代码不受影响（`MessageRepository` 端口在这里起作用）。

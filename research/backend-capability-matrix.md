# IM Web 一期：后端能力矩阵

盘点对象：`/Users/zz/AiProjects/im-whale-shark`（master 分支只读扫描）。
覆盖范围：`im-domain`、`im-message-store`、`im-common`、`im-infrastructure`、`im-tcp`、`im-codec`、`assert/sql/im_core.sql`。
不含 `im-design-demo`（设计模式演示工程，与 IM 业务无关，`pom.xml:24` 独立 module）。

## 0. 部署拓扑与调用入口

| 模块 | 启动类 | 端口 | 职责 | 前端是否直连 |
|---|---|---|---|---|
| `im-domain` | `im-domain/src/main/java/com/bantanger/im/domain/Application.java` | HTTP 18000（`im-domain/src/main/resources/application.yml` `server.port: 18000`） | 业务 REST：会话/好友/群/用户/后台发消息 | 是 |
| `im-message-store` | `im-message-store/src/main/java/com/bantanger/im/message/Application.java` | HTTP 18001（`im-message-store/src/main/resources/application.yml` `server.port: 18001`） | 消息历史同步（3 个只读端点） | 是（与 18000 同前缀 `v1/message`，需按端口分流） |
| `im-tcp` | `im-tcp/src/main/java/com/bantanger/im/tcp/Starter.java` | TCP 19001 / WebSocket 19002（`im-tcp/src/main/resources/config.yml`） | 长连接网关，同时起 `ImServer` + `ImWebSocketServer`（`Starter.java:39-40`） | 是（WS `ws://host:19002/ws`） |

WS 连接路径固定为 `/ws`（`im-tcp/.../server/ImWebSocketServer.java:68`），只接受 **BinaryWebSocketFrame**（`im-codec/.../WebSocketMessageDecoderHandler.java:22`）。

---

## 表 1：REST 端点全景（共 51 个）

说明：
- 所有入参 DTO 若 `extends RequestBase`，则隐式带 `appId`（`@NotNull`）、`operater`（`@NotBlank`）、`clientType`、`imei` 四个字段（`im-common/src/main/java/com/bantanger/im/common/model/RequestBase.java:24-35`）；下文用 `+RB` 标记，不再重复列举。
- 「写库」= 该端点路径上是否执行 INSERT/UPDATE/DELETE（含异步 MQ 落库）。
- 「页面用途」按 会话列表 / 聊天 / 通讯录 / 群管理 / 好友申请 / 客服台 / 观测 归类。

### 1.1 ConversationController — `v1/conversation`（3 个）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 1 | POST `/v1/conversation/deleteConversation` | `deleteConversation`（`ConversationController.java:26`） | `DeleteConversationReq`+RB：`conversationId`（@NotBlank）、`fromId`（@NotBlank） | `null` | 否，**不删库**，仅在 `deleteConversationSyncMode==1` 时推 WS `CONVERSATION_DELETE`（`ConversationServiceImpl.java:90-100`） | 是 | 会话列表 |
| 2 | POST `/v1/conversation/updateConversation` | `updateConversation`（`:31`） | `UpdateConversationReq`+RB：`conversationId`、`isMute`、`isTop`、`fromId`（三者均无校验注解） | `null` | 是，UPDATE `im_conversation_set`（`ConversationServiceImpl.java:107-122`） | 是（覆盖写） | 会话列表（置顶/免打扰） |
| 3 | POST `/v1/conversation/syncConversationList` | `syncConversationList`（`:36`） | `SyncReq`+RB：`lastSequence`、`maxLimit` | `SyncResp<ImConversationSetEntity>`：`maxSequence` / `isCompleted` / `dataList[conversationId, conversationType, fromId, toId, isMute, isTop, sequence, readSequence, appId]` | 否 | 是 | 会话列表（增量） |

> 注意：`syncConversationSet` 用 `ge("sequence", lastSequence)`（`ConversationServiceImpl.java:150`），是 **>=** 而非 **>**，客户端每轮会重复拿到边界那条会话。同文件 `:142` 直接读 `req.getMaxLimit()` 做比较，`maxLimit` 为 null 时 NPE。

### 1.2 ImFriendShipController — `v1/friendship`（12 个）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 4 | POST `/v1/friendship/importFriendShip` | `importFriendShip`（`ImFriendShipController.java:50`） | `ImportFriendShipReq`+RB：`fromId`（@NotBlank）、`friendItem[]{toId, remark, addSource, status, black}` | `ImportFriendShipResp{successId[], errorId[]}` | 是，批量 INSERT `im_friendship`（`ImFriendServiceImpl.java:80-110`） | 是（重复导入进 `errorId`） | 通讯录（初始化/迁移） |
| 5 | POST `/v1/friendship/addFriend` | `addFriend`（`:101`） | `AddFriendReq`+RB：`fromId`（@NotBlank）、`toItem: FriendDto{toId, remark, addSource, extra, addWording}` | `null` | 是，视对方 `friendAllowType` 决定直接双向 INSERT `im_friendship` 或 INSERT `im_friendship_request` | 否（已是好友返回 40002） | 好友申请 / 通讯录 |
| 6 | POST `/v1/friendship/updateFriend` | `updateFriend`（`:106`） | `UpdateFriendReq`+RB：`fromId`（@NotBlank）、`toItem: FriendDto` | `null` | 是，UPDATE `im_friendship.remark`（`ImFriendServiceImpl.java:197-213`） | 是 | 通讯录（改备注） |
| 7 | POST `/v1/friendship/deleteFriend` | `deleteFriend`（`:111`） | `DeleteFriendReq`+RB：`fromId`（@NotBlank）、`toId`（@NotBlank） | `null` | 是，UPDATE `im_friendship.status=2` | 是 | 通讯录 |
| 8 | POST `/v1/friendship/deleteAllFriend` | `deleteAllFriend`（`:116`） | `DeleteFriendReq`+RB（`toId` 未使用） | `null` | 是，批量 UPDATE `status=2`（`:403-418`） | 是 | 通讯录 |
| 9 | POST `/v1/friendship/getAllFriendShip` | `getAllFriendShip`（`:121`） | `GetAllFriendShipReq`+RB：`fromId`（@NotBlank） | `List<ImFriendShipEntity>`：`appId, fromId, toId, remark, status, black, createTime, friendSequence, blackSequence, addSource, extra` | 否 | 是 | 通讯录（全量，**无分页**） |
| 10 | POST `/v1/friendship/getRelation` | `getRelation`（`:126`） | `GetRelationReq`+RB：`fromId`、`toId`（均 @NotBlank） | `ImFriendShipEntity` | 否 | 是 | 通讯录 / 聊天头部关系判断 |
| 11 | POST `/v1/friendship/checkFriend` | `checkFriend`（`:131`） | `CheckFriendShipReq`+RB：`fromId`、`toIds[]`、`checkType`（1=单向，非 1=双向） | `List<CheckFriendShipResp{fromId, toId, status}>` | 否 | 是 | 通讯录 / 加好友前置 |
| 12 | POST `/v1/friendship/addBlack` | `addBlack`（`:136`） | `AddFriendShipBlackReq`+RB：`fromId`（@NotBlank）、`toId`（无校验） | `null` | 是，UPDATE `im_friendship.black`（`:476-545`） | 否（重复拉黑返回 40003） | 黑名单 |
| 13 | POST `/v1/friendship/deleteBlack` | `deleteBlack`（`:141`） | `DeleteBlackReq`+RB：`fromId`、`toId`（均 @NotBlank） | `null` | 是，UPDATE `black` 归位 | 是 | 黑名单 |
| 14 | POST `/v1/friendship/checkBlck` | `checkBlck`（`:146`） | `CheckFriendShipReq`+RB（复用加好友 DTO） | `List<CheckFriendShipResp>` | 否 | 是 | 黑名单 |
| 15 | POST `/v1/friendship/syncFriendShipList` | `syncFriendShipList`（`:156`） | `SyncReq`+RB：`lastSequence`、`maxLimit` | `SyncResp<ImFriendShipEntity>`；**空结果时走 `ResponseVO.successResponse()`，data 为 null**（`ImFriendServiceImpl.java:657`） | 否 | 是 | 通讯录（增量） |

> `checkType != 1` 时走 `checkFriendShipBoth` / `checkFriendShipBlackBoth`，这两个 SQL 少写了一个 `and`（`im-domain/src/main/resources/mapper/ImFriendShipMapper.xml`：`where app_id = #{appId} from_id = #{fromId}`），执行必然语法错误。前端只能调 `checkType=1` 分支。

### 1.3 ImFriendShipGroupController — `v1/friendship/group`（4 个）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 16 | POST `/v1/friendship/group/add` | `add`（`ImFriendShipGroupController.java:31`） | `AddFriendShipGroupReq`+RB：`fromId`（@NotBlank）、`groupName`（@NotBlank）、`toIds[]` | `null` | 是，INSERT `im_friendship_group` + 递归 `addGroupMember`（`ImFriendShipGroupServiceImpl.java:53-106`） | 否（重名 40007） | 通讯录（好友分组） |
| 17 | POST `/v1/friendship/group/del` | `del`（`:36`） | `DeleteFriendShipGroupReq`+RB：`fromId`（@NotBlank）、`groupName[]`（@NotEmpty） | `null` | 是，UPDATE `del_flag=1` | 是 | 通讯录（好友分组） |
| 18 | POST `/v1/friendship/group/member/add` | `memberAdd`（`:41`） | `AddFriendShipGroupMemberReq`+RB：`fromId`、`groupName`、`toIds[]`（均必填） | `AddGroupMemberResp{successId[], errorId[]}` | 是，INSERT `im_friendship_group_member` | 是（失败进 `errorId`） | 通讯录（好友分组） |
| 19 | POST `/v1/friendship/group/member/del` | `memberdel`（`:46`） | `DeleteFriendShipGroupMemberReq`+RB：`fromId`、`groupName`、`toIds[]` | `List<String>`（successId，`ImFriendShipGroupMemberServiceImpl.java:128`） | 是，DELETE 成员行 | 是 | 通讯录（好友分组） |

> **无「查询好友分组列表」端点**：`ImFriendShipGroupController` 只有 add/del/member-add/member-del，`ImFriendShipGroupEntity` 有 `groupId/groupName/sequence` 但没有任何 GET/SYNC 接口暴露。前端拿不到分组清单，只能靠 WS `FRIEND_GROUP_ADD(3012)` 事件侧录。且 `im_friendship_group_member` 主键是单列 `group_id`（见 表 3），一个分组只能存 1 个成员。

### 1.4 ImFriendShipRequestController — `v1/friendshipRequest`（3 个）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 20 | POST `/v1/friendshipRequest/approveFriendRequest` | `approveFriendRequest`（`ImFriendShipRequestController.java:42`） | `ApprovalFriendRequestReq`+RB：`id`（@NotNull，好友申请主键）、`status`（@NotNull，1 同意 / 2 拒绝） | `null` | 是，UPDATE `im_friendship_request.approve_status`，同意时写 `im_friendship` | 否（已审批返回错误） | 好友申请 |
| 21 | POST `/v1/friendshipRequest/getFriendRequest` | `getFriendRequest`（`:46`） | `GetFriendShipRequestReq`+RB：`fromId`（@NotBlank，语义是「收件人」） | `List<ImFriendShipRequestEntity>`：`id, appId, fromId, toId, remark, readStatus, addSource, addWording, approveStatus, createTime, updateTime, sequence` | 否 | 是 | 好友申请（列表） |
| 22 | POST `/v1/friendshipRequest/readFriendShipRequestReq` | `readFriendShipRequestReq`（`:51`） | `ReadFriendShipRequestReq`+RB：`fromId`（@NotBlank） | `null` | 是，UPDATE `read_status=1`（`:168-190`） | 是 | 好友申请（已读红点） |

> `getFriendRequest` 用 `LambdaQueryWrapper.eq(toId, fromId)`（`ImFriendShipRequestServiceImpl.java:49-56`），**无分页、无 approveStatus 过滤、无时间排序**，全量返回。

### 1.5 ImGroupController — `v1/group`（10 个）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 23 | POST `/v1/group/importGroup` | `importGroup`（`ImGroupController.java:63`） | `ImportGroupReq`+RB：`groupId, groupName(@NotBlank), ownerId, groupType, mute, applyJoinType, introduction, notification, photo, MaxMemberCount, createTime, extra` | `null` | 是，INSERT `im_group` | 是（已存在返回 40001「群已存在」） | 群管理（导入） |
| 24 | POST `/v1/group/createGroup` | `createGroup`（`:73`） | `CreateGroupReq`+RB：`groupId, groupType, ownerId, groupName(@NotBlank), member[]( @NotEmpty ), mute, applyJoinType, introduction, notification, photo, MaxMemberCount, extra` | `null` | 是，INSERT `im_group` + `im_group_member` + `im_conversation_set` | 否 | 群管理（建群） |
| 25 | POST `/v1/group/getGroupInfo` | `getGroupInfo`（`:83`） | `GetGroupReq`+RB：`groupId`（**无校验**） | `GetGroupResp`：`groupId, appId, ownerId, groupType, groupName, mute, applyJoinType, privateChat, introduction, notification, photo, maxMemberCount, status, memberList[GroupMemberDto]` | 否 | 是 | 群管理 / 聊天（群详情） |
| 26 | POST `/v1/group/update` | `update`（`:93`） | `UpdateGroupReq`+RB：`groupId`（@NotBlank）、`groupName, mute, applyJoinType, introduction, notification, photo, maxMemberCount, extra` | `null` | 是，UPDATE `im_group`（`:204-286`） | 是 | 群管理（群设置） |
| 27 | POST `/v1/group/getJoinedGroup` | `getJoinedGroup`（`:103`） | `GetJoinedGroupReq`+RB：`memberId`（@NotBlank）、`groupType[]`、`limit`、`offset` | `GetJoinedGroupResp{totalCount, groupList: List<ImGroupEntity>}` | 否 | 是 | 群管理（我加入的群） |
| 28 | POST `/v1/group/destroyGroup` | `destroyGroup`（`:113`） | `DestroyGroupReq`+RB：`groupId`（@NotNull） | `null` | 是，UPDATE `im_group.status=1`（逻辑解散） | 是 | 群管理 |
| 29 | POST `/v1/group/transferGroup` | `transferGroup`（`:123`） | `TransferGroupReq`+RB：`groupId`（@NotNull）、`ownerId`（**无校验**） | `null` | 是，UPDATE `im_group.owner_id` + `im_group_member.role` | 是 | 群管理（转让群主） |
| 30 | POST `/v1/group/forbidSendMessage` | `forbidSendMessage`（`:128`） | `MuteGroupReq`+RB：`groupId`（@NotBlank）、`mute`（@NotNull） | `null` | 是，UPDATE `im_group.mute`（`:456-496`） | 是 | 群管理（全员禁言） |
| 31 | POST `/v1/group/sendMessage` | `sendMessage`（`:133`） | `SendGroupMessageReq`+RB：`messageId, fromId, groupId, messageRandom, messageTime, messageBody, badgeMode, messageLifeTime, appId` | `SendMessageResp{messageId, messageTime}` | 是，异步 MQ 落 `im_message_body` + `im_group_message_history` | 是（`messageId` 5 分钟 Redis 去重，`GroupMessageService.java:62-75`） | 聊天（后台代发群消息） |
| 32 | POST `/v1/group/syncJoinedGroup` | `syncJoinedGroup`（`:138`） | `SyncReq`+RB：`lastSequence`、`maxLimit` | `SyncResp<ImGroupEntity>`：`maxSequence / isCompleted / dataList[]` | 否 | 是 | 群管理（群列表增量） |

> `GetGroupResp.privateChat` 在 `ImGroupEntity` 与 `im_group` 表中都不存在（`ImGroupServiceImpl.java:443` 靠 `BeanUtils.copyProperties` 拷贝，该字段恒为 null）。
> `syncJoinedGroup` 未做 `maxLimit` null 兜底，`:501` 直接比较，null 时 NPE。

### 1.6 ImGroupMemberController — `v1/group/member`（5 个）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 33 | POST `/v1/group/member/importGroupMember` | `importGroupMember`（`ImGroupMemberController.java:24`） | `ImportGroupMemberReq`+RB：`groupId`（@NotBlank）、`members[]: GroupMemberDto{memberId, alias, role, speakDate, joinType, joinTime}`（**无 @NotEmpty**） | `List<AddMemberResp{memberId, result, resultMessage}>`（result: 0 成功 / 1 失败 / 2 已入群） | 是，批量 INSERT `im_group_member` | 是（已入群 result=2） | 群管理（导入成员） |
| 34 | POST `/v1/group/member/add` | `addMember`（`:29`） | `AddGroupMemberReq`+RB：`groupId`（@NotBlank）、`members[]`（@NotEmpty） | `List<AddMemberResp>` | 是（`:239-328`） | 是（已入群返回 REPEAT_JOIN） | 群管理（拉人入群） |
| 35 | POST `/v1/group/member/remove` | `removeMember`（`:34`） | `RemoveGroupMemberReq`+RB：`groupId`（@NotBlank）、`memberId`（**无校验**） | `null` | 是，UPDATE `im_group_member.role=4`（软删）+ `leave_time` | 是 | 群管理（踢人） |
| 36 | POST `/v1/group/member/update` | `updateGroupMember`（`:39`） | `UpdateGroupMemberReq`+RB：`groupId`、`memberId`（均 @NotBlank）、`alias`、`role`、`extra` | `null` | 是，UPDATE `alias` / `role`（`:433-521`） | 是 | 群管理（改群昵称/设管理员） |
| 37 | POST `/v1/group/member/speak` | `speak`（`:44`） | `SpeaMemberReq`+RB：`groupId`、`memberId`（均 @NotBlank）、`speakDate`（@NotNull，禁言到期时间戳） | `null` | 是，UPDATE `im_group_member.speak_date`（`:550-626`） | 是 | 群管理（单人禁言） |

> **无「查询群成员列表」端点**：`imGroupMemberService.getGroupMember()`（`ImGroupMemberServiceImpl.java:417`）只在 `getGroupInfo` 内部被调用，未暴露 REST。成员分页/搜索只能靠 `getGroupInfo` 一次性带出全量 `memberList`。

### 1.7 MessageController — `v1/message`（4 个，im-domain / 18000）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 38 | POST `/v1/message/send` | `send`（`MessageController.java:43`） | `SendMessageReq`+RB：`messageId, fromId, toId, messageRandom, messageTime, messageBody, badgeMode, messageLifeTime, appId` | `SendMessageResp{messageId, messageTime}` | 是（异步 MQ），但**不写离线队列**、**不生成 `messageSequence`**、**不做 messageId 去重**（`P2PMessageService.java:149-169`） | **否** | 聊天（服务端/后台代发单聊） |
| 39 | POST `/v1/message/p2pCheckSend` | `checkP2PSend`（`:53`） | `CheckSendMessageReq{fromId, toId, appId, command}`（普通 POJO，不继承 RequestBase） | `null`（仅 code/msg） | 否 | 是 | 观测 / 内部 RPC（Feign 调用，`WebConfig.java:28` 排除鉴权） |
| 40 | POST `/v1/message/groupCheckSend` | `checkGroupSend`（`:64`） | `CheckSendMessageReq` | `null` | 否 | 是 | 观测 / 内部 RPC |
| 41 | POST `/v1/message/syncOfflineMessageList` | `syncP2POfflineMessageList`（`:70`） | `SyncReq`+RB：`lastSequence`、`maxLimit` | `SyncResp<OfflineMessageContent>`：`appId, messageKey, messageBody, messageTime, extra, delFlag, fromId, toId, messageSequence, messageRandom, conversationType, conversationId` | 否（读 Redis ZSet） | 是 | 聊天（离线补拉） |

> `/v1/message/send` 与 WS `MSG_P2P` 走的是两条不同路径：WS 路径有 seq、有离线队列、有 messageId 防重（`P2PMessageService.java:62-125`），REST 路径三者都没有。前端若用 REST 补发会产生 `sequence=null` 的历史行，后续增量同步会漏。

### 1.8 ImUserController — `v1/user`（4 个）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 42 | POST `/v1/user/importUser` | `importUser`（`ImUserController.java:39`） | `ImportUserReq`+RB：`userData[]: ImUserDataEntity`（**未加 @Validated**） | `ImportUserResp{successId[], errorId[]}` | 是，批量 INSERT `im_user_data` | 是（失败进 `errorId`） | 账号初始化 / 观测 |
| 43 | POST `/v1/user/deleteUser` | `deleteUser`（`:44`） | `DeleteUserReq`+RB：`userId[]`（@NotEmpty） | `ImportUserResp{successId[], errorId[]}` | 是，UPDATE `del_flag=1` | 是 | 账号管理 |
| 44 | POST `/v1/user/login` | `login`（`:49`） | `LoginReq`（**不继承 RequestBase**）：`userId`（@NotNull）、`appId`（@NotNull）、`clientType`、`imei`、`operater` | `RouteInfo{ip, port}`（从 ZK 取节点做一致性 hash 路由，`:52-63`） | **否**（`ImUserServiceImpl.login` 直接 `successResponse()`，注释 `// TODO 后期补充鉴权`，`:219-222`） | 是 | 登录（拿长连接地址） |
| 45 | POST `/v1/user/getUserSequence` | `getUserSequence`（`:73`） | `GetUserSequenceReq`+RB：`userId` | `Map<Object,Object>`：Redis hash `appId:SeqPrefix:userId` 全量 + 追加 `GroupSeq`（`ImUserServiceImpl.java:225-233`） | 否 | 是 | 会话列表 / 通讯录启动同步（各业务 maxSeq） |

> `login` 是唯一被排除鉴权的端点（`WebConfig.java:27`），且完全跳过密码校验，只做路由寻址。

### 1.9 ImUserDataController — `v1/user/data`（3 个）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 46 | POST `/v1/user/data/getUserInfo` | `getUserInfo`（`ImUserDataController.java:29`） | `GetUserInfoReq`+RB：`userIds[]`（**未加 @Validated**） | `GetUserInfoResp{userDataItem[]: ImUserDataEntity, failUser[]}` | 否 | 是 | 通讯录 / 聊天（批量头像昵称） |
| 47 | POST `/v1/user/data/getSingleUserInfo` | `getSingleUserInfo`（`:34`） | `UserId`+RB：`userId`（**无校验**） | `ImUserDataEntity`：`userId, nickName, location, birthDay, password, photo, userSex, selfSignature, friendAllowType, disableAddFriend, forbiddenFlag, silentFlag, userType, appId, delFlag, extra` | 否 | 是 | 个人资料 / 聊天头部 |
| 48 | POST `/v1/user/data/modifyUserInfo` | `modifyUserInfo`（`:39`） | `ModifyUserInfoReq`+RB：`userId`（@NotEmpty）、`nickName, location, birthDay, password, photo, userSex(@Deprecated 无值映射), selfSignature, friendAllowType, extra` | `null` | 是，UPDATE `im_user_data` + 推 WS `USER_MODIFY(4000)`（`ImUserServiceImpl.java:166-216`） | 是 | 个人资料 |

> `ModifyUserInfoReq.userSex` 是 `String`，而表字段 `user_sex` 是 `INT`（`im_core.sql:191`），`ImUserDataEntity.userSex` 是 `Integer`。直接传 `"1"` 会因类型不匹配失败/写入异常。

### 1.10 MessageSyncController — `v1/message`（3 个，im-message-store / 18001）

| # | 方法 路径 | Controller#方法 | 入参 DTO 字段 | 返回 data | 写库 | 幂等 | 页面用途 |
|---|---|---|---|---|---|---|---|
| 49 | POST `/v1/message/syncP2PMessage` | `syncP2PMessage`（`MessageSyncController.java:34`） | `SyncP2PMessageReq`：`appId`(@NotNull)、`operater`(@NotBlank)、`clientType`、`imei`、`lastSequence`、`maxLimit`（默认 100）、`userId`、`toId`（可选，不传则跨会话） | `SyncResp<ImMessageHistoryEntity>`：`appId, fromId, toId, ownerId, messageKey, sequence, messageRandom, messageTime, createTime` | 否 | 是 | 聊天（历史增量） |
| 50 | POST `/v1/message/syncGroupMessage` | `syncGroupMessage`（`:45`） | `SyncGroupMessageReq`：`lastSequence`、`maxLimit`（默认 100）、`userId`、`groupId` | `SyncResp<ImGroupMessageHistoryEntity>`：`appId, fromId, groupId, messageKey, sequence, messageRandom, messageTime, createTime` | 否 | 是 | 聊天（群历史增量） |
| 51 | POST `/v1/message/batchSyncMessage` | `batchSyncMessage`（`:56`） | `BatchSyncMessageReq`：`userId`、`conversations[]{conversationId, conversationType(1 单聊/2 群聊), sequence}`、`maxCount`（默认 100） | `BatchSyncMessageResp{conversationList: List<ConversationMessageResp>}`，每项含 `conversationId, conversationType, maxSequence, isCompleted, messages[]` | 否 | 是 | 会话列表 + 聊天（多会话首屏批量拉） |

> **三个同步端点都不返回 `messageBody`**。`ImMessageHistoryEntity` / `ImGroupMessageHistoryEntity` 只有 `messageKey`（`im-message-store/.../dao/ImMessageHistoryEntity.java`），而 `im_message_body` 在整个仓库里**只有 INSERT 没有 SELECT**（`grep -rn "messageBodyMapper"` → 仅 `StoreMessageService.java:51,73` 两处 insert）。前端能拿到消息行，但拿不到消息内容。

---

## 表 2：WS 命令全景（共 49 个）

判定依据：
- 「服务端 strategy」= 是否在 `CommandFactoryConfig.init()` 注册（`im-infrastructure/src/main/java/com/bantanger/im/service/strategy/command/CommandFactoryConfig.java:34-42`）。未注册的命令在 `NettyServerHandler.channelRead0`（`im-tcp/.../handler/NettyServerHandler.java:64-68`）会**被直接丢进 MQ**，不执行任何校验。
- 「产生方」= grep 到 `xxxCommand.YYY` 的实际调用点；无调用点即标「无」。

| command（十进制） | 枚举 | command 值 | 方向 | 服务端 strategy | 产生方 / 消费方 |
|---|---|---|---|---|---|
| `CONVERSATION_DELETE` 5000 | ConversationEventCommand:10 | 0x1388 | S→C | 无 | 产生：`ConversationServiceImpl.java:94`（多端同步删会话） |
| `CONVERSATION_UPDATE` 5001 | ConversationEventCommand:13 | 0x1389 | S→C | 无 | 产生：`ConversationServiceImpl.java:133`（置顶/免打扰多端同步） |
| `FRIEND_ADD` 3000 | FriendshipEventCommand:10 | 3000 | S→C | 无 | 产生：`ImFriendServiceImpl.java`（3 处，加好友成功后通知双方其余端） |
| `FRIEND_UPDATE` 3001 | FriendshipEventCommand:13 | 3001 | S→C | 无 | 产生：`ImFriendServiceImpl.java`（改备注同步） |
| `FRIEND_DELETE` 3002 | FriendshipEventCommand:16 | 3002 | S→C | 无 | 产生：`ImFriendServiceImpl.java` |
| `FRIEND_REQUEST` 3003 | FriendshipEventCommand:19 | 3003 | S→C | 无 | 产生：`ImFriendShipRequestServiceImpl.java`（`sendToUserAllClient` 推给收件人全部端） |
| `FRIEND_REQUEST_READ` 3004 | FriendshipEventCommand:22 | 3004 | S→C | 无 | 产生：`ImFriendShipRequestServiceImpl.java` |
| `FRIEND_REQUEST_APPROVER` 3005 | FriendshipEventCommand:25 | 3005 | S→C | 无 | 产生：`ImFriendShipRequestServiceImpl.java`（审批结果回推申请方） |
| `FRIEND_BLACK_ADD` 3010 | FriendshipEventCommand:28 | 3010 | S→C | 无 | 产生：`ImFriendServiceImpl.java` |
| `FRIEND_BLACK_DELETE` 3011 | FriendshipEventCommand:31 | 3011 | S→C | 无 | 产生：`ImFriendServiceImpl.java` |
| `FRIEND_GROUP_ADD` 3012 | FriendshipEventCommand:34 | 3012 | S→C | 无 | 产生：`ImFriendShipGroupServiceImpl.java:101` |
| `FRIEND_GROUP_DELETE` 3013 | FriendshipEventCommand:37 | 3013 | S→C | 无 | 产生：`ImFriendShipGroupServiceImpl.java` |
| `FRIEND_GROUP_MEMBER_ADD` 3014 | FriendshipEventCommand:40 | 3014 | S→C | 无 | 产生：`ImFriendShipGroupMemberServiceImpl.java:90` |
| `FRIEND_GROUP_MEMBER_DELETE` 3015 | FriendshipEventCommand:43 | 3015 | S→C | 无 | 产生：`ImFriendShipGroupMemberServiceImpl.java:123` |
| `FRIEND_ALL_DELETE` 3016 | FriendshipEventCommand:46 | 3016 | S→C | 无 | 产生：`ImFriendServiceImpl.java`（删全部好友同步） |
| `JOIN_GROUP` 2000 | GroupEventCommand:12 | 0x7d0 | 未实现 | **无** | **无任何产生方/消费方**（全仓库 grep 仅枚举定义） |
| `ADDED_MEMBER` 2001 | GroupEventCommand:17 | 0x7d1 | S→C | 无 | 产生：`GroupMessageProducer.java`、`ImGroupMemberServiceImpl.java`（通知管理员+本人） |
| `CREATED_GROUP` 2002 | GroupEventCommand:22 | 0x7d2 | S→C | 无 | 产生：`ImGroupServiceImpl.java`（通知所有成员） |
| `UPDATED_GROUP` 2003 | GroupEventCommand:27 | 0x7d3 | S→C | 无 | 产生：`ImGroupServiceImpl.java` |
| `EXIT_GROUP` 2004 | GroupEventCommand:32 | 0x7d4 | 未实现 | **无** | **无任何引用**（`RemoveGroupMemberReq.memberId==operater` 时走的是 `DELETED_MEMBER`） |
| `UPDATED_MEMBER` 2005 | GroupEventCommand:37 | 0x7d5 | S→C | 无 | 产生：`GroupMessageProducer.java`、`ImGroupMemberServiceImpl.java` |
| `DELETED_MEMBER` 2006 | GroupEventCommand:42 | 0x7d6 | S→C | 无 | 产生：`GroupMessageProducer.java`、`ImGroupMemberServiceImpl.java` |
| `DESTROY_GROUP` 2007 | GroupEventCommand:47 | 0x7d7 | S→C | 无 | 产生：`ImGroupServiceImpl.java` |
| `TRANSFER_GROUP` 2008 | GroupEventCommand:52 | 0x7d8 | 未实现 | **无** | **无任何引用**（转让群主只写库，不推 WS） |
| `MUTE_GROUP` 2009 | GroupEventCommand:57 | 0x7d9 | 未实现 | **无** | **无任何引用**（全员禁言只写库，不推 WS） |
| `SPEAK_GROUP_MEMBER` 2010 | GroupEventCommand:62 | 0x7da | S→C | 无 | 产生：`ImGroupMemberServiceImpl.java`（单人禁言） |
| `MSG_GROUP` 2104 | GroupEventCommand:67 | 0x838 | **双向** | **有**（`GroupMsgCommand`） | C→S：`CommandFactoryConfig.java:41` → 校验后投 MQ；S→C：`GroupMessageService.java`（`dispatchMessage` 推给所有在线群成员）；消费：`GroupChatOperateReceiver.java:57` |
| `MSG_GROUP_READ` 2106 | GroupEventCommand:72 | 0x83a | C→S | 无（走 MQ） | 消费：`GroupChatOperateReceiver.java:62`（客户端发已读） |
| `MSG_GROUP_READ_NOTIFY` 2053 | GroupEventCommand:77 | 0x805 | S→C | 无 | 产生：`MessageSyncServiceImpl.readMark`（同步给同账号其他端），`GroupChatOperateReceiver.java:67` |
| `MSG_GROUP_READ_RECEIPT` 2054 | GroupEventCommand:82 | 0x806 | S→C | 无 | 产生：`MessageSyncServiceImpl.readMark`（回执给原消息发送方），`GroupChatOperateReceiver.java:68` |
| `GROUP_MSG_ACK` 2047 | GroupEventCommand:87 | 0x7ff | S→C | 无 | 产生：`GroupMsgCommand.java:53`（校验失败）、`GroupMessageService.java:201`（成功 ACK） |
| `MSG_P2P` 1103 | MessageCommand:10 | 0x44F | **双向** | **有**（`P2PMsgCommand`） | C→S：`CommandFactoryConfig.java:40` → 校验后投 MQ；S→C：`P2PMessageService.syncToSender` / `dispatchMessage`（`:240,254`）；消费：`P2PChatOperateReceiver.java:58` |
| `MSG_ACK` 1046 | MessageCommand:13 | 0x416 | S→C | 无 | 产生：`P2PMsgCommand.java:53`（校验失败）、`P2PMessageService.ack`（`:207` 成功回执） |
| `MSG_RECEIVE_ACK` 1107 | MessageCommand:16 | 0x453 | **双向** | 无 | C→S：`P2PChatOperateReceiver.java:63` 消费客户端回执；S→C：`MessageSyncServiceImpl.java:52`（推给目标全端）、`P2PMessageService.java:225`（代离线用户回 ACK） |
| `MSG_READ` 1106 | MessageCommand:19 | 0x452 | C→S | 无（走 MQ） | 消费：`P2PChatOperateReceiver.java:68` |
| `MSG_READ_NOTIFY` 1053 | MessageCommand:22 | 0x41D | S→C | 无 | 产生：`MessageSyncServiceImpl.readMark`，`P2PChatOperateReceiver.java:73` |
| `MSG_READ_RECEIPT` 1054 | MessageCommand:25 | 0x41E | S→C | 无 | 产生：同上，`P2PChatOperateReceiver.java:73` |
| `MSG_RECALL` 1050 | MessageCommand:28 | 0x41A | 未实现 | **无** | **无任何引用**；`im-codec/.../pack/` 下也没有 RecallPack |
| `MSG_RECALL_NOTIFY` 1052 | MessageCommand:31 | 0x41C | 未实现 | **无** | **无任何引用** |
| `MSG_RECALL_ACK` 1051 | MessageCommand:34 | 0x41B | 未实现 | **无** | **无任何引用** |
| `PING` 9999 | SystemCommand:11 | 0x270f | C→S | **有**（`PingCommand`） | `CommandFactoryConfig.java:38`；仅刷新 channel `ReadTime` 属性 |
| `LOGIN` 9000 | SystemCommand:15 | 0x2328 | C→S | **有**（`LoginCommand`） | `CommandFactoryConfig.java:36`；绑定 channel + 写 Redis session + pub/sub 广播上线 |
| `LOGOUT` 9003 | SystemCommand:19 | 0x232b | C→S | **有**（`LogoutCommand`） | `CommandFactoryConfig.java:37`；解绑 channel |
| `MUTALOGIN` 9002 | SystemCommand:23 | 0x232a | S→C | 无 | 产生：`LoginStatus.sendMutualLoginMsg`（`LoginStatus.java:60`，多端互斥踢下线） |
| `SENDMSG` 9005 | SystemCommand:26 | 0x232d | 未实现 | **无** | **无任何引用**（消息发送实际走 `MSG_P2P` / `MSG_GROUP`） |
| `USER_MODIFY` 4000 | UserEventCommand:10 | 0xfa0 | S→C | 无 | 产生：`ImUserServiceImpl.java:204`（改资料后同步多端） |
| `USER_ONLINE_STATUS_CHANGE` 4001 | UserEventCommand:13 | 0xfa1 | 未实现 | **无** | **无任何引用** |
| `USER_ONLINE_STATUS_CHANGE_NOTIFY` 4004 | UserEventCommand:16 | 0xfa4 | 未实现 | **无** | **无任何引用** |
| `USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC` 4005 | UserEventCommand:19 | 0xfa5 | 未实现 | **无** | **无任何引用** |

### 2.1 WS 帧格式（前端必须按这个编码）

| 方向 | 结构 | 证据 |
|---|---|---|
| 客户端 → 服务端 | 28 字节定长头 + imei + body：`command(4) version(4) clientType(4) messageType(4) appId(4) imeiLength(4) bodyLen(4)` | `im-codec/.../utils/ByteBufToMessageUtils.java:25,30-48`（`PACKET_CODEC_LENGTH = 28`） |
| 服务端 → 客户端 | 仅 8 字节头 + JSON：`command(4) length(4) + JSON.stringify(MessagePack)` | `im-codec/.../WebSocketMessageEncoderHandler.java:27-33` |

**收发格式不对称**：前端必须手工拼 28 字节头发送，接收端解析 8 字节头。`MessagePack` 出参字段为 `userId, appId, toId, clientType, messageId, imei, command, data`（`im-codec/.../proto/MessagePack.java`）。

> `WebSocketMessageDecoderHandler` 直接复用 TCP 的 `ByteBufToMessageUtils.transition`，`imeiLength` 是必填位；前端若按「command+length+json」自己编码，会被 `< PACKET_CODEC_LENGTH` 判断直接 `close()`（`WebSocketMessageDecoderHandler.java:27-29`）。

---

## 表 3：数据模型（`assert/sql/im_core.sql`，实为 12 张表）

| # | 表名 | 主键 | 唯一约束 | 二级索引 | 前端会用到的字段 |
|---|---|---|---|---|---|
| 1 | `app_user`（`:4`） | `user_id` VARCHAR(20) | — | **无** | `user_id, user_name, password, mobile, create_time, update_time` |
| 2 | `im_conversation_set`（`:15`） | `(app_id, conversation_id)` | — | **无** | `conversation_id, conversation_type(0 单聊/1 群聊/2 机器人/3 公众号), from_id, to_id, is_mute, is_top, sequence, read_sequence, app_id` |
| 3 | `im_friendship`（`:29`） | `(app_id, from_id, to_id)` | — | **无** | `remark, status(1 正常/2 删除), black(1 正常/2 拉黑), create_time, friend_sequence, black_sequence, add_source, extra` |
| 4 | `im_friendship_group`（`:52`） | `group_id` INT AUTO_INCREMENT | `UNIQUE(app_id, from_id, group_name)`（`:63-64`） | 无 | `app_id, from_id, group_id, group_name, sequence, create_time, update_time, del_flag` |
| 5 | `im_friendship_group_member`（`:67`） | **`group_id` BIGINT（单列）** | — | **无** | `group_id, to_id` |
| 6 | `im_friendship_request`（`:74`） | `id` INT AUTO_INCREMENT | — | **无** | `app_id, from_id, to_id, remark, read_status, add_source, add_wording, approve_status(1 同意/2 拒绝), create_time, update_time, sequence` |
| 7 | `im_group`（`:93`） | `(APP_ID, GROUP_ID)` | — | **无** | `owner_id, group_type(1 私有/2 公开), group_name, mute, apply_join_type(0 禁入/1 需审批/2 自由加入), photo, max_member_count, introduction, notification, status(0 正常/1 解散), sequence, create_time, extra, update_time` |
| 8 | `im_group_member`（`:121`） | `group_member_id` BIGINT AUTO_INCREMENT | — | **无** | `group_id, app_id, member_id, role(0 普通/1 管理员/2 群主/3 禁言/4 已移除), speak_date, mute, alias, join_time, leave_time, join_type, extra` |
| 9 | `im_group_message_history`（`:144`） | `(app_id, group_id, message_key)` | — | **无** | `from_id, message_key, create_time, sequence, message_random, message_time` |
| 10 | `im_message_body`（`:157`） | `message_key` BIGINT | — | **无** | `app_id, message_key, message_body VARCHAR(5000), security_key, message_time, create_time, extra, del_flag` |
| 11 | `im_message_history`（`:170`） | `(app_id, owner_id, message_key)` | — | **无** | `from_id, to_id, owner_id, message_key, create_time, sequence, message_random, message_time` |
| 12 | `im_user_data`（`:184`） | `(app_id, user_id)` | — | **无** | `nick_name, photo, user_sex, birth_day, location, self_signature, friend_allow_type(1 无需验证/2 需验证), forbidden_flag, disable_add_friend, silent_flag, user_type(1 普通/2 客服/3 机器人), del_flag, extra` |

索引结论：
- 12 张表**全部没有二级索引**，`grep -n "KEY\|INDEX" assert/sql/im_core.sql` 只命中文中出现的 `PRIMARY KEY` 与 `im_friendship_group` 的那一个 `UNIQUE`。
- 索引缺失直接打穿的查询：`im_group_member` 按 `(app_id, member_id)` 查「我加入的群」（`ImGroupMemberMapper.xml` 5 条 SQL 全部按 `app_id + member_id` 或 `app_id + group_id` 过滤）；`im_message_history` 按 `(app_id, owner_id, to_id, sequence)` 范围扫（`MessageSyncServiceImpl.java:181-187`）；`im_conversation_set` 按 `(app_id, from_id)` 扫（`ImConversationSetMapper.xml` `getConversationSetMaxSeq`）。
- `im_friendship_group_member` 主键是 `group_id` 单列 → **一个好友分组物理上只能挂 1 个成员**，第 2 个成员 INSERT 必撞主键，被 `ImFriendShipGroupMemberServiceImpl.doAddGroupMember` 的 catch 吞成 `return 0`（`:130-141`），表现为 `errorId`。
- `app_user` 与 `im_user_data` 两套账号表并存，`app_user` 无任何 mapper 引用（`grep -rn "app_user"` 仅在 SQL 文件中），实际登录走 `im_user_data`。

### 表 4：前端会用到、但表里没有的字段

| 前端需求 | 缺失字段 / 缺失结构 | 证据 |
|---|---|---|
| 会话列表「最后一条消息预览」 | `im_conversation_set` 无 `last_message` / `last_message_time` / `last_from_id`；唯一能排序的是 `sequence`（会话级自增），不含内容 | `assert/sql/im_core.sql:15-27`；`ImConversationSetEntity.java` 字段清单 |
| 会话列表「未读数」 | 只有 `read_sequence`（会话内已读偏序），没有 `unread_count`；且 `sequence` 列被两套语义复用（会话排序 seq 与消息 seq 同列覆盖写，`ConversationServiceImpl.java:121`） | `im_core.sql:23-24`；`ImConversationSetMapper.xml` `readMark` 同时写 `sequence` 和 `read_sequence` |
| 消息气泡「消息类型」（文本/图片/语音/视频/文件/位置） | 无 `msg_type` / `content_type` 列，`message_body` 是裸 `VARCHAR(5000)`；`MessageType` 枚举只管 JSON/PB/XML 编码格式，与业务消息类型无关 | `im_core.sql:162`；`im-common/.../enums/MessageType.java:11-13` |
| 消息气泡「已撤回」态 | 只有 `del_flag`（`im_message_body:167`），无 `recall_status` / `recall_time` / `recall_operater` | `im_core.sql:157-168` |
| 消息气泡「发送状态」（发送中/失败/已送达） | 服务端有 `MSG_ACK`/`MSG_RECEIVE_ACK` 但**不落库**，无 `send_status` 列 | `P2PMessageService.ack`、`receiveAckByServer`（内存态仅推 WS） |
| 群成员「在线状态」 | 无 `online_status` 列；`UserOnlineStatusResp` DTO 存在但 `USER_ONLINE_STATUS_*` 三个 command 全未实现 | `im_core.sql:121-136`；`UserEventCommand.java:13-19` 无引用 |
| 群成员「邀请人 / 入群审批人」 | `join_type` 是自由字符串，无 `inviter_id` / `approver_id` 列 | `im_core.sql:134` |
| 好友「在线状态 / 最近活跃」 | `im_friendship` 无 `last_active_time`；无任何 online 状态表 | `im_core.sql:29-43` |
| 用户「头像裁剪尺寸 / 多规格 URL」 | 只有单列 `photo VARCHAR(255)` | `im_core.sql:190` |
| 用户「注册时间」 | `im_user_data` 无 `create_time` / `update_time`（`app_user` 有但无人用） | `im_core.sql:184-203` vs `:4-13` |
| 好友分组「成员数 / 成员列表」 | `im_friendship_group` 无 `member_count`；`im_friendship_group_member` 只有 `group_id + to_id` 且主键约束只允许 1 条 | `im_core.sql:52-72` |
| 群「群主昵称头像」 | `im_group` 只有 `owner_id`，需二次 JOIN `im_user_data`（前端得自己调 `getUserInfo`） | `im_core.sql:97` |

---

## 表 5：缺口清单（显式缺失能力）

| # | 缺失能力 | 证据（事实） | 影响页面 |
|---|---|---|---|
| G1 | **消息内容不可取回** | `im_message_body` 全仓库只有 INSERT（`im-message-store/.../StoreMessageService.java:51,73`），三个同步端点返回的 `ImMessageHistoryEntity` 无 `messageBody` 字段。前端能拿 `messageKey`，但没有按 key 取 body 的接口 | 聊天（致命） |
| G2 | **媒体消息（图片/语音/视频/文件）** | ① `message_body` 是纯字符串，无类型列；② 全仓库 `grep -i "image\|voice\|video\|file_url\|media"` 在 `im-common`/`im-domain`/`im-message-store`/`im-codec` 下**零命中**；③ 无对象存储/上传凭证端点；④ 无缩略图/时长/宽高字段 | 聊天 |
| G3 | **消息撤回** | `MessageCommand.MSG_RECALL(0x41A)` / `MSG_RECALL_NOTIFY(0x41C)` / `MSG_RECALL_ACK(0x41B)` 三个枚举存在（`MessageCommand.java:28,31,34`），但全仓库无引用、无 strategy、`im-codec/.../pack/` 下无 RecallPack、无 REST 端点 | 聊天 |
| G4 | **消息搜索** | 全仓库 `grep "\.like(\|keyword\|search"` 在 `im-domain`/`im-message-store`/`im-infrastructure` 下**零命中**；无全文索引、无 ES 依赖（`pom.xml` 7 个 module 无 ES 客户端） | 聊天 / 会话列表 |
| G5 | **入群审批** | `im_group.apply_join_type` 支持「1 需群主或管理员审批」（`im_core.sql:101-104`），但：① `GroupEventCommand.JOIN_GROUP(0x7d0)` 无任何引用；② 无 `im_group_join_request` 表（12 张表里没有）；③ 无 apply/approve REST 端点（`ImGroupController` 10 个端点全是管理动作）；④ 也无「主动退群」端点（`EXIT_GROUP` 同样零引用） | 群管理 |
| G6 | **机器人账号** | `im_user_data.user_type` 注释含「3 机器人」（`im_core.sql:199`），`im_conversation_set.conversation_type` 注释含「2 机器人 / 3 公众号」（`:18`），但：无 bot 注册/配置端点、无 `@机器人` 消息路由、无欢迎语/自动回复表、无 bot 专属 command | 聊天 / 群管理 |
| G7 | **鉴权** | ① `GateWayInterceptor.preHandle` 首行 `if (true) { return true; }`（`im-domain/.../interceptor/GateWayInterceptor.java:32-34`），签名校验整体短路；② `ImUserServiceImpl.login` 注释 `// TODO 后期补充鉴权`，直接 `successResponse()`（`:219-222`）；③ 拦截器从 `request.getParameter(...)` 取 `appId/identifier/userSign`（`:36-50`），而全部端点用 `@RequestBody`，即使打开也取不到；④ `spring-boot-starter-security` 未引入（`pom.xml`） | 全部 |
| G8 | **群成员列表 / 好友分组列表 / 好友申请分页**（查询类端点缺失） | `getGroupMember` 只在 `getGroupInfo` 内部调用（`ImGroupMemberServiceImpl.java:417`）；好友分组只有 add/del/member-add/member-del 四个写端点，无任何查询；`getFriendRequest` 无分页无状态过滤（`ImFriendShipRequestServiceImpl.java:48-56`） | 群管理 / 通讯录 / 好友申请 |
| G9 | **已读回执的落库** | `MSG_READ` → `readMark` 只更新 `im_conversation_set.read_sequence`（`ImConversationSetMapper.xml`），不写消息级已读明细表；群聊「谁已读」无从查询 | 聊天 |
| G10 | **消息引用 / 回复、@提醒、表情包、富文本** | `im_message_body` 只有 `message_body` + `extra` 两个自由文本列，无 `quote_message_key` / `mention_ids` / `at_type`；无相关 command | 聊天 |
| G11 | **多端 session 查询接口** | `UserOnlineStatusResp{session[], customText, customStatus}` DTO 存在（`im-domain/.../user/model/resp/UserOnlineStatusResp.java`），但无端点引用它；`USER_ONLINE_STATUS_*` 三个 command 全未实现 | 观测 / 个人设置 |
| G12 | **客服台专属能力** | `user_type=2 客服` 只是枚举值，无排队/接待/转接/评价/工单表与端点；`im-domain` 顶层只有 `config / interceptor / conversation / friendship / group / message / user` 七个包，无客服或机器人相关包 | 客服台（整体缺失） |
| G13 | **观测类端点** | 51 个端点中只有 `p2pCheckSend`/`groupCheckSend` 带「内部校验」语义，且被鉴权排除；无健康检查、无指标、无 traceId 透传 | 观测 |
| G14 | **好友分组表结构缺陷** | `im_friendship_group_member` 主键 `group_id` 单列（`im_core.sql:69-70`），一个分组只能存一个成员 | 通讯录 |
| G15 | **两条 SQL 语法错误** | `ImFriendShipMapper.xml` 的 `checkFriendShipBoth` / `checkFriendShipBlackBoth` 中 `where app_id = #{appId} from_id = #{fromId}` 缺少 `and`，必然抛 SQL 语法异常 | 通讯录 / 黑名单 |

---

## 表 6：结论

### 6.1 前端可以直接消费的能力（后端已闭环）

| 能力 | 端点 / command | 闭环程度 |
|---|---|---|
| 登录取长连接地址 | `POST /v1/user/login` → `{ip, port}` + WS `LOGIN(0x2328)` | 完整（无鉴权，但可用） |
| 长连接保活 / 多端互斥 | WS `PING(0x270f)` / `LOGOUT(0x232b)` / `MUTALOGIN(0x232a)` | 完整 |
| 会话列表（元数据） | `POST /v1/conversation/syncConversationList` + `updateConversation` | 完整（但无最后消息/未读数，见 表 4） |
| 通讯录 CRUD | `/v1/friendship/*` 12 个端点中 10 个（排除 `checkFriend`/`checkBlck` 的非单向分支）+ `/v1/user/data/getUserInfo` | 完整 |
| 好友申请流 | `/v1/friendshipRequest/*` 3 个端点 + WS `FRIEND_REQUEST(3003)` / `FRIEND_REQUEST_APPROVER(3005)` / `FRIEND_REQUEST_READ(3004)` | 完整（缺分页） |
| 黑名单 | `addBlack` / `deleteBlack` / `checkBlck(checkType=1)` + WS 3010/3011 | 完整 |
| 群基础管理 | `/v1/group/*` 10 个端点（建群/改群/解散/转让/禁言/我已加入群）+ `/v1/group/member/*` 5 个 | 完整（缺成员列表端点与入群审批） |
| 群事件实时推送 | WS 2001/2002/2003/2005/2006/2007/2010 | 完整（2000/2004/2008/2009 未实现） |
| 单聊/群聊发消息（在线路径） | WS `MSG_P2P(0x44F)` / `MSG_GROUP(0x838)` + `MSG_ACK` / `GROUP_MSG_ACK` | 发送链路闭环，**但消息内容不可取回** |
| 已读状态 | WS `MSG_READ` / `MSG_GROUP_READ` → `*_READ_NOTIFY` / `*_READ_RECEIPT` | 实时闭环（不落细则，重启后不可查） |
| 离线补拉 | `POST /v1/message/syncOfflineMessageList`（Redis ZSet，含 `messageBody`） | 完整 —— **唯一能拿到 `messageBody` 的读路径** |
| 历史增量 | `POST /v1/message/syncP2PMessage` / `syncGroupMessage` / `batchSyncMessage` | 结构完整，**不含 body** |
| 用户资料 | `getUserInfo` / `getSingleUserInfo` / `modifyUserInfo` + WS `USER_MODIFY(0xfa0)` | 完整（`userSex` 类型不匹配需绕） |

### 6.2 必须先补后端的能力（前端无法绕）

| 优先级 | 能力 | 最小补齐前提 |
|---|---|---|
| P0 | 取回消息正文 | 新增「按 `messageKey` 批量查 `im_message_body`」端点，或在 `syncP2PMessage` / `syncGroupMessage` / `batchSyncMessage` 返回体里 JOIN `im_message_body` |
| P0 | 消息类型（文本/图片/语音/视频/文件） | `im_message_body` 加 `msg_type` 列 + 媒体上传端点（对象存储直传凭证）；否则聊天框只能渲染纯文本 |
| P0 | 媒体存储 | 无任何上传/下载端点，`grep -i media` 零命中 |
| P0 | 鉴权 | 打开 `GateWayInterceptor`（去掉 `:32` 的 `if (true)`），且拦截器取值需从 query 参数改为读取 Header 或 Body；`login` 需补真实校验 |
| P1 | 会话列表「最后消息 + 未读数」 | `im_conversation_set` 加 `last_message_key` / `unread_count`（或新增汇总视图），否则首屏必须 N 次调 `syncP2PMessage` |
| P1 | 消息撤回 | `MSG_RECALL` 全链路：codec pack + strategy/service + `im_message_body.recall_status` 列 + REST 端点 |
| P1 | 群成员列表端点 | 把已有的 `ImGroupMemberService.getGroupMember` 暴露成 REST（带分页），避免 `getGroupInfo` 全量带成员 |
| P1 | 入群审批 | 新建 `im_group_join_request` 表 + apply/approve/list 端点 + 启用 `JOIN_GROUP(0x7d0)` |
| P2 | 消息搜索 | 无 `LIKE`/ES 任何基础设施，需新建（建议独立检索服务，不要在主库加 `LIKE %kw%`） |
| P2 | 机器人账号 | `user_type=3` 仅是枚举；需 bot 注册端点 + 消息路由 + 自动回复配置 |
| P2 | 客服台 | 整体缺失：无排队/接待/转接/评价/工单模型与端点 |
| P2 | 观测 | 无健康检查、无指标端点、无 traceId 透传（`application.yml` 的 access log pattern 里预留了 `%{x-request-id}r`，但代码未写入） |
| P3 | 缺陷修复 | `im_friendship_group_member` 主键改 `(group_id, to_id)`；`ImFriendShipMapper.xml` 两处补 `and`；`ModifyUserInfoReq.userSex` 改 `Integer`；`syncConversationSet` 的 `ge`→`gt` 与 `maxLimit` 空指针 |

### 6.3 前端接入时必须知道的三条硬约束

1. **WS 只有 BinaryWebSocketFrame，且收发帧格式不一致**（表 2.1）。发送必须手工拼 28 字节头；不能发 text frame。
2. **`v1/message` 前缀横跨两个端口**：`/v1/message/send|p2pCheckSend|groupCheckSend|syncOfflineMessageList` 在 18000，`/v1/message/syncP2PMessage|syncGroupMessage|batchSyncMessage` 在 18001。同一个 baseURL 直接调会 404。
3. **`syncConversationList` / `syncJoinedGroup` 的 `maxLimit` 必须显式传**（两处均为 `req.getMaxLimit()` 直接拆箱比较，null 时 NPE）。

---

## 计数核对

| 指标 | 数值 | 核对方式 |
|---|---|---|
| REST 端点总数 | **51** | `grep -c "public ResponseVO" im-domain/src/main/java/com/bantanger/im/domain/*/controller/*.java im-message-store/src/main/java/com/bantanger/im/message/controller/*.java` → 3+12+4+3+10+5+4+4+3+3 = 51 |
| Controller 类数 | 10 | `find ... -path "*controller*" -name "*.java"` → im-domain 9 + im-message-store 1 |
| WS command 枚举总数 | **49** | `grep -cE "^\s{4}[A-Z][A-Z0-9_]*\("` 于 6 个命令枚举 → Conversation 2 + Friendship 13 + Group 16 + Message 9 + System 5 + User 4 = 49（`CommandType` 的 4 个不算 command） |
| 有服务端 strategy 的 command | 5 | `CommandFactoryConfig.java:36-41`：LOGIN / LOGOUT / PING / MSG_P2P / MSG_GROUP |
| 无任何引用的 command | 11 | JOIN_GROUP、EXIT_GROUP、TRANSFER_GROUP、MUTE_GROUP、MSG_RECALL、MSG_RECALL_NOTIFY、MSG_RECALL_ACK、SENDMSG、USER_ONLINE_STATUS_CHANGE、USER_ONLINE_STATUS_CHANGE_NOTIFY、USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC |
| 数据表数量 | **12** | `grep -c "CREATE TABLE" assert/sql/im_core.sql` → 12（任务说明中的「11 张」与实际不符，实际 12 张） |
| 二级索引数量 | 0 | `grep -n "KEY\|INDEX" assert/sql/im_core.sql` 仅命中 `PRIMARY KEY` 与 1 处 `constraint UNIQUE` |

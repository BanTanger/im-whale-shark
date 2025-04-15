CREATE DATABASE IF NOT EXISTS im_core;
USE im_core;

CREATE TABLE app_user
(
    user_id     VARCHAR(20)  NOT NULL
        PRIMARY KEY COMMENT '用户唯一标识',
    user_name   VARCHAR(255) NULL COMMENT '用户名',
    password    VARCHAR(255) NULL COMMENT '用户密码',
    mobile      VARCHAR(255) NULL COMMENT '用户手机号',
    create_time BIGINT       NULL COMMENT '创建时间，时间戳格式',
    update_time BIGINT       NULL COMMENT '更新时间，时间戳格式'
);

CREATE TABLE im_conversation_set(
    conversation_id   VARCHAR(255) NOT NULL COMMENT '会话唯一标识',
    conversation_type INT(10)      NOT NULL DEFAULT 0 COMMENT '会话类型：0 单聊，1 群聊，2 机器人，3 公众号',
    from_id           VARCHAR(50)  NOT NULL COMMENT '会话发起方 ID',
    to_id             VARCHAR(50)  NOT NULL COMMENT '会话接收方 ID',
    is_mute           INT(10)      NOT NULL DEFAULT 0 COMMENT '是否免打扰：0 否，1 是',
    is_top            INT(10)      NOT NULL DEFAULT 0 COMMENT '是否置顶：0 否，1 是',
    sequence          BIGINT       NOT NULL DEFAULT 0 COMMENT '会话序列号，用于排序',
    read_sequence     BIGINT       NOT NULL DEFAULT 0 COMMENT '已读最大消息序列号',
    app_id            INT(10)      NOT NULL COMMENT '应用 ID',
    PRIMARY KEY (app_id, conversation_id)
) ENGINE = InnoDB COMMENT = '会话表';

INSERT INTO im_core.im_conversation_set (conversation_id, conversation_type, from_id, to_id, is_mute, is_top, sequence, read_sequence, app_id) VALUES ('0_10001_10002', 0, '10001', '10002', 0, 0, 0, 0, 10001);
INSERT INTO im_core.im_conversation_set (conversation_id, conversation_type, from_id, to_id, is_mute, is_top, sequence, read_sequence, app_id) VALUES ('0_bantanger_10001', 0, 'bantanger', '10001', 0, 0, 0, 0, 10001);
INSERT INTO im_core.im_conversation_set (conversation_id, conversation_type, from_id, to_id, is_mute, is_top, sequence, read_sequence, app_id) VALUES ('0_bantanger_10002', 0, 'bantanger', '10002', 0, 0, 0, 0, 10001);
INSERT INTO im_core.im_conversation_set (conversation_id, conversation_type, from_id, to_id, is_mute, is_top, sequence, read_sequence, app_id) VALUES ('1_10001_27a35ff2f9be4cc9a8d3db1ad3322804', 1, '10001', '27a35ff2f9be4cc9a8d3db1ad3322804', 0, 0, 0, 0, 10001);
INSERT INTO im_core.im_conversation_set (conversation_id, conversation_type, from_id, to_id, is_mute, is_top, sequence, read_sequence, app_id) VALUES ('1_bantanger_27a35ff2f9be4cc9a8d3db1ad3322804', 1, 'bantanger', '27a35ff2f9be4cc9a8d3db1ad3322804', 0, 0, 0, 0, 10001);
INSERT INTO im_core.im_conversation_set (conversation_id, conversation_type, from_id, to_id, is_mute, is_top, sequence, read_sequence, app_id) VALUES ('1_10002_27a35ff2f9be4cc9a8d3db1ad3322804', 1, '10002', '27a35ff2f9be4cc9a8d3db1ad3322804', 0, 0, 0, 0, 10001);

CREATE TABLE im_friendship
(
    app_id          INT(20)       NOT NULL COMMENT '应用 ID',
    from_id         VARCHAR(50)   NOT NULL COMMENT '发起方用户 ID',
    to_id           VARCHAR(50)   NOT NULL COMMENT '接收方用户 ID',
    remark          VARCHAR(50)   NULL COMMENT '好友备注',
    status          INT(10)       NOT NULL DEFAULT 1 COMMENT '好友状态：1 正常，2 删除',
    black           INT(10)       NOT NULL DEFAULT 1 COMMENT '黑名单状态：1 正常，2 拉黑',
    create_time     BIGINT        NULL COMMENT '创建时间，时间戳格式',
    friend_sequence BIGINT        NOT NULL DEFAULT 1 COMMENT '好友申请序列号，用于排序',
    black_sequence  BIGINT        NOT NULL DEFAULT 0 COMMENT '黑名单序列号，用于排序',
    add_source      VARCHAR(20)   NULL COMMENT '添加好友来源',
    extra           VARCHAR(1000) NULL COMMENT '额外信息',
    PRIMARY KEY (app_id, from_id, to_id)
);

INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10001', '10002', 'minim tempor', 1, 1, 1680608016816, 1, 0, '二维码', '二维码扫描添加');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10002', '10001', 'minim tempor', 1, 1, 1680608016850, 1, 0, '二维码', '二维码扫描添加');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10001', 'bantanger', 'minim tempor', 1, 1, 1680608016850, 1, 0, '通讯录', '我是 xxx，一起认识一下吧');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, 'bantanger', '10001', 'minim tempor', 1, 1, 1680608016850, 1, 0, '通讯录', '我是 xxx，一起认识一下吧');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10002', 'bantanger', 'minim tempor', 1, 1, 1681024983443, 1, 0, 'ID搜索', '你好呀');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, 'bantanger', '10002', 'minim tempor', 1, 1, 1681024987165, 1, 0, 'ID搜索', '你好呀');

CREATE TABLE im_friendship_group
(
    app_id      INT(20)     NOT NULL COMMENT '应用 ID',
    from_id     VARCHAR(50) NOT NULL COMMENT '用户 ID',
    group_id    INT(50) AUTO_INCREMENT
        PRIMARY KEY COMMENT '分组唯一标识',
    group_name  VARCHAR(50) NOT NULL DEFAULT '默认分组' COMMENT '好友分组名称',
    sequence    BIGINT      NOT NULL DEFAULT 0 COMMENT '分组序列号，用于排序',
    create_time BIGINT      NULL COMMENT '创建时间，时间戳格式',
    update_time BIGINT      NULL COMMENT '更新时间，时间戳格式',
    del_flag    INT(10)     NOT NULL DEFAULT 0 COMMENT '删除标识：0 未删除，1 已删除',
    CONSTRAINT `UNIQUE`
        UNIQUE (app_id, from_id, group_name)
) COMMENT '好友分组表';

CREATE TABLE im_friendship_group_member
(
    group_id INT(50) AUTO_INCREMENT
        PRIMARY KEY COMMENT '分组 ID',
    to_id    VARCHAR(50) NULL COMMENT '好友 ID'
) COMMENT '好友分组成员表';

CREATE TABLE im_friendship_request
(
    id             INT(20) AUTO_INCREMENT COMMENT '请求唯一标识'
        PRIMARY KEY,
    app_id         INT(20)     NULL COMMENT '应用 ID',
    from_id        VARCHAR(50) NULL COMMENT '发起方用户 ID',
    to_id          VARCHAR(50) NULL COMMENT '接收方用户 ID',
    remark         VARCHAR(50) NULL COMMENT '备注信息',
    read_status    INT(10)     NULL COMMENT '是否已读：0 未读，1 已读',
    add_source     VARCHAR(20) NULL COMMENT '好友来源',
    add_wording    VARCHAR(50) NULL COMMENT '好友验证信息',
    approve_status INT(10)     NULL COMMENT '审批状态：1 同意，2 拒绝',
    create_time    BIGINT      NULL COMMENT '创建时间，时间戳格式',
    update_time    BIGINT      NULL COMMENT '更新时间，时间戳格式',
    sequence       BIGINT      NULL COMMENT '请求序列号，用于排序'
);

INSERT INTO im_core.im_friendship_request (id, app_id, from_id, to_id, remark, read_status, add_source, add_wording, approve_status, create_time, update_time, sequence) VALUES (1, 10001, '10003', 'bantanger', 'minim tempor', 0, 'cillum', 'ipsum id', 0, 1681025483768, null, 1);

CREATE TABLE im_group
(
    app_id           INT(20)       NOT NULL COMMENT '应用 ID',
    group_id         VARCHAR(50)   NOT NULL COMMENT '群组唯一标识',
    owner_id         VARCHAR(50)   NOT NULL COMMENT '群主用户 ID',
    group_type       INT(10)       NULL COMMENT '群组类型：1 私有群，2 公开群',
    group_name       VARCHAR(100)  NULL COMMENT '群组名称',
    mute             INT(10)       NULL COMMENT '是否全员禁言：0 否，1 是',
    apply_join_type  INT(10)       NULL COMMENT '加入群组方式：0 禁止申请，1 需审批，2 自由加入',
    photo            VARCHAR(300)  NULL COMMENT '群组头像 URL',
    max_member_count INT(20)       NULL COMMENT '最大成员数量',
    introduction     VARCHAR(100)  NULL COMMENT '群组简介',
    notification     VARCHAR(1000) NULL COMMENT '群组公告',
    status           INT(5)        NULL COMMENT '群组状态：0 正常，1 解散',
    sequence         BIGINT        NULL COMMENT '群组序列号，用于排序',
    create_time      BIGINT        NULL COMMENT '创建时间，时间戳格式',
    update_time      BIGINT        NULL COMMENT '更新时间，时间戳格式',
    extra            VARCHAR(1000) NULL COMMENT '额外信息',
    PRIMARY KEY (APP_ID, GROUP_ID)
);

INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '158e4d1df28d42ba9c23bf0d47e03be1', 'bantanger', 1, 'Java 后端必胜组', 0, 2, 'http://dummyimage.com/400x400', null, 'laborum irure minim', 'qui ipsum ut tempor', 1, 1, 1681028052204, 'ullamco consequat pariatur', null);
INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '27a35ff2f9be4cc9a8d3db1ad3322804', 'bantanger', 1, '半糖的IM小屋', 0, 2, 'http://dummyimage.com/400x400', 200, '半糖的IM聊天室小屋', '大家好，我是 BanTanger 半糖 ', 0, null, 1680055132161, 'aliquip esse eiusmod Duis ullamco', null);
INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '5ce492e515a346acbf316233d303e213', 'bantanger', 1, 'Java 后端必胜组2', 0, 2, 'http://dummyimage.com/400x400', null, 'laborum irure minim', 'qui ipsum ut tempor', 1, 2, 1681028156991, 'ullamco consequat pariatur', null);

CREATE TABLE im_group_member
(
    group_member_id BIGINT AUTO_INCREMENT
        PRIMARY KEY COMMENT '群成员唯一标识',
    group_id        VARCHAR(50)   NOT NULL COMMENT '群组 ID',
    app_id          INT(10)       NULL COMMENT '应用 ID',
    member_id       VARCHAR(50)   NOT NULL COMMENT '成员用户 ID',
    role            INT(10)       NULL COMMENT '成员角色：0 普通成员，1 管理员，2 群主，3 禁言，4 已移除',
    speak_date      BIGINT(100)   NULL COMMENT '禁言结束时间，时间戳格式',
    mute            INT(10)       NULL COMMENT '是否禁言：0 否，1 是',
    alias           VARCHAR(100)  NULL COMMENT '群昵称',
    join_time       BIGINT        NULL COMMENT '加入时间，时间戳格式',
    leave_time      BIGINT        NULL COMMENT '离开时间，时间戳格式',
    join_type       VARCHAR(50)   NULL COMMENT '加入方式',
    extra           VARCHAR(1000) NULL COMMENT '额外信息'
);

INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (3, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, '10001', 1, null, null, null, 1679400643080, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (4, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, '10002', 0, null, null, null, 1679400643080, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (5, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, 'bantanger', 2, 20200411, null, 'sit reprehenderit', 1681471124554, null, 'id', null);

CREATE TABLE im_group_message_history
(
    app_id         INT(20)     NOT NULL COMMENT '应用 ID',
    from_id        VARCHAR(50) NOT NULL COMMENT '发消息用户 ID',
    group_id       VARCHAR(50) NOT NULL COMMENT '群组 ID',
    message_key    BIGINT(50)  NOT NULL COMMENT '消息唯一标识',
    create_time    BIGINT      NULL COMMENT '消息创建时间，时间戳格式',
    sequence       BIGINT      NULL COMMENT '消息序列号，用于排序',
    message_random INT(20)     NULL COMMENT '消息随机数',
    message_time   BIGINT      NULL COMMENT '消息发送时间，时间戳格式',
    PRIMARY KEY (app_id, group_id, message_key)
);

CREATE TABLE im_message_body
(
    app_id       INT(10)       NOT NULL COMMENT '应用 ID',
    message_key  BIGINT(50)    NOT NULL
        PRIMARY KEY COMMENT '消息唯一标识',
    message_body VARCHAR(5000) NULL COMMENT '消息内容',
    security_key VARCHAR(100)  NULL COMMENT '消息安全密钥',
    message_time BIGINT        NULL COMMENT '消息发送时间，时间戳格式',
    create_time  BIGINT        NULL COMMENT '消息创建时间，时间戳格式',
    extra        VARCHAR(1000) NULL COMMENT '额外信息',
    del_flag     INT(10)       NULL COMMENT '删除标识：0 未删除，1 已删除'
);

CREATE TABLE im_message_history
(
    app_id         INT(20)     NOT NULL COMMENT '应用 ID',
    from_id        VARCHAR(50) NOT NULL COMMENT '发消息用户 ID',
    to_id          VARCHAR(50) NOT NULL COMMENT '接收消息用户 ID',
    owner_id       VARCHAR(50) NOT NULL COMMENT '消息所属用户 ID',
    message_key    BIGINT(50)  NOT NULL COMMENT '消息唯一标识',
    create_time    BIGINT      NULL COMMENT '消息创建时间，时间戳格式',
    sequence       BIGINT      NULL COMMENT '消息序列号，用于排序',
    message_random INT(20)     NULL COMMENT '消息随机数',
    message_time   BIGINT      NULL COMMENT '消息发送时间，时间戳格式',
    PRIMARY KEY (app_id, owner_id, message_key)
);

CREATE TABLE im_user_data
(
    user_id            VARCHAR(50)       NOT NULL COMMENT '用户唯一标识',
    app_id             INT               NOT NULL COMMENT '应用 ID',
    nick_name          VARCHAR(100)      NULL COMMENT '用户昵称',
    password           VARCHAR(255)      NULL COMMENT '用户密码',
    photo              VARCHAR(255)      NULL COMMENT '用户头像 URL',
    user_sex           INT(10)           NULL COMMENT '用户性别：1 男，2 女',
    birth_day          VARCHAR(50)       NULL COMMENT '用户生日',
    location           VARCHAR(50)       NULL COMMENT '用户地址',
    self_signature     VARCHAR(255)      NULL COMMENT '用户个性签名',
    friend_allow_type  INT(10) DEFAULT 1 NOT NULL COMMENT '加好友验证类型：1 无需验证，2 需要验证',
    forbidden_flag     INT(10) DEFAULT 0 NOT NULL COMMENT '禁用标识：0 未禁用，1 已禁用',
    disable_add_friend INT(10) DEFAULT 0 NOT NULL COMMENT '禁止添加好友：0 未禁用，1 已禁用',
    silent_flag        INT(10) DEFAULT 0 NOT NULL COMMENT '禁言标识：0 未禁言，1 已禁言',
    user_type          INT(10) DEFAULT 1 NOT NULL COMMENT '用户类型：1 普通用户，2 客服，3 机器人',
    del_flag           INT(20) DEFAULT 0 NOT NULL COMMENT '删除标识：0 未删除，1 已删除',
    extra              VARCHAR(1000)     NULL COMMENT '额外信息',
    PRIMARY KEY (app_id, user_id)
);

INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('10001', 10001, '段勇', 'consequat fugiat commodo eu ut', 'http://dummyimage.com/400x400', 1, '2004-01-28', 'nisi enim veniam minim Duis', 'velit consequat dolor Duis in', 1, 0, 0, 0, 1, 0, 'ad nostrud');
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('10002', 10001, '汤敏', 'in sed nulla occaecat', 'http://dummyimage.com/400x400', 2, '1991-07-23', 'aliqua enim in sint adipisicing', 'adipisicing', 1, 0, 0, 0, 1, 0, 'nisi ut veniam anim eiusmod');
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('10003', 10001, '白艳', 'Duis culpa ipsum deserunt', 'http://dummyimage.com/400x400', 1, '2008-11-12', 'tempor consectetur', 'consequat veniam culpa officia in', 1, 0, 0, 0, 1, 0, 'esse sit dolor anim et');
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('10004', 10001, '秦娜', 'mollit est consequat in', 'http://dummyimage.com/400x400', 1, '1988-11-27', 'nisi laborum aliquip', 'consequat', 1, 0, 0, 0, 1, 0, 'labore esse ex');
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('10005', 10001, '白芳', 'Excepteur consectetur', 'http://dummyimage.com/400x400', 1, '1994-01-08', 'culpa nostrud id reprehenderit occaecat', 'ut', 1, 0, 0, 0, 1, 0, 'ipsum nisi sint');
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('10006', 10001, '顾杰', 'est eu ipsum', 'http://dummyimage.com/400x400', 2, '1983-01-06', 'quis Duis', 'sit ex mollit', 1, 0, 0, 0, 1, 0, 'quis tempor commodo deserunt qui');
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('10007', 10001, '孔杰', 'deserunt', 'http://dummyimage.com/400x400', 1, '2009-10-28', 'ex anim fugiat ut', 'nulla laboris ea do', 1, 0, 0, 0, 1, 0, 'id ipsum veniam');
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('10008', 10001, '薛秀英', 'aliquip', 'http://dummyimage.com/400x400', 2, '2014-05-29', 'Duis tempor minim quis', 'in', 1, 0, 0, 0, 1, 0, 'ut sunt');
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('admin', 10001, 'admin', null, null, null, null, null, null, 1, 0, 0, 0, 100, 0, null);
INSERT INTO im_core.im_user_data (user_id, app_id, nick_name, password, photo, user_sex, birth_day, location, self_signature, friend_allow_type, forbidden_flag, disable_add_friend, silent_flag, user_type, del_flag, extra) VALUES ('bantanger', 10001, '半糖', null, null, null, null, null, null, 2, 0, 0, 0, 1, 0, null);
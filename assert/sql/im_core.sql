CREATE DATABASE IF NOT EXISTS im_core;
USE im_core;

CREATE TABLE app_user
(
    user_id     VARCHAR(20)  NOT NULL
        PRIMARY KEY,
    user_name   VARCHAR(255) NULL,
    password    VARCHAR(255) NULL,
    mobile      VARCHAR(255) NULL,
    create_time BIGINT       NULL,
    update_time BIGINT       NULL
);

CREATE TABLE im_conversation_set
(
    conversation_id   VARCHAR(255) NOT NULL,
    conversation_type INT(10)      NULL COMMENT '0 单聊 1群聊 2机器人 3公众号',
    from_id           VARCHAR(50)  NULL,
    to_id             VARCHAR(50)  NULL,
    is_mute           INT(10)      NULL COMMENT '是否免打扰 1免打扰',
    is_top            INT(10)      NULL COMMENT '是否置顶 1置顶',
    sequence          BIGINT       NULL COMMENT 'sequence',
    read_sequence     BIGINT       NULL,
    app_id            INT(10)      NOT NULL,
    PRIMARY KEY (app_id, conversation_id)
);

CREATE TABLE im_friendship
(
    app_id          INT(20)       NOT NULL COMMENT 'app_id',
    from_id         VARCHAR(50)   NOT NULL COMMENT 'from_id',
    to_id           VARCHAR(50)   NOT NULL COMMENT 'to_id',
    remark          VARCHAR(50)   NULL COMMENT '备注',
    status          INT(10)       NULL COMMENT '状态 1正常 2删除',
    black           INT(10)       NULL COMMENT '1正常 2拉黑',
    create_time     BIGINT        NULL,
    friend_sequence BIGINT        NULL,
    black_sequence  BIGINT        NULL,
    add_source      VARCHAR(20)   NULL COMMENT '来源',
    extra           VARCHAR(1000) NULL COMMENT '来源',
    PRIMARY KEY (app_id, from_id, to_id)
);

INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10001', '10002', 'minim tempor', 1, 1, 1680608016816, null, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10001', 'bantanger', 'minim tempor', 1, 1, 1680608016850, null, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10002', '10001', 'minim tempor', 1, 1, 1680608016850, null, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10002', 'bantanger', 'minim tempor', 1, 1, 1681024983443, 1, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, 'bantanger', '10001', 'minim tempor', 1, 1, 1680608016850, null, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, 'bantanger', '10002', 'minim tempor', 1, 1, 1681024987165, 1, null, 'cillum', 'dolore fugiat');

CREATE TABLE im_friendship_group
(
    app_id      int(20)     null comment 'app_id',
    from_id     varchar(50) null comment 'from_id',
    group_id    int(50) auto_increment
        primary key,
    group_name  varchar(50) null,
    sequence    bigint      null,
    create_time bigint      null,
    update_time bigint      null,
    del_flag    int(10)     null,
    constraint `UNIQUE`
        unique (app_id, from_id, group_name)
);

CREATE TABLE im_friendship_group_member
(
    group_id BIGINT      NOT NULL
        PRIMARY KEY,
    to_id    VARCHAR(50) NULL
);

CREATE TABLE im_friendship_request
(
    id             INT(20) AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    app_id         INT(20)     NULL COMMENT 'app_id',
    from_id        VARCHAR(50) NULL COMMENT 'from_id',
    to_id          VARCHAR(50) NULL COMMENT 'to_id',
    remark         VARCHAR(50) NULL COMMENT '备注',
    read_status    INT(10)     NULL COMMENT '是否已读 1已读',
    add_source     VARCHAR(20) NULL COMMENT '好友来源',
    add_wording    VARCHAR(50) NULL COMMENT '好友验证信息',
    approve_status INT(10)     NULL COMMENT '审批状态 1同意 2拒绝',
    create_time    BIGINT      NULL,
    update_time    BIGINT      NULL,
    sequence       BIGINT      NULL
);

INSERT INTO im_core.im_friendship_request (id, app_id, from_id, to_id, remark, read_status, add_source, add_wording, approve_status, create_time, update_time, sequence) VALUES (1, 10001, '10003', 'bantanger', 'minim tempor', 0, 'cillum', 'ipsum id', 0, 1681025483768, null, 1);

CREATE TABLE im_group
(
    app_id           INT(20)       NOT NULL COMMENT 'app_id',
    group_id         VARCHAR(50)   NOT NULL COMMENT 'group_id',
    owner_id         VARCHAR(50)   NOT NULL COMMENT '群主',
    group_type       INT(10)       NULL COMMENT '群类型 1私有群（类似微信） 2公开群(类似QQ）',
    group_name       VARCHAR(100)  NULL,
    mute             INT(10)       NULL COMMENT '是否全员禁言，0 不禁言；1 全员禁言',
    apply_join_type  INT(10)       NULL COMMENT '//    申请加群选项包括如下几种：
//    0 表示禁止任何人申请加入
//    1 表示需要群主或管理员审批
//    2 表示允许无需审批自由加入群组',
    photo            VARCHAR(300)  NULL,
    max_member_count INT(20)       NULL,
    introduction     VARCHAR(100)  NULL COMMENT '群简介',
    notification     VARCHAR(1000) NULL COMMENT '群公告',
    status           INT(5)        NULL COMMENT '群状态 0正常 1解散',
    sequence         BIGINT        NULL,
    create_time      BIGINT        NULL,
    extra            VARCHAR(1000) NULL COMMENT '来源',
    update_time      BIGINT        NULL,
    PRIMARY KEY (APP_ID, GROUP_ID)
);

INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '158e4d1df28d42ba9c23bf0d47e03be1', 'bantanger', 1, 'Java 后端必胜组', 0, 2, 'http://dummyimage.com/400x400', null, 'laborum irure minim', 'qui ipsum ut tempor', 1, 1, 1681028052204, 'ullamco consequat pariatur', null);
INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '27a35ff2f9be4cc9a8d3db1ad3322804', 'bantanger', 1, '半糖的IM小屋', 0, 2, 'http://dummyimage.com/400x400', 200, '半糖的IM聊天室小屋', '大家好，我是 BanTanger 半糖 ', 0, null, 1680055132161, 'aliquip esse eiusmod Duis ullamco', null);
INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '5ce492e515a346acbf316233d303e213', 'bantanger', 1, 'Java 后端必胜组2', 0, 2, 'http://dummyimage.com/400x400', null, 'laborum irure minim', 'qui ipsum ut tempor', 1, 2, 1681028156991, 'ullamco consequat pariatur', null);

CREATE TABLE im_group_member
(
    group_member_id BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    group_id        VARCHAR(50)   NOT NULL COMMENT 'group_id',
    app_id          INT(10)       NULL,
    member_id       VARCHAR(50)   NOT NULL COMMENT '成员ID',
    role            INT(10)       NULL COMMENT '群成员类型，0 普通成员, 1 管理员, 2 群主， 3 禁言，4 已经移除的成员',
    speak_date      BIGINT(100)   NULL,
    mute            INT(10)       NULL COMMENT '是否全员禁言，0 不禁言；1 全员禁言',
    alias           VARCHAR(100)  NULL COMMENT '群昵称',
    join_time       BIGINT        NULL COMMENT '加入时间',
    leave_time      BIGINT        NULL COMMENT '离开时间',
    join_type       VARCHAR(50)   NULL COMMENT '加入类型',
    extra           VARCHAR(1000) NULL
);

INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (1, '9122c352bbcb411f9a6cb365961dced3', 10001, '10002', 1, 20180210, null, 'consectetur quis nisi labore', 1679400643055, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (2, '9122c352bbcb411f9a6cb365961dced3', 10001, '10003', 1, 20170202, null, 'ipsum nisi magna est laboris', 1679400643080, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (3, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, '10001', 1, null, null, null, 1679400643080, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (4, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, '10002', 0, null, null, null, 1679400643080, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (5, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, 'bantanger', 2, 20200411, null, 'sit reprehenderit', 1681471124554, null, 'id', null);

CREATE TABLE im_group_message_history
(
    app_id         INT(20)     NOT NULL COMMENT 'app_id',
    from_id        VARCHAR(50) NOT NULL COMMENT 'from_id',
    group_id       VARCHAR(50) NOT NULL COMMENT 'group_id',
    message_key    BIGINT(50)  NOT NULL COMMENT 'messageBodyId',
    create_time    BIGINT      NULL,
    sequence       BIGINT      NULL,
    message_random INT(20)     NULL,
    message_time   BIGINT      NULL COMMENT '来源',
    PRIMARY KEY (app_id, group_id, message_key)
);

CREATE TABLE im_message_body
(
    app_id       INT(10)       NOT NULL,
    message_key  BIGINT(50)    NOT NULL COMMENT 'messageBodyId'
        PRIMARY KEY,
    message_body VARCHAR(5000) NULL,
    security_key VARCHAR(100)  NULL COMMENT '加密密钥',
    message_time BIGINT        NULL,
    create_time  BIGINT        NULL,
    extra        VARCHAR(1000) NULL,
    del_flag     INT(10)       NULL
);

CREATE TABLE im_message_history
(
    app_id         INT(20)     NOT NULL COMMENT 'app_id',
    from_id        VARCHAR(50) NOT NULL COMMENT 'from_id',
    to_id          VARCHAR(50) NOT NULL COMMENT 'to_id',
    owner_id       VARCHAR(50) NOT NULL COMMENT 'owner_id',
    message_key    BIGINT(50)  NOT NULL COMMENT 'messageBodyId',
    create_time    BIGINT      NULL,
    sequence       BIGINT      NULL,
    message_random INT(20)     NULL,
    message_time   BIGINT      NULL COMMENT '来源',
    PRIMARY KEY (app_id, owner_id, message_key)
);

CREATE TABLE im_user_data
(
    user_id            VARCHAR(50)       NOT NULL,
    app_id             INT               NOT NULL,
    nick_name          VARCHAR(100)      NULL COMMENT '昵称',
    password           VARCHAR(255)      NULL,
    photo              VARCHAR(255)      NULL,
    user_sex           INT(10)           NULL,
    birth_day          VARCHAR(50)       NULL COMMENT '生日',
    location           VARCHAR(50)       NULL COMMENT '地址',
    self_signature     VARCHAR(255)      NULL COMMENT '个性签名',
    friend_allow_type  INT(10) DEFAULT 1 NOT NULL COMMENT '加好友验证类型（Friend_AllowType） 1无需验证 2需要验证',
    forbidden_flag     INT(10) DEFAULT 0 NOT NULL COMMENT '禁用标识 1禁用',
    disable_add_friend INT(10) DEFAULT 0 NOT NULL COMMENT '管理员禁止用户添加加好友：0 未禁用 1 已禁用',
    silent_flag        INT(10) DEFAULT 0 NOT NULL COMMENT '禁言标识 1禁言',
    user_type          INT(10) DEFAULT 1 NOT NULL COMMENT '用户类型 1普通用户 2客服 3机器人',
    del_flag           INT(20) DEFAULT 0 NOT NULL,
    extra              VARCHAR(1000)     NULL,
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
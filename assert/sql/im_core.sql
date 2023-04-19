create table app_user
(
    user_id     varchar(20)  not null
        primary key,
    user_name   varchar(255) null,
    password    varchar(255) null,
    mobile      varchar(255) null,
    create_time bigint       null,
    update_time bigint       null
);

create table im_conversation_set
(
    conversation_id   varchar(255) not null,
    conversation_type int(10)      null comment '0 单聊 1群聊 2机器人 3公众号',
    from_id           varchar(50)  null,
    to_id             varchar(50)  null,
    is_mute           int(10)      null comment '是否免打扰 1免打扰',
    is_top            int(10)      null comment '是否置顶 1置顶',
    sequence          bigint       null comment 'sequence',
    read_sequence     bigint       null,
    app_id            int(10)      not null,
    primary key (app_id, conversation_id)
);

create table im_friendship
(
    app_id          int(20)       not null comment 'app_id',
    from_id         varchar(50)   not null comment 'from_id',
    to_id           varchar(50)   not null comment 'to_id',
    remark          varchar(50)   null comment '备注',
    status          int(10)       null comment '状态 1正常 2删除',
    black           int(10)       null comment '1正常 2拉黑',
    create_time     bigint        null,
    friend_sequence bigint        null,
    black_sequence  bigint        null,
    add_source      varchar(20)   null comment '来源',
    extra           varchar(1000) null comment '来源',
    primary key (app_id, from_id, to_id)
);

INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10001', '10002', 'minim tempor', 1, 1, 1680608016816, null, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10001', 'bantanger', 'minim tempor', 1, 1, 1680608016850, null, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10002', '10001', 'minim tempor', 1, 1, 1680608016850, null, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, '10002', 'bantanger', 'minim tempor', 1, 1, 1681024983443, 1, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, 'bantanger', '10001', 'minim tempor', 1, 1, 1680608016850, null, null, 'cillum', 'dolore fugiat');
INSERT INTO im_core.im_friendship (app_id, from_id, to_id, remark, status, black, create_time, friend_sequence, black_sequence, add_source, extra) VALUES (10001, 'bantanger', '10002', 'minim tempor', 1, 1, 1681024987165, 1, null, 'cillum', 'dolore fugiat');
create table im_friendship_group
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

create table im_friendship_group_member
(
    group_id bigint      not null
        primary key,
    to_id    varchar(50) null
);

create table im_friendship_request
(
    id             int(20) auto_increment comment 'id'
        primary key,
    app_id         int(20)     null comment 'app_id',
    from_id        varchar(50) null comment 'from_id',
    to_id          varchar(50) null comment 'to_id',
    remark         varchar(50) null comment '备注',
    read_status    int(10)     null comment '是否已读 1已读',
    add_source     varchar(20) null comment '好友来源',
    add_wording    varchar(50) null comment '好友验证信息',
    approve_status int(10)     null comment '审批状态 1同意 2拒绝',
    create_time    bigint      null,
    update_time    bigint      null,
    sequence       bigint      null
);

INSERT INTO im_core.im_friendship_request (id, app_id, from_id, to_id, remark, read_status, add_source, add_wording, approve_status, create_time, update_time, sequence) VALUES (1, 10001, '10003', 'bantanger', 'minim tempor', 0, 'cillum', 'ipsum id', 0, 1681025483768, null, 1);
create table im_group
(
    app_id           int(20)       not null comment 'app_id',
    group_id         varchar(50)   not null comment 'group_id',
    owner_id         varchar(50)   not null comment '群主
',
    group_type       int(10)       null comment '群类型 1私有群（类似微信） 2公开群(类似qq）',
    group_name       varchar(100)  null,
    mute             int(10)       null comment '是否全员禁言，0 不禁言；1 全员禁言',
    apply_join_type  int(10)       null comment '//    申请加群选项包括如下几种：
//    0 表示禁止任何人申请加入
//    1 表示需要群主或管理员审批
//    2 表示允许无需审批自由加入群组',
    photo            varchar(300)  null,
    max_member_count int(20)       null,
    introduction     varchar(100)  null comment '群简介',
    notification     varchar(1000) null comment '群公告',
    status           int(5)        null comment '群状态 0正常 1解散',
    sequence         bigint        null,
    create_time      bigint        null,
    extra            varchar(1000) null comment '来源',
    update_time      bigint        null,
    primary key (app_id, group_id)
);

INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '158e4d1df28d42ba9c23bf0d47e03be1', 'bantanger', 1, 'Java 后端必胜组', 0, 2, 'http://dummyimage.com/400x400', null, 'laborum irure minim', 'qui ipsum ut tempor', 1, 1, 1681028052204, 'ullamco consequat pariatur', null);
INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '27a35ff2f9be4cc9a8d3db1ad3322804', 'bantanger', 1, '半糖的IM小屋', 0, 2, 'http://dummyimage.com/400x400', 200, '半糖的IM聊天室小屋', '大家好，我是 BanTanger 半糖 ', 0, null, 1680055132161, 'aliquip esse eiusmod Duis ullamco', null);
INSERT INTO im_core.im_group (app_id, group_id, owner_id, group_type, group_name, mute, apply_join_type, photo, max_member_count, introduction, notification, status, sequence, create_time, extra, update_time) VALUES (10001, '5ce492e515a346acbf316233d303e213', 'bantanger', 1, 'Java 后端必胜组2', 0, 2, 'http://dummyimage.com/400x400', null, 'laborum irure minim', 'qui ipsum ut tempor', 1, 2, 1681028156991, 'ullamco consequat pariatur', null);
create table im_group_member
(
    group_member_id bigint auto_increment
        primary key,
    group_id        varchar(50)   not null comment 'group_id',
    app_id          int(10)       null,
    member_id       varchar(50)   not null comment '成员id
',
    role            int(10)       null comment '群成员类型，0 普通成员, 1 管理员, 2 群主， 3 禁言，4 已经移除的成员',
    speak_date      bigint(100)   null,
    mute            int(10)       null comment '是否全员禁言，0 不禁言；1 全员禁言',
    alias           varchar(100)  null comment '群昵称',
    join_time       bigint        null comment '加入时间',
    leave_time      bigint        null comment '离开时间',
    join_type       varchar(50)   null comment '加入类型',
    extra           varchar(1000) null
);

INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (1, '9122c352bbcb411f9a6cb365961dced3', 10001, '10002', 1, 20180210, null, 'consectetur quis nisi labore', 1679400643055, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (2, '9122c352bbcb411f9a6cb365961dced3', 10001, '10003', 1, 20170202, null, 'ipsum nisi magna est laboris', 1679400643080, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (3, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, '10001', 1, null, null, null, 1679400643080, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (4, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, '10002', 0, null, null, null, 1679400643080, null, null, null);
INSERT INTO im_core.im_group_member (group_member_id, group_id, app_id, member_id, role, speak_date, mute, alias, join_time, leave_time, join_type, extra) VALUES (5, '27a35ff2f9be4cc9a8d3db1ad3322804', 10001, 'bantanger', 2, 20200411, null, 'sit reprehenderit', 1681471124554, null, 'id', null);
create table im_group_message_history
(
    app_id         int(20)     not null comment 'app_id',
    from_id        varchar(50) not null comment 'from_id',
    group_id       varchar(50) not null comment 'group_id',
    message_key    bigint(50)  not null comment 'messageBodyId',
    create_time    bigint      null,
    sequence       bigint      null,
    message_random int(20)     null,
    message_time   bigint      null comment '来源',
    primary key (app_id, group_id, message_key)
);

create table im_message_body
(
    app_id       int(10)       not null,
    message_key  bigint(50)    not null
        primary key,
    message_body varchar(5000) null,
    security_key varchar(100)  null,
    message_time bigint        null,
    create_time  bigint        null,
    extra        varchar(1000) null,
    del_flag     int(10)       null
);

create table im_message_history
(
    app_id         int(20)     not null comment 'app_id',
    from_id        varchar(50) not null comment 'from_id',
    to_id          varchar(50) not null comment 'to_id
',
    owner_id       varchar(50) not null comment 'owner_id
',
    message_key    bigint(50)  not null comment 'messageBodyId',
    create_time    bigint      null,
    sequence       bigint      null,
    message_random int(20)     null,
    message_time   bigint      null comment '来源',
    primary key (app_id, owner_id, message_key)
);

create table im_user_data
(
    user_id            varchar(50)       not null,
    app_id             int               not null,
    nick_name          varchar(100)      null comment '昵称',
    password           varchar(255)      null,
    photo              varchar(255)      null,
    user_sex           int(10)           null,
    birth_day          varchar(50)       null comment '生日',
    location           varchar(50)       null comment '地址',
    self_signature     varchar(255)      null comment '个性签名',
    friend_allow_type  int(10) default 1 not null comment '加好友验证类型（Friend_AllowType） 1无需验证 2需要验证',
    forbidden_flag     int(10) default 0 not null comment '禁用标识 1禁用',
    disable_add_friend int(10) default 0 not null comment '管理员禁止用户添加加好友：0 未禁用 1 已禁用',
    silent_flag        int(10) default 0 not null comment '禁言标识 1禁言',
    user_type          int(10) default 1 not null comment '用户类型 1普通用户 2客服 3机器人',
    del_flag           int(20) default 0 not null,
    extra              varchar(1000)     null,
    primary key (app_id, user_id)
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
package com.bantanger.im.domain.user;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QImUserData is a Querydsl query type for ImUserData
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QImUserData extends EntityPathBase<ImUserData> {

    private static final long serialVersionUID = -66816925L;

    public static final QImUserData imUserData = new QImUserData("imUserData");

    public final com.bantanger.jpa.support.QBaseJpaAggregate _super = new com.bantanger.jpa.support.QBaseJpaAggregate(this);

    public final NumberPath<Integer> appId = createNumber("appId", Integer.class);

    public final StringPath birthDay = createString("birthDay");

    //inherited
    public final DateTimePath<java.time.Instant> createdAt = _super.createdAt;

    public final EnumPath<com.bantanger.common.enums.ValidStatus> disableAddFriend = createEnum("disableAddFriend", com.bantanger.common.enums.ValidStatus.class);

    public final ListPath<com.bantanger.common.model.CodeValue, SimplePath<com.bantanger.common.model.CodeValue>> extra = this.<com.bantanger.common.model.CodeValue, SimplePath<com.bantanger.common.model.CodeValue>>createList("extra", com.bantanger.common.model.CodeValue.class, SimplePath.class, PathInits.DIRECT2);

    public final EnumPath<com.bantanger.common.enums.ValidStatus> forbiddenFlag = createEnum("forbiddenFlag", com.bantanger.common.enums.ValidStatus.class);

    public final EnumPath<com.bantanger.im.domain.user.enums.FriendAllowType> friendAllowType = createEnum("friendAllowType", com.bantanger.im.domain.user.enums.FriendAllowType.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath location = createString("location");

    public final StringPath nickName = createString("nickName");

    public final StringPath password = createString("password");

    public final StringPath photo = createString("photo");

    public final StringPath selfSignature = createString("selfSignature");

    public final EnumPath<com.bantanger.common.enums.ValidStatus> silentFlag = createEnum("silentFlag", com.bantanger.common.enums.ValidStatus.class);

    //inherited
    public final DateTimePath<java.time.Instant> updatedAt = _super.updatedAt;

    public final StringPath userSex = createString("userSex");

    public final EnumPath<com.bantanger.im.domain.user.enums.UserType> userType = createEnum("userType", com.bantanger.im.domain.user.enums.UserType.class);

    public final EnumPath<com.bantanger.common.enums.ValidStatus> validStatus = createEnum("validStatus", com.bantanger.common.enums.ValidStatus.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QImUserData(String variable) {
        super(ImUserData.class, forVariable(variable));
    }

    public QImUserData(Path<? extends ImUserData> path) {
        super(path.getType(), path.getMetadata());
    }

    public QImUserData(PathMetadata metadata) {
        super(ImUserData.class, metadata);
    }

}


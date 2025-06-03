package com.bantanger.im.domain.groupddd;

import com.bantanger.codegen.processor.api.GenCreateRequest;
import com.bantanger.codegen.processor.api.GenQueryRequest;
import com.bantanger.codegen.processor.api.GenResponse;
import com.bantanger.codegen.processor.api.GenUpdateRequest;
import com.bantanger.codegen.processor.controller.GenController;
import com.bantanger.codegen.processor.creator.GenCreator;
import com.bantanger.codegen.processor.mapper.GenMapper;
import com.bantanger.codegen.processor.query.GenQuery;
import com.bantanger.codegen.processor.repository.GenRepository;
import com.bantanger.codegen.processor.service.GenService;
import com.bantanger.codegen.processor.creator.IgnoreCreator;
import com.bantanger.codegen.processor.updater.IgnoreUpdater;
import com.bantanger.codegen.processor.service.GenServiceImpl;
import com.bantanger.codegen.processor.updater.GenUpdater;
import com.bantanger.codegen.processor.vo.GenVo;
import com.bantanger.common.annotation.FieldDesc;
import com.bantanger.common.annotation.TypeConverter;
import com.bantanger.common.converter.CodeValueListConverter;
import com.bantanger.common.enums.ValidStatus;
import com.bantanger.common.model.CodeValue;
import com.bantanger.im.common.converter.GroupMemberRoleTypeConverter;
import com.bantanger.im.common.enums.group.GroupMemberRoleType;
import com.bantanger.jpa.converter.ValidStatusConverter;
import com.bantanger.jpa.support.BaseJpaAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;

@GenVo(pkgName = "com.bantanger.im.domain.groupddd.vo")
@GenCreator(pkgName = "com.bantanger.im.domain.groupddd.creator")
@GenUpdater(pkgName = "com.bantanger.im.domain.groupddd.updater")
@GenRepository(pkgName = "com.bantanger.im.domain.groupddd.repository")
@GenService(pkgName = "com.bantanger.im.domain.groupddd.service")
@GenServiceImpl(pkgName = "com.bantanger.im.domain.groupddd.service")
@GenQuery(pkgName = "com.bantanger.im.domain.groupddd.query")
@GenMapper(pkgName = "com.bantanger.im.domain.groupddd.mapper")
@GenController(pkgName = "com.bantanger.im.domain.groupddd.controller")
@GenCreateRequest(pkgName = "com.bantanger.im.domain.groupddd.request")
@GenUpdateRequest(pkgName = "com.bantanger.im.domain.groupddd.request")
@GenQueryRequest(pkgName = "com.bantanger.im.domain.groupddd.request")
@GenResponse(pkgName = "com.bantanger.im.domain.groupddd.response")
@Entity
@Table(name = "im_group_member")
@Data
public class ImGroupMember extends BaseJpaAggregate {

    @FieldDesc(name = "应用ID")
    @IgnoreUpdater
    private Integer appId;

    @FieldDesc(name = "群组ID")
    @IgnoreUpdater
    private String groupId;

    @FieldDesc(name = "成员ID")
    @IgnoreUpdater
    private String memberId;

    @FieldDesc(name = "成员角色")
    @Convert(converter = GroupMemberRoleTypeConverter.class)
    @TypeConverter
    private GroupMemberRoleType role;

    @FieldDesc(name = "禁言时间")
    private Long silenceTime;

    @FieldDesc(name = "群昵称")
    private String alias;

    @FieldDesc(name = "加入时间")
    @IgnoreUpdater
    @IgnoreCreator
    private Long joinTime;

    @FieldDesc(name = "离开时间")
    private Long leaveTime;

    @FieldDesc(name = "加入类型")
    private String joinType;

    @FieldDesc(name = "扩展信息")
    @Convert(converter = CodeValueListConverter.class)
    @Column(columnDefinition = "text")
    private List<CodeValue> extra;

    @Convert(converter = ValidStatusConverter.class)
    @IgnoreUpdater
    @IgnoreCreator
    private ValidStatus validStatus;

    public void init() {
        setValidStatus(ValidStatus.VALID);
        setJoinTime(System.currentTimeMillis());
    }

    public void valid() {
        setValidStatus(ValidStatus.VALID);
    }

    public void invalid() {
        setValidStatus(ValidStatus.INVALID);
    }

    public void leave() {
        setLeaveTime(System.currentTimeMillis());
    }

    public void rejoin() {
        setLeaveTime(null);
        setJoinTime(System.currentTimeMillis());
    }
} 
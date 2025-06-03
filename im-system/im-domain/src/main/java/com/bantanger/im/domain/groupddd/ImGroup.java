package com.bantanger.im.domain.groupddd;

import com.bantanger.codegen.processor.api.GenCreateRequest;
import com.bantanger.codegen.processor.api.GenQueryRequest;
import com.bantanger.codegen.processor.api.GenResponse;
import com.bantanger.codegen.processor.api.GenUpdateRequest;
import com.bantanger.codegen.processor.controller.GenController;
import com.bantanger.codegen.processor.creator.GenCreator;
import com.bantanger.codegen.processor.creator.IgnoreCreator;
import com.bantanger.codegen.processor.mapper.GenMapper;
import com.bantanger.codegen.processor.query.GenQuery;
import com.bantanger.codegen.processor.repository.GenRepository;
import com.bantanger.codegen.processor.service.GenService;
import com.bantanger.codegen.processor.service.GenServiceImpl;
import com.bantanger.codegen.processor.updater.GenUpdater;
import com.bantanger.codegen.processor.updater.IgnoreUpdater;
import com.bantanger.codegen.processor.vo.GenVo;
import com.bantanger.common.annotation.FieldDesc;
import com.bantanger.common.annotation.TypeConverter;
import com.bantanger.common.converter.CodeValueListConverter;
import com.bantanger.common.enums.ValidStatus;
import com.bantanger.common.model.CodeValue;
import com.bantanger.im.common.converter.GroupMuteTypeConverter;
import com.bantanger.im.common.converter.GroupTypeConverter;
import com.bantanger.im.common.enums.group.GroupMuteType;
import com.bantanger.im.common.enums.group.GroupType;
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
@Table(name = "im_group")
@Data
public class ImGroup extends BaseJpaAggregate {

    @FieldDesc(name = "群组ID")
    @IgnoreUpdater
    private String groupId;

    @FieldDesc(name = "应用ID")
    @IgnoreUpdater
    private Integer appId;

    @FieldDesc(name = "群主ID")
    @IgnoreUpdater
    private String ownerId;

    @FieldDesc(name = "群类型")
    @Convert(converter = GroupTypeConverter.class)
    @TypeConverter
    private GroupType groupType;

    @FieldDesc(name = "群名称")
    private String groupName;

    @FieldDesc(name = "是否全员禁言")
    @Convert(converter = GroupMuteTypeConverter.class)
    @TypeConverter
    private GroupMuteType mute;

    @FieldDesc(name = "申请加群类型")
    private Integer applyJoinType;

    @FieldDesc(name = "群简介")
    private String introduction;

    @FieldDesc(name = "群公告")
    private String notification;

    @FieldDesc(name = "群头像")
    // TODO url 合法性校验，大小检测
    private String photo;

    @FieldDesc(name = "群成员上限")
    private Integer maxMemberCount;

    @FieldDesc(name = "群状态")
    @Convert(converter = ValidStatusConverter.class)
    private ValidStatus status;

    @FieldDesc(name = "序列号")
    @IgnoreUpdater
    @IgnoreCreator
    private Long sequence;

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
        setStatus(ValidStatus.VALID);
    }

    public void valid() {
        setValidStatus(ValidStatus.VALID);
    }

    public void invalid() {
        setValidStatus(ValidStatus.INVALID);
    }

    public void destroy() {
        setStatus(ValidStatus.INVALID);
    }

    public void restore() {
        setStatus(ValidStatus.VALID);
    }
} 
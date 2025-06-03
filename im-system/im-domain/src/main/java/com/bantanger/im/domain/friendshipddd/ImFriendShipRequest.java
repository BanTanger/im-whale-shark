package com.bantanger.im.domain.friendshipddd;

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
import com.bantanger.common.converter.CodeValueListConverter;
import com.bantanger.common.enums.ValidStatus;
import com.bantanger.common.model.CodeValue;
import com.bantanger.jpa.converter.ValidStatusConverter;
import com.bantanger.jpa.support.BaseJpaAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;

@GenVo(pkgName = "com.bantanger.im.domain.friendshipddd.vo")
@GenCreator(pkgName = "com.bantanger.im.domain.friendshipddd.creator")
@GenUpdater(pkgName = "com.bantanger.im.domain.friendshipddd.updater")
@GenRepository(pkgName = "com.bantanger.im.domain.friendshipddd.repository")
@GenService(pkgName = "com.bantanger.im.domain.friendshipddd.service")
@GenServiceImpl(pkgName = "com.bantanger.im.domain.friendshipddd.service")
@GenQuery(pkgName = "com.bantanger.im.domain.friendshipddd.query")
@GenMapper(pkgName = "com.bantanger.im.domain.friendshipddd.mapper")
@GenController(pkgName = "com.bantanger.im.domain.friendshipddd.controller")
@GenCreateRequest(pkgName = "com.bantanger.im.domain.friendshipddd.request")
@GenUpdateRequest(pkgName = "com.bantanger.im.domain.friendshipddd.request")
@GenQueryRequest(pkgName = "com.bantanger.im.domain.friendshipddd.request")
@GenResponse(pkgName = "com.bantanger.im.domain.friendshipddd.response")
@Entity
@Table(name = "im_friendship_request")
@Data
public class ImFriendShipRequest extends BaseJpaAggregate {

    @FieldDesc(name = "发送方ID")
    @IgnoreUpdater
    private String fromId;

    @FieldDesc(name = "接收方ID")
    @IgnoreUpdater
    private String toId;

    @FieldDesc(name = "好友来源")
    private Integer addSource;

    @FieldDesc(name = "好友来源描述")
    private String addWording;

    @FieldDesc(name = "好友来源描述")
    private String remark;

    @FieldDesc(name = "扩展信息")
    @Convert(converter = CodeValueListConverter.class)
    @Column(columnDefinition = "text")
    private List<CodeValue> extra;

    @FieldDesc(name = "好友状态")
    private Integer status;

    @Convert(converter = ValidStatusConverter.class)
    @IgnoreUpdater
    @IgnoreCreator
    private ValidStatus validStatus;

    public void init() {
        setValidStatus(ValidStatus.VALID);
    }

    public void valid() {
        setValidStatus(ValidStatus.VALID);
    }

    public void invalid() {
        setValidStatus(ValidStatus.INVALID);
    }
} 
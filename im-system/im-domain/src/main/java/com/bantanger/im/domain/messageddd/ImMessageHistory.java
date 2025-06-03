package com.bantanger.im.domain.messageddd;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */
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
import com.bantanger.common.enums.ValidStatus;
import com.bantanger.im.common.converter.MsgTypeConverter;
import com.bantanger.im.common.enums.message.MsgType;
import com.bantanger.jpa.converter.ValidStatusConverter;
import com.bantanger.jpa.support.BaseJpaAggregate;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@GenVo(pkgName = "com.bantanger.im.domain.messageddd.vo")
@GenCreator(pkgName = "com.bantanger.im.domain.messageddd.creator")
@GenUpdater(pkgName = "com.bantanger.im.domain.messageddd.updater")
@GenRepository(pkgName = "com.bantanger.im.domain.messageddd.repository")
@GenService(pkgName = "com.bantanger.im.domain.messageddd.service")
@GenServiceImpl(pkgName = "com.bantanger.im.domain.messageddd.service")
@GenQuery(pkgName = "com.bantanger.im.domain.messageddd.query")
@GenMapper(pkgName = "com.bantanger.im.domain.messageddd.mapper")
@GenController(pkgName = "com.bantanger.im.domain.messageddd.controller")
@GenCreateRequest(pkgName = "com.bantanger.im.domain.messageddd.request")
@GenUpdateRequest(pkgName = "com.bantanger.im.domain.messageddd.request")
@GenQueryRequest(pkgName = "com.bantanger.im.domain.messageddd.request")
@GenResponse(pkgName = "com.bantanger.im.domain.messageddd.response")
@Entity
@Table(name = "im_message_history")
@Data
public class ImMessageHistory extends BaseJpaAggregate {

    @FieldDesc(name = "消息类型")
    @Convert(converter = MsgTypeConverter.class)
    @TypeConverter
    private MsgType msgType;

    @FieldDesc(name = "消息来源方ID")
    private String fromId;

    /**
     * 目标对象 Id 或者群组 Id
     */
    @FieldDesc(name = "消息接收方ID")
    private String toId;

    /**
     * 客户端消息ID
     * <pre>客户端透传过来的字段，
     * 对服务端无用，对客户端而言，是ACK等待的标识，
     * 服务端处理完消息后需要回返该字段以便客户端消除对应ACK等待时钟</pre>
     */
    @FieldDesc(name = "客户端消息ID")
    @IgnoreUpdater
    private Long clientMsgId;

    /**
     * 服务端消息ID
     * <pre>关联表 im_message_body 的 message_key 字段
     * {@link ImMessageBody#getMessageKey()}</pre>
     */
    @FieldDesc(name = "服务端消息ID")
    @IgnoreUpdater
    private Long serverMsgId;

    @FieldDesc(name = "消息序列号（相对序列）")
    private Long sequence;

    @FieldDesc(name = "消息加密密钥")
    private String messageSecurityKey;

    @FieldDesc(name = "消息拥有者，写扩散标识写入哪个消息队列")
    private String ownerId;

    private Long messageTime;

    @FieldDesc(name = "用户所在APP")
    @IgnoreUpdater
    private Integer appId;

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

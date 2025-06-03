package com.bantanger.im.domain.conversationddd;

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
import com.bantanger.im.common.converter.ConversationAppearsTypeConverter;
import com.bantanger.im.common.converter.ConversationNoticeTypeConverter;
import com.bantanger.im.common.converter.ConversationTypeConverter;
import com.bantanger.im.common.enums.conversation.ConversationAppearsType;
import com.bantanger.im.common.enums.conversation.ConversationType;
import com.bantanger.im.common.enums.conversation.ConversationNoticeType;
import com.bantanger.jpa.converter.ValidStatusConverter;
import com.bantanger.jpa.support.BaseJpaAggregate;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@GenVo(pkgName = "com.bantanger.im.domain.conversationddd.vo")
@GenCreator(pkgName = "com.bantanger.im.domain.conversationddd.creator")
@GenUpdater(pkgName = "com.bantanger.im.domain.conversationddd.updater")
@GenRepository(pkgName = "com.bantanger.im.domain.conversationddd.repository")
@GenService(pkgName = "com.bantanger.im.domain.conversationddd.service")
@GenServiceImpl(pkgName = "com.bantanger.im.domain.conversationddd.service")
@GenQuery(pkgName = "com.bantanger.im.domain.conversationddd.query")
@GenMapper(pkgName = "com.bantanger.im.domain.conversationddd.mapper")
@GenController(pkgName = "com.bantanger.im.domain.conversationddd.controller")
@GenCreateRequest(pkgName = "com.bantanger.im.domain.conversationddd.request")
@GenUpdateRequest(pkgName = "com.bantanger.im.domain.conversationddd.request")
@GenQueryRequest(pkgName = "com.bantanger.im.domain.conversationddd.request")
@GenResponse(pkgName = "com.bantanger.im.domain.conversationddd.response")
@Entity
@Table(name = "im_conversation_set")
@Data
public class ImConversationSet extends BaseJpaAggregate {

    /** 如 0_fromId_toId */
    @FieldDesc(name = "会话ID")
    private String conversationId;

    @FieldDesc(name = "会话类型")
    @Convert(converter = ConversationTypeConverter.class)
    @TypeConverter
    private ConversationType conversationType;

    @FieldDesc(name = "消息来源方ID")
    private String fromId;

    /** 目标对象 Id 或者群组 Id */
    @FieldDesc(name = "消息接收方ID")
    private String toId;

    @FieldDesc(name = "会话通知方式")
    @Convert(converter = ConversationNoticeTypeConverter.class)
    @TypeConverter
    private ConversationNoticeType conversationNoticeType;

    @FieldDesc(name = "会话显示位置")
    @Convert(converter = ConversationAppearsTypeConverter.class)
    @TypeConverter
    private ConversationAppearsType conversationAppearsType;

    @FieldDesc(name = "会话最新消息序列号")
    private Long sequence;

    /**
     * 消息已读偏序
     * <p>持久化客户端上报的本地端最大的已读消息序列号，这个消息序列号只会增长，
     * 不会回退，哪怕当前客户端上报的已读消息序列号小于服务端已读消息序列号。
     * 事实上，这种情况我们认为是消息同步出现了问题：多端处于不一致。此时会重新同步消息。
     * </p>
     */
    @FieldDesc(name = "消息已读偏序")
    private Long readSequence;

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

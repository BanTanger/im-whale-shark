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
@Table(name = "im_message_body")
@Data
public class ImMessageBody extends BaseJpaAggregate {

    /**
     * 消息唯一ID标识
     * <pre>关联  {@link ImMessageHistory#getServerMsgId()}
     * ID 生成策略依据 {@link com.bantanger.im.service.support.ids.SnowflakeIdWorker}
     * </pre>
     */
    @FieldDesc(name = "唯一ID标识")
    private Long messageKey;

    private String messageBody;

    private Long messageTime;

    @FieldDesc(name = "用户所在APP")
    @IgnoreUpdater
    private Integer appId;

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
    }

    public void valid() {
        setValidStatus(ValidStatus.VALID);
    }

    public void invalid() {
        setValidStatus(ValidStatus.INVALID);
    }
}

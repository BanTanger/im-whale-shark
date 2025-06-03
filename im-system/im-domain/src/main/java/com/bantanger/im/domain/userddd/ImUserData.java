package com.bantanger.im.domain.userddd;

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
import com.bantanger.codegen.processor.vo.IgnoreVo;
import com.bantanger.common.annotation.FieldDesc;
import com.bantanger.common.annotation.TypeConverter;
import com.bantanger.common.converter.CodeValueListConverter;
import com.bantanger.common.enums.ValidStatus;
import com.bantanger.common.model.CodeValue;
import com.bantanger.im.common.converter.FriendAllowTypeConverter;
import com.bantanger.im.common.converter.UserTypeConverter;
import com.bantanger.im.common.enums.user.FriendAllowType;
import com.bantanger.im.common.enums.user.UserType;
import com.bantanger.jpa.converter.ValidStatusConverter;
import com.bantanger.jpa.support.BaseJpaAggregate;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;

@GenVo(pkgName = "com.bantanger.im.domain.userddd.vo")
@GenCreator(pkgName = "com.bantanger.im.domain.userddd.creator")
@GenUpdater(pkgName = "com.bantanger.im.domain.userddd.updater")
@GenRepository(pkgName = "com.bantanger.im.domain.userddd.repository")
@GenService(pkgName = "com.bantanger.im.domain.userddd.service")
@GenServiceImpl(pkgName = "com.bantanger.im.domain.userddd.service")
@GenQuery(pkgName = "com.bantanger.im.domain.userddd.query")
@GenMapper(pkgName = "com.bantanger.im.domain.userddd.mapper")
@GenController(pkgName = "com.bantanger.im.domain.userddd.controller")
@GenCreateRequest(pkgName = "com.bantanger.im.domain.userddd.request")
@GenUpdateRequest(pkgName = "com.bantanger.im.domain.userddd.request")
@GenQueryRequest(pkgName = "com.bantanger.im.domain.userddd.request")
@GenResponse(pkgName = "com.bantanger.im.domain.userddd.response")
@Entity
@Table(name = "im_user_data")
@Data
public class ImUserData extends BaseJpaAggregate {

    @FieldDesc(name = "用户名称")
    private String nickName;

    @FieldDesc(name = "位置")
    private String location;

    @FieldDesc(name = "生日")
    private String birthDay;

    @FieldDesc(name = "密码")
    @IgnoreVo
    private String password;

    @FieldDesc(name = "头像")
    private String photo;

    @FieldDesc(name = "性别")
    private String userSex;

    @FieldDesc(name = "个性签名")
    private String selfSignature;

    @FieldDesc(name = "加好友验证类型")
    @Convert(converter = FriendAllowTypeConverter.class)
    @TypeConverter
    private FriendAllowType friendAllowType;

    @FieldDesc(name = "管理员禁止用户添加加好友")
    @Convert(converter = ValidStatusConverter.class)
    private ValidStatus disableAddFriend;

    @FieldDesc(name = "禁用标识")
    @Convert(converter = ValidStatusConverter.class)
    private ValidStatus forbiddenFlag;

    @FieldDesc(name = "禁言标识")
    @Convert(converter = ValidStatusConverter.class)
    private ValidStatus silentFlag;

    @FieldDesc(name = "用户类型")
    @Convert(converter = UserTypeConverter.class)
    @TypeConverter
    private UserType userType;

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

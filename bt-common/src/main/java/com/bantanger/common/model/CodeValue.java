package com.bantanger.common.model;

import com.bantanger.common.annotation.FieldDesc;
import lombok.Data;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@Data
public class CodeValue {

    @FieldDesc(name = "key-键")
    private String k;
    @FieldDesc(name = "value-值")
    private String v;
    @FieldDesc(name = "level-标签")
    private String l;

}

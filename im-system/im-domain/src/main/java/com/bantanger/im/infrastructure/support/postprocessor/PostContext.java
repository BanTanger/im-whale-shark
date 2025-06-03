package com.bantanger.im.infrastructure.support.postprocessor;

import lombok.Getter;
import lombok.Setter;

/**
 * 主流程与分支流程数据流传的上下文定义
 * @author BanTanger 半糖
 * @Date 2023/12/16 23:02
 */
@Getter
@Setter
public class PostContext<T> {

    /**
     * 数据流转主体对象
     */
    private T bizData;

    /**
     * 主流程的运行结果
     * 是否会对后继节点产生影响, 如果会则有值，否则有没有值都无所谓
     */
    private Object mainProcessRes;

    /**
     * 其余的扩展信息
     */
    private Object extra;

}

package com.bantanger.im.codec.proto;

import com.bantanger.im.common.enums.message.ImSystemMessageType;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 18:58
 */
@Data
public class MessageHeader {

    /**
     * 消息操作指令(4字节) 十六进制 一个消息的开始通常以0x开头
     */
    private Integer command;
    /**
     * 4字节 版本号
     */
    private Integer version;
    /**
     * 4字节 端类型
     */
    private Integer clientType;
    /**
     * 应用ID(4字节)
     */
    private Integer appId;
    /**
     * 数据解析类型(4字节) 和具体业务无关
     * 后续根据解析类型解析data数据 0x0:Json,0x1:ProtoBuf,0x2:Xml,默认:0x0
     */
    private Integer messageType = ImSystemMessageType.DATA_TYPE_JSON.getCode();

    /**
     * 4字节 imel长度
     */
    private Integer imeiLength;

    /**
     * 4字节 包体长度
     */
    private int length;

    /**
     * imei号
     */
    private String imei;
}
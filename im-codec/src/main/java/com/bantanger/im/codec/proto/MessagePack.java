package com.bantanger.im.codec.proto;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息服务发送给 tcp 的包体信息，tcp 再更具改包体协议解析成 Message 发送给客户端
 * @author BanTanger 半糖
 * @Date 2023/3/27 13:21
 */
@Data
public class MessagePack<T> implements Serializable {

    private String userId;

    private Integer appId;

    /**
     * 接收方
     */
    private String toId;

    /**
     * 客户端标识
     */
    private int clientType;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 客户端设备唯一标识
     */
    private String imei;

    private Integer command;

    /**
     * 业务数据对象，如果是聊天消息则不需要解析直接透传
     */
    private T data;

//    /** 用户签名*/
//    private String UserSign;

}

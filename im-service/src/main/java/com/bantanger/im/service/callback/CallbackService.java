package com.bantanger.im.service.callback;

import com.bantanger.im.common.ResponseVO;

/**
 * 回调机制接口定义
 * @author BanTanger 半糖
 * @Date 2023/3/30 19:01
 */
public interface CallbackService {

    /**
     * 在事件执行之前的回调
     * 干预事件的后续流程处理，以及对用户行为埋点，记录日志
     * 需要返回值(用户有感，异步)
     * @param appId
     * @param callbackCommand
     * @param jsonBody
     * @return
     */
    ResponseVO beforeCallback(Integer appId, String callbackCommand, String jsonBody);

    /**
     * 在事件执行之后的回调
     * 进行数据同步
     * 不需要返回值(用户无感)
     * @param appId
     * @param callbackCommand
     * @param jsonBody
     */
    void afterCallback(Integer appId, String callbackCommand, String jsonBody);

}

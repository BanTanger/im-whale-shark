package com.bantanger.im.service.sendmsg;

import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.common.model.UserSession;
import com.bantanger.im.service.session.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 消息发送目标/数量选择 实体类
 * @author BanTanger 半糖
 * @Date 2023/3/31 23:20
 */
@Slf4j
@Component
public class MessageProducer extends AbstractMessageSend {

    @Resource
    UserSessionService userSessionService;

    /**
     * 消息发送【兼容管理员和普通用户】
     * @param toId
     * @param command
     * @param data
     * @param appId
     * @param clientType
     * @param imei
     */
    public void sendMsgToUser(String toId, Command command, Object data, Integer appId, Integer clientType, String imei) {
        if (clientType != null && StringUtils.isNotBlank(imei)) {
            // (app 调用)普通用户发起的消息，发送给出本端以外的所有端
            ClientInfo clientInfo = new ClientInfo(appId, clientType, imei);
            sendToUserExceptClient(toId, command, data, clientInfo);
        } else {
            // (后台调用)管理员发起的消息(管理员没有 imei 号)，发送给所有端
            sendToUserAllClient(toId, command, data, appId);
        }
    }

    @Override
    public List<ClientInfo> sendToUserAllClient(String toId, Command command, Object data, Integer appId) {
        // 获取当前用户所有在线端的 Session
        List<UserSession> userSessions = userSessionService.getUserSession(appId, toId);
        return userSessions.stream()
                // 筛出非空对象
                .filter(Objects::nonNull)
                // 消息发送
                .filter(session -> sendMessage(toId, command, data, session))
                .map(session -> new ClientInfo(session.getAppId(),
                        session.getClientType(), session.getImei()))
                .collect(Collectors.toList());
    }


    @Override
    public void sendToUserOneClient(String toId, Command command, Object data, ClientInfo clientInfo) {
        UserSession userSession = userSessionService.getUserSession(
                clientInfo.getAppId(), toId, clientInfo.getClientType(), clientInfo.getImei());
        sendMessage(toId, command, data, userSession);
    }

    @Override
    public void sendToUserExceptClient(String toId, Command command, Object data, ClientInfo clientInfo) {
        List<UserSession> userSession = userSessionService.getUserSession(clientInfo.getAppId(), toId);
        userSession.stream()
                .filter(session -> !isMatch(session, clientInfo))
                .forEach(session -> sendMessage(toId, command, data, session));
    }

    private boolean isMatch(UserSession session, ClientInfo clientInfo) {
        return Objects.equals(session.getAppId(), clientInfo.getAppId())
                && Objects.equals(session.getClientType(), clientInfo.getClientType())
                && Objects.equals(session.getImei(), clientInfo.getImei());
    }

}

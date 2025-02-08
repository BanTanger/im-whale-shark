package com.bantanger.im.service.strategy.login.impl;

import com.bantanger.im.common.enums.device.ClientType;
import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.service.strategy.login.LoginStatus;
import com.bantanger.im.service.utils.UserChannelRepository;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/27 15:19
 */
@Slf4j
public class TwoClientLoginStatus extends LoginStatus {
    @Override
    public void switchStatus(LoginStatus status) {
        context.setStatus(status);
    }

    @Override
    public void handleUserLogin(UserClientDto dto) {
        List<Channel> userChannels = UserChannelRepository.getUserChannels(dto.getAppId(), dto.getUserId());
        for (Channel userChannel : userChannels) {
            UserClientDto userInfo = UserChannelRepository.getUserInfo(userChannel);
            Integer channelClientType = userInfo.getClientType();
            String channelImei = userInfo.getImei();

            // 允许 Web 多端登录
            if (ClientType.WEB.getCode().equals(dto.getClientType()) || ClientType.WEB.getCode().equals(channelClientType)) {
                continue;
            }

            sendMutualLoginMsg(userChannel, channelClientType, channelImei, dto);
        }
    }
}

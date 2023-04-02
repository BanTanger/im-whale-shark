package com.bantanger.im.service.rabbitmq.process;

import com.bantanger.im.codec.proto.MessagePack;
import com.bantanger.im.service.utils.UserChannelRepository;
import io.netty.channel.Channel;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/2 10:42
 */
public abstract class BaseProcess {

    public void process(MessagePack messagePack) {
        processBefore();

        Channel userChannel = UserChannelRepository.getUserChannel(messagePack.getAppId(),
                messagePack.getToId(), messagePack.getClientType(), messagePack.getImei());
        if (userChannel != null) {
            // 数据通道写入消息内容
            userChannel.writeAndFlush(messagePack);
        }

        processAfter();
    }

    /**
     * 流程执行前的定制化处理
     */
    public abstract void processBefore();

    /**
     * 流程执行后的定制化处理
     */
    public abstract void processAfter();

}
